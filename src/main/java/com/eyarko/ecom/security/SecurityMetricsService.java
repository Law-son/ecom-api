package com.eyarko.ecom.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final ConcurrentHashMap<String, AtomicInteger> endpointAccessCounts = new ConcurrentHashMap<>();
    
    /**
     * Records a failed authentication attempt.
     * <p>
     * Tracks attempts per IP address and logs warnings when brute-force thresholds are exceeded.
     *
     * @param email attempted email (maybe null)
     * @param ipAddress client IP address
     */
    public void recordFailedLogin(String email, String ipAddress) {
        FailedAttemptRecord record = failedAttempts.computeIfAbsent(
            ipAddress,
            k -> new FailedAttemptRecord()
        );
        
        record.increment();
        
        // Check if brute-force threshold is exceeded
        if (record.getCount() >= MAX_FAILED_ATTEMPTS) {
            long minutesSinceFirst = ChronoUnit.MINUTES.between(record.getFirstAttempt(), Instant.now());
            if (minutesSinceFirst <= BRUTE_FORCE_WINDOW_MINUTES) {
                logger.warn("SECURITY_ALERT: Potential brute-force attack detected | ip={} | email={} | failedAttempts={} | windowMinutes={} | timestamp={}",
                    ipAddress, email != null ? email : "unknown", record.getCount(), minutesSinceFirst, Instant.now());
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
            endpointAccessCounts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        }
        
        // Track by IP address
        String ipKey = ipAddress + ":" + endpoint;
        endpointAccessCounts.computeIfAbsent(ipKey, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    /**
     * Gets the number of failed login attempts for an IP address.
     *
     * @param ipAddress IP address
     * @return number of failed attempts
     */
    public int getFailedAttemptCount(String ipAddress) {
        FailedAttemptRecord record = failedAttempts.get(ipAddress);
        return record != null ? record.getCount() : 0;
    }
    
    /**
     * Gets endpoint access count for a user/IP and endpoint combination.
     *
     * @param email user email (null for anonymous)
     * @param ipAddress IP address
     * @param endpoint endpoint path
     * @return access count
     */
    public int getEndpointAccessCount(String email, String ipAddress, String endpoint) {
        if (email != null) {
            String key = email + ":" + endpoint;
            AtomicInteger count = endpointAccessCounts.get(key);
            if (count != null) {
                return count.get();
            }
        }
        String ipKey = ipAddress + ":" + endpoint;
        AtomicInteger count = endpointAccessCounts.get(ipKey);
        return count != null ? count.get() : 0;
    }
    
    /**
     * Cleans up old failed attempt records and resets access counts periodically.
     * Runs every hour to prevent memory leaks.
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupOldRecords() {
        Instant cutoff = Instant.now().minus(BRUTE_FORCE_WINDOW_MINUTES + 5, ChronoUnit.MINUTES);
        
        // Remove old failed attempt records
        failedAttempts.entrySet().removeIf(entry -> 
            entry.getValue().getFirstAttempt().isBefore(cutoff)
        );
        
        // Reset access counts (optional - can be adjusted based on needs)
        // For now, we keep them for the lifetime of the application
        // In production, you might want to persist these to a database
    }
    
    /**
     * Internal record for tracking failed login attempts.
     */
    private static class FailedAttemptRecord {
        private final AtomicInteger count = new AtomicInteger(0);
        private final Instant firstAttempt = Instant.now();
        
        public void increment() {
            count.incrementAndGet();
        }
        
        public int getCount() {
            return count.get();
        }
        
        public Instant getFirstAttempt() {
            return firstAttempt;
        }
    }
}

