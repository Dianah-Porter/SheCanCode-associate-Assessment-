package com.igirepay.idempotency.scheduler;

import com.igirepay.idempotency.repository.IdempotencyRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class IdempotencyKeyExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyExpiryScheduler.class);

    private final IdempotencyRecordRepository repository;

    @Value("${idempotency.ttl-hours:24}")
    private long ttlHours;

    public IdempotencyKeyExpiryScheduler(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "${idempotency.cleanup-cron:0 */10 * * * *}")
    @Transactional
    public void purgeExpiredKeys() {
        Instant cutoff = Instant.now().minus(ttlHours, ChronoUnit.HOURS);
        int deleted = repository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged {} expired idempotency records (TTL={}h)", deleted, ttlHours);
        }
    }
}
