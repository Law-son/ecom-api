# Feature 5: Metrics Collection & Reporting - Progress Summary

## Branch

- `feature/metrics-reporting`

## Scope Completed

### Runtime Metrics Collection

- Verified Actuator metrics endpoint exposure via `application.properties`:
  - `management.endpoints.web.exposure.include=...metrics...`
- JVM/system metric binders are configured in `PerformanceMonitoringConfig`:
  - `JvmThreadMetrics`
  - `JvmMemoryMetrics`
  - `JvmGcMetrics`
  - `ProcessorMetrics`
- Confirmed `http.server.requests` metric availability at runtime.

### Custom Metrics

- Added `ApplicationMetricsService` using `MeterRegistry`.
- Implemented custom counter:
  - `app.orders.processed.total` (increments on successful order creation)
- Implemented custom timer:
  - `app.orders.create.duration` (records order creation duration)
- Enabled method-level timed instrumentation using `@Timed`:
  - `OrderService.createOrder` -> `app.orders.create.timed`
  - `OrderService.listOrders` -> `app.orders.list.timed`
  - `ProductService.getProduct` -> `app.products.get.timed`
  - `ProductService.listProducts` -> `app.products.list.timed`
  - `ReviewService.createReview` -> `app.reviews.create.timed`
  - `ReviewService.listReviews` -> `app.reviews.list.timed`
- Added `TimedAspect` bean so `@Timed` annotations are emitted to Micrometer.

### Metrics Distribution Configuration

Added percentile/histogram support for key metrics in `application.properties`:

- `management.metrics.distribution.percentiles-histogram.http.server.requests=true`
- `management.metrics.distribution.percentiles.app.orders.create.duration=0.5,0.95`
- `management.metrics.distribution.percentiles.app.products.list.timed=0.5,0.95`
- `management.metrics.distribution.percentiles.app.reviews.list.timed=0.5,0.95`

## Runtime Verification (Local)

Validation run:

- Start app on `:8083` (dev profile)
- Trigger representative endpoints:
  - `GET /api/v1/products?page=0&size=20`
  - `GET /api/v1/reviews?page=0&size=10`
  - `POST /api/v1/orders` (authenticated)
- Check `/actuator/metrics` names list

Observed metric presence:

- `http.server.requests`: present
- `app.orders.processed.total`: present
- `app.orders.create.duration`: present
- `app.orders.create.timed`: present
- `app.products.list.timed`: present
- `app.reviews.list.timed`: present

## Build Validation

- `mvn -Dmaven.test.skip=true compile` -> **BUILD SUCCESS**

## Remaining Feature 5 Work

### Consolidated Metrics Report (2026-03-01)

Primary evidence sources:
- Baseline: `docs/baseline_performance_report.md`
- Optimized A/B: `docs/feature4_ab_metrics.json`
- Throughput matrix: `docs/thread_pool_tuning_matrix.jsonl`

#### A) Baseline vs Optimized Runtime + Latency

| Metric | Baseline | Optimized | Delta (%) | Evidence |
| --- | ---: | ---: | ---: | --- |
| `GET /api/v1/products?page=0&size=20` avg latency | 15.05 ms | 14.09 ms | +6.38% | Baseline §3.2 / `feature4_ab_metrics.json` |
| `GET /api/v1/products/3` avg latency | 18.98 ms | 9.36 ms | +50.68% | Baseline §3.2 / `feature4_ab_metrics.json` |
| `GET /api/v1/reviews?page=0&size=20` avg latency | 428.15 ms | 401.80 ms | +6.15% | Baseline §3.2 / `feature4_ab_metrics.json` |
| `system.cpu.usage` snapshot | 0.0 | 0.0 | 0.00% | Baseline §3.1 / `feature4_ab_metrics.json` |
| `jvm.memory.used` snapshot | 256,089,144 B | 270,201,776 B | -5.51% | Baseline §3.1 / `feature4_ab_metrics.json` |
| `jvm.threads.live` snapshot | 42 | 43 | -2.38% | Baseline §3.1 / `feature4_ab_metrics.json` |

#### B) Throughput Comparison (Feature 3 Tuning Matrix)

| Profile | Throughput (req/s) | Avg Latency (ms) | p95 Latency (ms) | Error Rate |
| --- | ---: | ---: | ---: | ---: |
| Small (`2/4/20`) | 25.90 | 338.09 | 1575.47 | 0.0% |
| Medium (`5/10/100`) | 25.28 | 359.34 | 1751.01 | 0.0% |
| Large (`10/20/300`) | 24.15 | 355.29 | 1681.01 | 0.0% |

Selected profile:
- `corePoolSize=2`, `maxPoolSize=4`, `queueCapacity=20` (best throughput and p95 in measured matrix).

#### C) Screenshot/Artifact References

- Baseline screenshots:
  - `docs/screenshots/baseline/api-response-times-normal.png`
  - `docs/screenshots/baseline/api-response-times-concurrent.png`
  - `docs/screenshots/baseline/cpu-usage.png`
  - `docs/screenshots/baseline/jvm-memory.png`
  - `docs/screenshots/baseline/jmeter-results.png` (pending if JMeter is not installed)
- Consolidated measurement artifacts:
  - `docs/feature4_ab_metrics.json`
  - `docs/thread_pool_tuning_matrix.jsonl`
  - `docs/FEATURE_4_SUMMARY.md` (A/B delta table)

#### D) Summary of Improvements

- Product read paths improved measurably, with strongest gain on product detail latency.
- Review path improved modestly but remains the highest-latency endpoint and should be prioritized for next optimization pass.
- Runtime snapshots are generally stable; memory/thread samples vary slightly across probe windows.
- Throughput under the Feature 3 mixed workload is stable across profiles, with the small profile leading in this environment.

### Remaining Feature 5 Work

- Prepare final submission report and demo notes
