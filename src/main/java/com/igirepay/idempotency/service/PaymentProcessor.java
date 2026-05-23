package com.igirepay.idempotency.service;

import com.igirepay.idempotency.dto.PaymentRequest;
import com.igirepay.idempotency.dto.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    public PaymentResponse process(String idempotencyKey, PaymentRequest request) {
        log.info("Processing payment key={} amount={} currency={}",
                idempotencyKey, request.getAmount(), request.getCurrency());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payment processing interrupted", e);
        }
        String status = "Charged "
                + request.getAmount().stripTrailingZeros().toPlainString()
                + " " + request.getCurrency();
        return PaymentResponse.builder()
                .status(status)
                .idempotencyKey(idempotencyKey)
                .transactionId(UUID.randomUUID().toString())
                .build();
    }
}
