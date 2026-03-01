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

- Collect baseline-vs-optimized comparison values (CPU/memory/throughput/latency)
- Build final reporting table with screenshot references
- Prepare final submission report and demo notes
