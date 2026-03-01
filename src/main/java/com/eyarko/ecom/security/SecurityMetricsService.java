package com.eyarko.ecom.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for tracking security metrics and detecting suspicious activity.
 * <p>
 * This service tracks:
 * <ul>
 *   <li>Failed login attempts per IP address</li>
 *   <li>Endpoint access frequency</li>
 *   <li>Token usage patterns</li>
 * </ul>
 * <p>
 * <b>Brute-Force Detection:</b>
 * <ul>
 *   <li>Tracks failed login attempts per IP address</li>
 *   <li>Detects multiple failures within a time window (default: 5 failures in 15 minutes)</li>
 *   <li>Logs warnings when thresholds are exceeded</li>
 * </ul>
 * <p>
 * <b>Access Frequency Tracking:</b>
 * <ul>
 *   <li>Tracks endpoint access counts per user/IP</li>
 *   <li>Maintains rolling window statistics</li>
 *   <li>Useful for detecting unusual access patterns</li>
 * </ul>
 */
@Service
public class SecurityMetricsService {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SecurityMetricsService.class);
    
    // Brute-force detection thresholds
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int BRUTE_FORCE_WINDOW_MINUTES = 15;
    
    /**
     * Map tracking failed login attempts per IP address.
     * Key: IP address, Value: Attempt record with count and timestamp
     */
    private final ConcurrentHashMap<String, FailedAttemptRecord> failedAttempts = new ConcurrentHashMap<>();
    
    /**
     * Map tracking endpoint access frequency.
     * Key: "email:endpoint" or "ip:endpoint", Value: Access count
     */
    private final ConcurrentHashMap<String, Integer> endpointAccessCounts = new ConcurrentHashMap<>();

    /**
     * Snapshot-friendly alert history for diagnostics.
     * CopyOnWriteArrayList is optimized for reads and safe iteration under concurrent writes.
     */
    private final CopyOnWriteArrayList<SecurityAlertRecord> securityAlerts = new CopyOnWriteArrayList<>();
    
    /**
     * Records a failed authentication attempt.
     * <p>
     * Tracks attempts per IP address and logs warnings when brute-force thresholds are exceeded.
     *
     * @param email attempted email (maybe null)
     * @param ipAddress client IP address
     */
    public void recordFailedLogin(String email, String ipAddress) {
        Instant now = Instant.now();
        FailedAttemptRecord record = failedAttempts.compute(ipAddress, (key, existing) -> {
            if (existing == null) {
                return new FailedAttemptRecord(1, now);
            }
            long minutesSinceFirst = ChronoUnit.MINUTES.between(existing.firstAttempt(), now);
            if (minutesSinceFirst > BRUTE_FORCE_WINDOW_MINUTES) {
                // Reset rolling window when the previous attempt is too old.
                return new FailedAttemptRecord(1, now);
            }
            return new FailedAttemptRecord(existing.count() + 1, existing.firstAttempt());
        });
        
        // Check if brute-force threshold is exceeded
        if (record != null && record.count() >= MAX_FAILED_ATTEMPTS) {
            long minutesSinceFirst = ChronoUnit.MINUTES.between(record.firstAttempt(), now);
            if (minutesSinceFirst <= BRUTE_FORCE_WINDOW_MINUTES) {
                logger.warn("SECURITY_ALERT: Potential brute-force attack detected | ip={} | email={} | failedAttempts={} | windowMinutes={} | timestamp={}",
                    ipAddress, email != null ? email : "unknown", record.count(), minutesSinceFirst, now);
                securityAlerts.add(new SecurityAlertRecord(
                    now,
                    "Brute-force threshold exceeded",
                    ipAddress,
                    email != null ? email : "unknown",
                    record.count()
                ));
            }
        }
    }
    
    /**
     * Records successful authentication, resetting failed attempt counter for the IP.
     *
     * @param ipAddress client IP address
     */
    public void recordSuccessfulLogin(String ipAddress) {
        failedAttempts.remove(ipAddress);
    }
    
    /**
     * Records endpoint access for frequency tracking.
     *
     * @param email user email (null for anonymous)
     * @param ipAddress client IP address
     * @param endpoint accessed endpoint
     */
    public void recordEndpointAccess(String email, String ipAddress, String endpoint) {
        // Track by user email if authenticated
        if (email != null) {
            String key = email + ":" + endpoint;
            endpointAccessCounts.compute(key, (k, count) -> count == null ? 1 : count + 1);
        }
        
        // Track by IP address
        String ipKey = ipAddress + ":" + endpoint;
        endpointAccessCounts.compute(ipKey, (k, count) -> count == null ? 1 : count + 1);
    }

    /**
     * Returns the most recent security alerts for diagnostics and support triage.
     *
     * @param limit maximum number of latest alerts to return
     * @return immutable snapshot of recent alerts
     */
    public List<SecurityAlertRecord> getRecentSecurityAlerts(int limit) {
        if (limit <= 0 || securityAlerts.isEmpty()) {
            return List.of();
        }
        int from = Math.max(0, securityAlerts.size() - limit);
        return List.copyOf(securityAlerts.subList(from, securityAlerts.size()));
    }

    @Scheduled(fixedDelay = 300000)
    void cleanupMetricsWindows() {
        Instant cutoff = Instant.now().minus(BRUTE_FORCE_WINDOW_MINUTES, ChronoUnit.MINUTES);
        failedAttempts.entrySet().removeIf(entry -> entry.getValue().firstAttempt().isBefore(cutoff));
        securityAlerts.removeIf(alert -> alert.timestamp().isBefore(cutoff));
    }
    
    /**
     * Internal record for tracking failed login attempts.
     */
    private record FailedAttemptRecord(int count, Instant firstAttempt) {}

    public record SecurityAlertRecord(
        Instant timestamp,
        String reason,
        String ipAddress,
        String email,
        int failedAttempts
    ) {}
}

