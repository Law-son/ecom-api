package com.eyarko.ecom.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * In-process lock manager for product-scoped inventory mutations.
 *
 * <p>Locks are keyed by product ID so unrelated products can still be processed concurrently.
 */
@Component
public class InventoryLockManager {
    private final ConcurrentHashMap<Long, ReentrantLock> productLocks = new ConcurrentHashMap<>();

    public <T> T withProductLock(Long productId, Supplier<T> action) {
        ReentrantLock lock = productLocks.computeIfAbsent(productId, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    public void withProductLock(Long productId, Runnable action) {
        withProductLock(productId, () -> {
            action.run();
            return null;
        });
    }
}
