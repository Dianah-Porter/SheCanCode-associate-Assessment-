package com.igirepay.idempotency;

import com.igirepay.idempotency.dto.PaymentResponse;
import com.igirepay.idempotency.model.IdempotencyRecord;
import com.igirepay.idempotency.model.RequestState;
import com.igirepay.idempotency.repository.IdempotencyRecordRepository;
import com.igirepay.idempotency.scheduler.IdempotencyKeyExpiryScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IdempotencyRecordRepository repository;

    @Autowired
    private IdempotencyKeyExpiryScheduler scheduler;

    @BeforeEach
    void cleanDb() {
        repository.deleteAll();
    }

    // ── Story 1: happy path ───────────────────────────────────────────────────

    @Test
    void happyPath_returns201WithCorrectBody() {
        ResponseEntity<PaymentResponse> response = sendRequest(UUID.randomUUID().toString(), 100, "RWF");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("Charged 100 RWF");
        assertThat(response.getBody().getTransactionId()).isNotBlank();
        assertThat(response.getHeaders().getFirst("X-Cache-Hit")).isNull();
    }

    // ── Story 2: cache hit ────────────────────────────────────────────────────

    @Test
    void duplicateRequest_returnsCachedResponse_withCacheHitHeader_quickly() {
        String key = UUID.randomUUID().toString();

        long start1 = System.currentTimeMillis();
        ResponseEntity<PaymentResponse> first = sendRequest(key, 100, "RWF");
        long duration1 = System.currentTimeMillis() - start1;

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(duration1).isGreaterThanOrEqualTo(2000L);

        long start2 = System.currentTimeMillis();
        ResponseEntity<PaymentResponse> second = sendRequest(key, 100, "RWF");
        long duration2 = System.currentTimeMillis() - start2;

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getHeaders().getFirst("X-Cache-Hit")).isEqualTo("true");
        assertThat(second.getBody().getTransactionId()).isEqualTo(first.getBody().getTransactionId());
        assertThat(second.getBody().getStatus()).isEqualTo(first.getBody().getStatus());
        assertThat(duration2).isLessThan(500L);
    }

    // ── Story 3: key reuse with different body ────────────────────────────────

    @Test
    void differentBodySameKey_returns422WithExactMessage() {
        String key = UUID.randomUUID().toString();
        sendRequest(key, 100, "RWF");

        ResponseEntity<String> conflict = sendRaw(key, 500, "RWF");

        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(conflict.getBody()).contains("Idempotency key already used for a different request body.");
    }

    // ── Missing header → 400 ─────────────────────────────────────────────────

    @Test
    void missingIdempotencyKey_returns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"amount": 100, "currency": "RWF"}""";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url(), entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Idempotency-Key header is required");
    }

    // ── Bonus: concurrent in-flight ───────────────────────────────────────────

    @Test
    void concurrentIdenticalRequests_bothSucceed_sameTransactionId_secondIsCacheHit()
            throws Exception {
        String key = UUID.randomUUID().toString();

        AtomicReference<ResponseEntity<PaymentResponse>> responseA = new AtomicReference<>();
        AtomicReference<ResponseEntity<PaymentResponse>> responseB = new AtomicReference<>();

        // A starts first; B fires 300 ms later so A is definitely in the 2-second sleep
        CompletableFuture<Void> futureA = CompletableFuture.runAsync(() ->
                responseA.set(sendRequest(key, 100, "RWF")));

        Thread.sleep(300);

        CompletableFuture<Void> futureB = CompletableFuture.runAsync(() ->
                responseB.set(sendRequest(key, 100, "RWF")));

        CompletableFuture.allOf(futureA, futureB).get(15, TimeUnit.SECONDS);

        assertThat(responseA.get().getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(responseB.get().getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(responseA.get().getBody().getTransactionId())
                .isEqualTo(responseB.get().getBody().getTransactionId());

        assertThat(responseB.get().getHeaders().getFirst("X-Cache-Hit")).isEqualTo("true");
    }

    // ── Developer's Choice: TTL expiry ───────────────────────────────────────

    @Test
    void expiredKey_isPurged_andBehavesAsNewRequest() {
        String key = UUID.randomUUID().toString();

        // Plant a COMPLETED record that is 25 hours old
        repository.save(IdempotencyRecord.builder()
                .idempotencyKey(key)
                .requestHash("stale-hash")
                .responseBody("{\"status\":\"old\",\"idempotencyKey\":\"" + key + "\",\"transactionId\":\"old-tx\"}")
                .statusCode(201)
                .state(RequestState.COMPLETED)
                .createdAt(Instant.now().minus(25, ChronoUnit.HOURS))
                .completedAt(Instant.now().minus(25, ChronoUnit.HOURS))
                .build());

        assertThat(repository.findByIdempotencyKey(key)).isPresent();

        // Trigger the sweeper
        scheduler.purgeExpiredKeys();

        assertThat(repository.findByIdempotencyKey(key)).isEmpty();

        // Now the key should be usable as a fresh request
        ResponseEntity<PaymentResponse> response = sendRequest(key, 100, "RWF");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getFirst("X-Cache-Hit")).isNull();
        assertThat(response.getBody().getTransactionId()).isNotEqualTo("old-tx");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<PaymentResponse> sendRequest(String key, int amount, String currency) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", key);
        String body = String.format("{\"amount\": %d, \"currency\": \"%s\"}", amount, currency);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(url(), entity, PaymentResponse.class);
    }

    private ResponseEntity<String> sendRaw(String key, int amount, String currency) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", key);
        String body = String.format("{\"amount\": %d, \"currency\": \"%s\"}", amount, currency);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(url(), entity, String.class);
    }

    private String url() {
        return "http://localhost:" + port + "/process-payment";
    }
}
