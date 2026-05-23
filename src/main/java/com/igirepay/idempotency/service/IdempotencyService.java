package com.igirepay.idempotency.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igirepay.idempotency.dto.PaymentRequest;
import com.igirepay.idempotency.dto.PaymentResponse;
import com.igirepay.idempotency.dto.PaymentResult;
import com.igirepay.idempotency.exception.IdempotencyConflictException;
import com.igirepay.idempotency.model.IdempotencyRecord;
import com.igirepay.idempotency.model.RequestState;
import com.igirepay.idempotency.repository.IdempotencyRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyRecordRepository repository;
    private final PaymentProcessor paymentProcessor;
    private final ObjectMapper objectMapper;

    // Per-key futures coordinate in-flight requests without holding DB connections open
    private final ConcurrentHashMap<String, CompletableFuture<PaymentResponse>> inFlight =
            new ConcurrentHashMap<>();

    public IdempotencyService(IdempotencyRecordRepository repository,
                               PaymentProcessor paymentProcessor,
                               ObjectMapper objectMapper) {
        this.repository       = repository;
        this.paymentProcessor = paymentProcessor;
        this.objectMapper     = objectMapper;
    }

    public PaymentResult processPayment(String idempotencyKey, PaymentRequest request) {
        String requestHash = computeHash(request);
        log.info("Received payment request key={} hash={}", idempotencyKey, requestHash);

        Optional<IdempotencyRecord> existing = repository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();

            if (!record.getRequestHash().equals(requestHash)) {
                log.info("Key conflict key={} storedHash={} incomingHash={}",
                        idempotencyKey, record.getRequestHash(), requestHash);
                throw new IdempotencyConflictException(
                        "Idempotency key already used for a different request body.");
            }

            if (record.getState() == RequestState.COMPLETED) {
                log.info("Cache hit key={}", idempotencyKey);
                return new PaymentResult(deserializeResponse(record.getResponseBody()), true);
            }

            // IN_FLIGHT: park on the owning thread's future
            log.info("In-flight detected, waiting key={}", idempotencyKey);
            return waitForInFlight(idempotencyKey);
        }

        // New key: race to become the owner via atomic putIfAbsent
        CompletableFuture<PaymentResponse> future = new CompletableFuture<>();
        CompletableFuture<PaymentResponse> existingFuture = inFlight.putIfAbsent(idempotencyKey, future);

        if (existingFuture != null) {
            log.info("Lost putIfAbsent race for key={}, waiting", idempotencyKey);
            return new PaymentResult(waitFor(existingFuture, idempotencyKey), true);
        }

        // Won the race: persist IN_FLIGHT then process
        IdempotencyRecord record = repository.save(IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .state(RequestState.IN_FLIGHT)
                .createdAt(Instant.now())
                .build());

        try {
            PaymentResponse response = paymentProcessor.process(idempotencyKey, request);
            record.setResponseBody(serializeResponse(response));
            record.setStatusCode(201);
            record.setState(RequestState.COMPLETED);
            record.setCompletedAt(Instant.now());
            repository.save(record);
            log.info("Payment completed key={}", idempotencyKey);
            future.complete(response);
            return new PaymentResult(response, false);
        } catch (Exception e) {
            future.completeExceptionally(e);
            repository.delete(record);
            throw e;
        } finally {
            inFlight.remove(idempotencyKey, future);
        }
    }

    private PaymentResult waitForInFlight(String idempotencyKey) {
        CompletableFuture<PaymentResponse> future = inFlight.get(idempotencyKey);
        if (future != null) {
            return new PaymentResult(waitFor(future, idempotencyKey), true);
        }
        // Future completed and removed from map between our DB check and here — re-fetch
        return repository.findByIdempotencyKey(idempotencyKey)
                .filter(r -> r.getState() == RequestState.COMPLETED)
                .map(r -> new PaymentResult(deserializeResponse(r.getResponseBody()), true))
                .orElseThrow(() -> new RuntimeException(
                        "Record in unexpected state for key: " + idempotencyKey));
    }

    private PaymentResponse waitFor(CompletableFuture<PaymentResponse> future, String key) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for key: " + key, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Error in in-flight request for key: " + key, e.getCause());
        }
    }

    public String computeHash(PaymentRequest request) {
        try {
            // TreeMap sorts keys alphabetically → deterministic canonical form
            Map<String, String> canonical = new TreeMap<>();
            canonical.put("amount", request.getAmount().stripTrailingZeros().toPlainString());
            canonical.put("currency", request.getCurrency());
            String json = objectMapper.writeValueAsString(canonical);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute request hash", e);
        }
    }

    private String serializeResponse(PaymentResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize response", e);
        }
    }

    private PaymentResponse deserializeResponse(String json) {
        try {
            return objectMapper.readValue(json, PaymentResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize response", e);
        }
    }
}
