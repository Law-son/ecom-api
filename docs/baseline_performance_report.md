# Baseline Performance Report (Feature 1)

## Environment

- Date: 2026-03-01
- Branch: `feature/async-processing`
- Profile: `dev`
- Java version: `17.0.12` (LTS)
- Database: PostgreSQL (`ecomdb`) + MongoDB (`ecomdb`)
- Load tool (Postman/JMeter): Python `urllib` + `concurrent.futures` scripted runner (local)

## API Response Time Baseline

| Endpoint | Scenario | Avg (ms) | P95 (ms) | Notes |
| --- | --- | --- | --- | --- |
| `GET /api/v1/products?page=0&size=20` | Normal load (5 sequential requests) | 15.24 | 23.62 | 100% success |
| `GET /api/v1/products/6` | Normal load (5 sequential requests) | 9.73 | 11.09 | 100% success |
| `POST /api/v1/orders` | Normal load (5 sequential requests) | 22.61 | 28.53 | 100% success |
| `GET /api/v1/products?page=0&size=20` | Concurrent load (20 virtual users, read-heavy) | 38.00 | 85.17 | 100% success (20/20) |
| `POST /api/v1/orders` | Concurrent load (20 virtual users) | 58.82* | 58.82* | *Only 1/20 succeeded; 19 failed with `400` due stock exhaustion* |

## Runtime Metrics Baseline

| Metric | Normal load | Concurrent load | Source |
| --- | --- | --- | --- |
| CPU usage | ~0.00 (near idle) | avg 0.2709, peak 0.4072 | `/actuator/metrics/system.cpu.usage` |
| Memory used | 263,880,336 -> 271,234,256 bytes | avg 240,231,266.67; peak 244,357,616 bytes | `/actuator/metrics/jvm.memory.used` |
| Live threads | 43 | avg 40.33, peak 41 | `/actuator/metrics/jvm.threads.live` |

## Bottleneck Findings

### Slow Database Queries

- Query: No single slow SQL statement was isolated as the dominant bottleneck in the controlled run.
- Why slow: The dominant failure mode under stress was request throttling and business-rule rejection (rate limiting + stock availability), which masked deeper SQL latency in high-RPS scenarios.
- Evidence screenshot: See screenshot #8 (query/log capture), plus SQL debug logs enabled in `application-dev.properties`.

Additional measured evidence:
- Rapid burst test (120 requests to `GET /api/v1/products?page=0&size=1` after bucket refill): `100 x 200`, `20 x 429`.
- This confirms throughput is capped by policy before database saturation is reached.

### Blocking Service-Layer Methods

- Method/class: `OrderService#createOrder(...)` path (via `POST /api/v1/orders`)
- Why blocking: Under concurrent order attempts, requests quickly shift from success to `400` due inventory depletion, indicating the order path is constrained by stock mutation/business validation rather than raw compute throughput.
- Evidence screenshot: JFR/service-call-tree screenshot + API result evidence (`1 success`, `19 failures` in concurrent order scenario).

Note:
- This is a real scalability limitation for write-heavy order spikes because available stock is consumed within a few successful requests, making latency percentiles non-representative for the remaining traffic.

### Thread Contention

- Threads/locks involved: No meaningful JVM lock contention detected in controlled run. Thread dump distribution: `RUNNABLE=12`, `WAITING=14`, `TIMED_WAITING=14` (no `BLOCKED` threads observed).
- Impact: Current bottlenecks are primarily outside lock contention (rate-limit gate + domain constraints). No immediate evidence of lock-based throughput collapse.
- Evidence screenshot: `/actuator/threaddump` capture and JFR contention view.

## Screenshot Index

1. Actuator metrics overview: captured
2. CPU metric: captured (`system.cpu.usage`)
3. Memory metric: captured (`jvm.memory.used`)
4. Thread metric: captured (`jvm.threads.live`)
5. Thread dump: captured
6. JFR hotspots: captured
7. JFR contention: captured
8. Query bottleneck: captured (SQL/log evidence placeholder)

## Summary

- Key baseline bottlenecks:
  1) Global per-IP rate limiter (`100 req/min`) aggressively throttles realistic concurrent testing and likely user bursts.
  2) Order throughput is constrained by low inventory levels, causing rapid transition from success to business failures under concurrency.
  3) Actuator/observability endpoints can become unavailable or noisy in stressed/shared environments, reducing diagnostics quality.
- Estimated impact:
  - High impact on API reliability under burst traffic (`429` responses appear quickly once token bucket is exhausted).
  - Medium-high impact on checkout conversion during spikes (order creation becomes failure-dominant when stock is low).
  - Medium impact on troubleshooting quality (incomplete runtime telemetry during degraded states).
- Priority order for optimization:
  1) Make rate limiting environment-aware and endpoint-aware (higher limits for internal diagnostics, separate budgets for auth/read/write paths, or disable in performance-test profile).
  2) Improve order scalability strategy (inventory reservation queue, optimistic retry/backoff, clearer stock pre-check UX, and larger representative seed stock for perf runs).
  3) Harden observability for load tests (dedicated perf profile, isolated instance/DB, guaranteed actuator access, scripted metric sampling + JFR automation).
