package com.igirepay.idempotency.dto;

public record PaymentResult(PaymentResponse response, boolean cacheHit) {}
