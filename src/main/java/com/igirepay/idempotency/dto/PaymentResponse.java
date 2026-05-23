package com.igirepay.idempotency.dto;

public class PaymentResponse {

    private String status;
    private String idempotencyKey;
    private String transactionId;

    public PaymentResponse() {}

    public PaymentResponse(String status, String idempotencyKey, String transactionId) {
        this.status         = status;
        this.idempotencyKey = idempotencyKey;
        this.transactionId  = transactionId;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String status;
        private String idempotencyKey;
        private String transactionId;

        public Builder status(String v)         { status = v;         return this; }
        public Builder idempotencyKey(String v) { idempotencyKey = v; return this; }
        public Builder transactionId(String v)  { transactionId = v;  return this; }
        public PaymentResponse build()          { return new PaymentResponse(status, idempotencyKey, transactionId); }
    }

    public String getStatus()         { return status; }
    public void setStatus(String v)   { status = v; }

    public String getIdempotencyKey()          { return idempotencyKey; }
    public void setIdempotencyKey(String v)    { idempotencyKey = v; }

    public String getTransactionId()           { return transactionId; }
    public void setTransactionId(String v)     { transactionId = v; }
}
