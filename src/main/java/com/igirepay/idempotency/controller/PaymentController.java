package com.igirepay.idempotency.controller;

import com.igirepay.idempotency.dto.PaymentRequest;
import com.igirepay.idempotency.dto.PaymentResult;
import com.igirepay.idempotency.exception.MissingIdempotencyKeyException;
import com.igirepay.idempotency.service.IdempotencyService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/process-payment")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final IdempotencyService idempotencyService;

    public PaymentController(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    public ResponseEntity<?> processPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException("Idempotency-Key header is required.");
        }

        PaymentResult result = idempotencyService.processPayment(idempotencyKey, request);

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.CREATED);
        if (result.cacheHit()) {
            builder = builder.header("X-Cache-Hit", "true");
        }
        return builder.body(result.response());
    }
}
