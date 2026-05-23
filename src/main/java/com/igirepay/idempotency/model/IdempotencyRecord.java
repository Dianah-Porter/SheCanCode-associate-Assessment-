package com.igirepay.idempotency.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private RequestState state;

    public IdempotencyRecord() {}

    private IdempotencyRecord(Builder b) {
        this.idempotencyKey = b.idempotencyKey;
        this.requestHash    = b.requestHash;
        this.responseBody   = b.responseBody;
        this.statusCode     = b.statusCode;
        this.createdAt      = b.createdAt;
        this.completedAt    = b.completedAt;
        this.state          = b.state;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String idempotencyKey;
        private String requestHash;
        private String responseBody;
        private Integer statusCode;
        private Instant createdAt;
        private Instant completedAt;
        private RequestState state;

        public Builder idempotencyKey(String v) { idempotencyKey = v; return this; }
        public Builder requestHash(String v)    { requestHash = v;    return this; }
        public Builder responseBody(String v)   { responseBody = v;   return this; }
        public Builder statusCode(Integer v)    { statusCode = v;     return this; }
        public Builder createdAt(Instant v)     { createdAt = v;      return this; }
        public Builder completedAt(Instant v)   { completedAt = v;    return this; }
        public Builder state(RequestState v)    { state = v;          return this; }
        public IdempotencyRecord build()        { return new IdempotencyRecord(this); }
    }

    public Long getId()                  { return id; }
    public String getIdempotencyKey()    { return idempotencyKey; }
    public String getRequestHash()       { return requestHash; }
    public String getResponseBody()      { return responseBody; }
    public Integer getStatusCode()       { return statusCode; }
    public Instant getCreatedAt()        { return createdAt; }
    public Instant getCompletedAt()      { return completedAt; }
    public RequestState getState()       { return state; }

    public void setIdempotencyKey(String v)  { idempotencyKey = v; }
    public void setRequestHash(String v)     { requestHash = v; }
    public void setResponseBody(String v)    { responseBody = v; }
    public void setStatusCode(Integer v)     { statusCode = v; }
    public void setCreatedAt(Instant v)      { createdAt = v; }
    public void setCompletedAt(Instant v)    { completedAt = v; }
    public void setState(RequestState v)     { state = v; }
}
