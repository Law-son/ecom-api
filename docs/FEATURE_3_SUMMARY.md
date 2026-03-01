# Feature 3: Concurrency & Thread Safety - Summary

## Status

- Branch: `feature/concurrency-thread-safety`
- Implementation status: In progress (core concurrency hardening completed)
- Validation status: Compile + concurrent smoke tests completed

## Completed Items

### Thread-Safe Collections and Atomic Updates

- Replaced shared mutable digest synchronization with thread-local hashing in `TokenBlacklistService`:
  - `ThreadLocal<MessageDigest>` instead of one synchronized `MessageDigest`
  - atomic cleanup with `blacklist.remove(tokenHash, expiration)`
- Improved `SecurityMetricsService` concurrent behavior:
  - endpoint counters updated via `ConcurrentHashMap.compute(...)`
  - failed-attempt window updates handled atomically via `compute(...)`
  - added `CopyOnWriteArrayList` for snapshot-safe recent alert tracking
  - added scheduled cleanup of stale failed-attempt and alert records
- Explicitly typed rate-limit bucket storage as `ConcurrentHashMap<String, Bucket>` in `RateLimitFilter`

### Shared Resource Protection

- Added `InventoryLockManager`:
  - product-scoped lock map using `ConcurrentHashMap<Long, ReentrantLock>`
  - helper methods to execute inventory mutations inside per-product critical sections
- Added pessimistic DB row locking for inventory write paths:
  - `InventoryRepository.findByProductIdForUpdate(...)` with `@Lock(PESSIMISTIC_WRITE)`
- Applied combined lock strategy (in-process lock + DB row lock) to:
  - `OrderService` (reserve/restore inventory)
  - `AsyncOrderService` (reserve/restore inventory)
  - `InventoryService` (adjust inventory)

### Thread Pool Tuning Foundations

- Added configurable async executor properties:
  - `app.async.executor.*` in `application.properties`
  - mapped via new `AsyncExecutorProperties`
- Updated `AsyncConfig`:
  - externalized pool sizing/timing
  - added `CallerRunsPolicy` for backpressure
  - bound executor metrics via Micrometer for runtime observation

## Verification Evidence

### Build Validation

- Command: `mvn -Dmaven.test.skip=true compile`
- Result: success after applying Feature 3 changes

### Concurrent Order Smoke Test (Quick)

- Scenario:
  - product stock: 4
  - concurrent order attempts: 7
- Result:
  - success: 4
  - rejected (`400` insufficient stock): 3
  - final stock: 0
  - oversell detected: false
  - negative stock detected: false

### Concurrent Order Smoke Test (Heavier)

- Timestamp: 2026-03-01 11:42:26
- Scenario:
  - product: `Blender` (`id=5`)
  - initial stock: 3
  - concurrent order attempts: 30
- Result:
  - `200`: 3
  - `400`: 27
  - final stock: 0
  - oversell detected: false
  - negative stock detected: false

### Runtime Metrics Snapshot (Heavier Test)

- Before run:
  - `jvm.threads.live`: 44
  - `jvm.memory.used`: 310,585,672 bytes
- After run:
  - `jvm.threads.live`: 65
  - `jvm.memory.used`: 284,681,664 bytes

## Remaining Feature 3 Work

- Validate deadlock safety under repeated mixed operations (order create + order cancel + inventory adjust)
- Run multi-configuration thread pool tuning tests and compare:
  - CPU usage
  - memory usage
  - throughput
- Document and justify selected final thread-pool configuration
- Add a concise concurrency test runbook for repeatability

## Notes

- Current smoke tests strongly indicate race-condition mitigation for oversell is effective.
- Order rejection under load is now bounded by stock availability rather than inconsistent concurrent writes.
