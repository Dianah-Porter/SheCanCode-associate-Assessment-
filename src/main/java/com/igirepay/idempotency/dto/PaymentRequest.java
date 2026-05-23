package com.igirepay.idempotency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class PaymentRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency must not be blank")
    private String currency;

    public BigDecimal getAmount()      { return amount; }
    public void setAmount(BigDecimal v) { amount = v; }

    public String getCurrency()        { return currency; }
    public void setCurrency(String v)  { currency = v; }
}
