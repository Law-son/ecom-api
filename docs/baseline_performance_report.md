# Baseline Performance Report (Before Optimization)

**Date:** 2026-03-01  
**Environment:** Development  
**Java Version:** 17+  
**Spring Boot Version:** 3.3.5

---

## 1. Executive Summary

This report documents the baseline performance metrics of the e-commerce system before optimization. It identifies bottlenecks in database queries, service-layer methods, thread contention, and resource utilization.

---

## 2. Profiling Setup

### 2.1 Tools Used
- **Spring Boot Actuator** - Runtime metrics collection
- **Java Flight Recorder (JFR)** - JVM profiling (JDK 21 built-in)
- **Hibernate Statistics** - SQL query performance
- **Postman/JMeter** - API response time measurement
- **Micrometer** - Custom metrics tracking

### 2.2 Actuator Endpoints Enabled
- `/actuator/health` - Application health status
- `/actuator/metrics` - Runtime metrics
- `/actuator/threaddump` - Thread analysis
- `/actuator/heapdump` - Memory analysis
- `/actuator/info` - Application information
- `/actuator/env` - Environment properties
- `/actuator/loggers` - Log level management

### 2.3 Hibernate SQL Logging
- SQL statement logging: **ENABLED**
- SQL formatting: **ENABLED**
- SQL comments: **ENABLED**
- Statistics generation: **ENABLED**
- Parameter binding trace: **ENABLED**

---

## 3. Baseline Metrics Collection

### 3.1 JVM Metrics

#### Memory Usage (Heap)
```
Initial Heap Size: [NOT CAPTURED IN QUICK RUN]
Max Heap Size: [NOT CAPTURED IN QUICK RUN]
Used Heap: ~268 MB before normal load, ~278 MB after normal load
Committed Heap: [NOT CAPTURED IN QUICK RUN]
```

**Screenshot Location:** `docs/screenshots/baseline/jvm-memory.png`

#### Thread Metrics
```
Active Threads: 44 (normal load)
Peak Threads: 45 (concurrent read run sample)
Daemon Threads: [NOT CAPTURED IN QUICK RUN]
Thread States Distribution: [SEE THREADEDUMP SCREENSHOT]
```

**Screenshot Location:** `docs/screenshots/baseline/jvm-threads.png`

#### CPU Usage
```
System CPU Usage: 0.0 before normal load, ~0.199 after normal load
Process CPU Usage: [NOT CAPTURED IN QUICK RUN]
CPU Load Average: [NOT CAPTURED IN QUICK RUN]
```

**Screenshot Location:** `docs/screenshots/baseline/cpu-usage.png`

#### Garbage Collection
```
GC Collections (Young Gen): [TO BE MEASURED]
GC Collections (Old Gen): [TO BE MEASURED]
GC Time (Young Gen): [TO BE MEASURED]
GC Time (Old Gen): [TO BE MEASURED]
```

**Screenshot Location:** `docs/screenshots/baseline/gc-metrics.png`

---

### 3.2 API Response Times (Normal Load)

Test using Postman with single requests:

| Endpoint | Method | Avg Response Time | Min | Max | Status |
|----------|--------|-------------------|-----|-----|--------|
| `/api/v1/products?page=0&size=20` | GET | 15.05 ms | - | - | 200 (10/10) |
| `/api/v1/products/3` | GET | 18.98 ms | - | - | 200 (10/10) |
| `/api/v1/orders` | GET | [TO BE MEASURED] | - | - | - |
| `/api/v1/orders` | POST | N/A | - | - | 400 (stock unavailable in current dataset) |
| `/api/v1/users` | GET | [TO BE MEASURED] | - | - | - |
| `/api/v1/categories` | GET | [TO BE MEASURED] | - | - | - |
| `/api/v1/reviews` | GET | [TO BE MEASURED] | - | - | - |

Normal-load note:
- `POST /api/v1/orders` is currently blocked by test-data state (`stockQuantity=0` across products). Replenish inventory before re-running order latency baseline.

**Screenshot Location:** `docs/screenshots/baseline/api-response-times-normal.png`

---

### 3.3 API Response Times (Concurrent Load)

Test using JMeter with 50 concurrent users, 100 requests each:

| Endpoint | Method | Avg Response Time | Throughput (req/s) | Error Rate | 90th Percentile | 95th Percentile |
|----------|--------|-------------------|-------------------|------------|-----------------|-----------------|
| `/api/v1/products?page=0&size=20` | GET | 18.49 ms | N/A (quick concurrent probe) | 6.0% (3/50 = 429) | 35.96 ms | 47.45 ms |
| `/api/v1/orders` | POST | N/A | N/A | 100% blocked by stock state | N/A | N/A |
| `/api/v1/users` | GET | [TO BE MEASURED] | - | - | - | - |

Concurrent-load note:
- Quick run used 25 workers x 2 loops for product list/detail endpoints to avoid triggering excessive rate-limit failures.
- Rate limiter (`429`) still appears under burst traffic and should be considered when interpreting throughput/error-rate values.

**Screenshot Location:** `docs/screenshots/baseline/api-response-times-concurrent.png`

---

## 4. Database Query Analysis

### 4.1 Slow Queries Identified

#### Query 1: Product list with category join + pagination
```sql
select
    p1_0.product_id, p1_0.avg_rating, p1_0.category_id, c1_0.category_id,
    c1_0.created_at, c1_0.category_name, c1_0.version, p1_0.created_at,
    p1_0.description, p1_0.image_url, p1_0.name, p1_0.price,
    p1_0.review_count, p1_0.version
from products p1_0
join categories c1_0 on c1_0.category_id = p1_0.category_id
order by p1_0.name
fetch first ? rows only
```
- **Execution Time:** ~17 ms (Hibernate statistics log sample)
- **Rows Returned:** 7
- **Issue:** Query itself is not slow in sampled run; endpoint latency was higher than SQL time, indicating additional non-DB overhead in request path.

**Screenshot Location:** `docs/screenshots/baseline/slow-query-1.png`

#### Query 2: Inventory quantity batch lookup for product list
```sql
SELECT product_id AS productId, quantity AS quantity
FROM inventory
WHERE product_id IN (?, ?, ?, ?, ?, ?, ?)
```
- **Execution Time:** ~3 ms (Hibernate statistics log sample)
- **Rows Returned:** 5
- **Issue:** No immediate SQL bottleneck observed; current batch lookup pattern is efficient and avoids per-product inventory queries.

**Screenshot Location:** `docs/screenshots/baseline/slow-query-2.png`

### 4.2 N+1 Query Problems
- **Location:** `ProductService.listProducts()` + `InventoryRepository.findQuantitiesByProductIds(...)`
- **Description:** N+1 pattern for inventory lookup appears mitigated: product IDs are collected once and inventory is fetched with a single `IN` query.
- **Impact:** Reduced DB round-trips compared to per-row inventory fetch.

**Screenshot Location:** `docs/screenshots/baseline/n-plus-1-queries.png`

### 4.3 Hibernate Statistics
```
Total Queries Executed: [PARTIAL SAMPLE - REQUEST WINDOW ONLY]
Total Query Execution Time: Product list query ~17 ms, inventory batch query ~3 ms
Cache Hit Ratio: [NOT CAPTURED IN THIS QUICK RUN]
Second Level Cache Hits: [NOT CAPTURED IN THIS QUICK RUN]
Second Level Cache Misses: [NOT CAPTURED IN THIS QUICK RUN]
```

**Screenshot Location:** `docs/screenshots/baseline/hibernate-stats.png`

---

## 5. Service Layer Analysis

### 5.1 Blocking Methods Identified

#### Method 1: `ReviewService.listReviews(...)`
- **Execution Time:** ~207.76 ms avg, ~255.78 ms p95 (sampled via `GET /api/v1/reviews?productId=3&page=0&size=20`)
- **Blocking Reason:** MongoDB read + pagination + object mapping on review path.
- **Impact:** Highest-latency read endpoint in sampled baseline; contributes to slower user-facing review pages under load.

**Screenshot Location:** `docs/screenshots/baseline/blocking-method-1.png`

#### Method 2: `AuthService.login(...)` (via `POST /api/v1/auth/login`)
- **Execution Time:** ~134.45 ms avg (valid login), ~116.18 ms avg (invalid login)
- **Blocking Reason:** User lookup + password hash verification/token generation in authentication path.
- **Impact:** Moderate request latency on auth endpoints; can add noticeable overhead under repeated login traffic.

**Screenshot Location:** `docs/screenshots/baseline/blocking-method-2.png`

---

## 6. Thread Contention Analysis

### 6.1 Thread Dump Analysis
```
Blocked Threads: 0
Waiting Threads: 16
Runnable Threads: 12
Timed Waiting Threads: 16
```

**Screenshot Location:** `docs/screenshots/baseline/thread-dump.png`

### 6.2 Contention Points Identified
- **Location:** No hard contention hotspot observed in sampled thread dump.
- **Type:** No `BLOCKED` threads detected during capture window.
- **Wait Time:** Not significant in sampled run (threads are mostly `WAITING` / `TIMED_WAITING`, not lock-blocked).

**Screenshot Location:** `docs/screenshots/baseline/thread-contention.png`

---

## 7. Identified Bottlenecks Summary

### 7.1 Critical Bottlenecks (High Priority)
1. **Review endpoint latency (`ReviewService.listReviews`)**
   - **Type:** Service/DB read path
   - **Impact:** ~200-255 ms latency band in baseline sample; significantly slower than product endpoints.
   - **Recommendation:** Optimize review query/index strategy and validate serialization/mapping overhead under load.

2. **Rate limiting interference during concurrent profiling**
   - **Type:** Service/policy
   - **Impact:** `429` responses appear in burst tests and can skew measured throughput/error-rate values.
   - **Recommendation:** Use a dedicated perf profile (or endpoint-specific limiter policy) for cleaner benchmark windows.

### 7.2 Moderate Bottlenecks (Medium Priority)
1. **Authentication path latency (`AuthService.login`)**
   - **Type:** Service/security
   - **Impact:** ~116-134 ms average latency in sampled run.
   - **Recommendation:** Monitor under sustained load; tune auth-related DB access and hashing settings only if proven bottleneck.

### 7.3 Minor Bottlenecks (Low Priority)
1. **Thread contention**
   - **Type:** Thread/locking
   - **Impact:** No meaningful lock contention detected in sampled dump (`BLOCKED=0`).
   - **Recommendation:** Re-check during heavier mixed read/write load, but currently low priority.

---

## 8. How to Capture Screenshots

### 8.1 JVM Metrics (Actuator)
1. Start the application: `mvn spring-boot:run`
2. Open browser and navigate to: `http://localhost:8080/actuator/metrics`
3. Take screenshot of the metrics list
4. For specific metrics, navigate to:
   - Memory: `http://localhost:8080/actuator/metrics/jvm.memory.used`
   - Threads: `http://localhost:8080/actuator/metrics/jvm.threads.live`
   - CPU: `http://localhost:8080/actuator/metrics/system.cpu.usage`
   - GC: `http://localhost:8080/actuator/metrics/jvm.gc.pause`
5. Save screenshots to `docs/screenshots/baseline/`

### 8.2 Thread Dump
1. Navigate to: `http://localhost:8080/actuator/threaddump`
2. Take screenshot showing thread states
3. Look for BLOCKED or WAITING threads
4. Save to `docs/screenshots/baseline/thread-dump.png`

### 8.3 Heap Dump (Optional - Large File)
1. Navigate to: `http://localhost:8080/actuator/heapdump`
2. This downloads a `.hprof` file
3. Analyze with VisualVM or Eclipse MAT
4. Take screenshot of memory analysis
5. Save to `docs/screenshots/baseline/heap-analysis.png`

### 8.4 Hibernate SQL Logs
1. Run the application with SQL logging enabled
2. Execute API requests (e.g., GET `/api/v1/products`)
3. Check console output for SQL statements
4. Take screenshot of:
   - SQL queries with execution time
   - N+1 query patterns
   - Slow queries (> 100ms)
5. Save to `docs/screenshots/baseline/sql-logs.png`

### 8.5 API Response Times (Postman)
1. Open Postman
2. Send requests to API endpoints
3. Check the response time in bottom-right corner
4. Take screenshot showing:
   - Request URL
   - Response time
   - Response status
5. Save to `docs/screenshots/baseline/api-response-times-normal.png`

### 8.6 Load Testing (JMeter)
1. Install Apache JMeter: https://jmeter.apache.org/download_jmeter.cgi
2. Create a Thread Group:
   - Number of Threads: 50
   - Ramp-up Period: 10 seconds
   - Loop Count: 100
3. Add HTTP Request samplers for each endpoint
4. Add Listeners:
   - Summary Report
   - View Results Tree
   - Aggregate Report
5. Run the test
6. Take screenshots of:
   - Summary Report (throughput, avg response time)
   - Aggregate Report (90th/95th percentile)
   - View Results Tree (sample requests)
7. Save to `docs/screenshots/baseline/jmeter-results.png`

### 8.7 Java Flight Recorder (JFR)
1. Start application with JFR:
   ```bash
   java -XX:StartFlightRecording=duration=60s,filename=baseline.jfr -jar target/ecom-0.0.1-SNAPSHOT.jar
   ```
2. Or use `jcmd` while app is running:
   ```bash
   jcmd <pid> JFR.start duration=60s filename=baseline.jfr
   ```
3. Open `baseline.jfr` with JDK Mission Control or VisualVM
4. Take screenshots of:
   - CPU usage over time
   - Memory allocation
   - Thread activity
   - Hot methods (methods consuming most CPU)
5. Save to `docs/screenshots/baseline/jfr-analysis.png`

---

## 9. Next Steps

After collecting baseline metrics:
1. Analyze all bottlenecks and prioritize by impact
2. Document optimization strategies for each bottleneck
3. Implement optimizations in subsequent features:
   - Feature 2: Async Programming
   - Feature 3: Concurrency & Thread Safety
   - Feature 4: Data & Algorithmic Optimization
4. Re-run profiling after optimizations
5. Compare baseline vs optimized metrics
6. Generate final performance report

---

## 10. Baseline vs Optimized Comparison (To be filled after optimization)

| Metric | Baseline | Optimized | Improvement |
|--------|----------|-----------|-------------|
| Avg API Response Time | [TO BE MEASURED] | - | - |
| Throughput (req/s) | [TO BE MEASURED] | - | - |
| CPU Usage (%) | [TO BE MEASURED] | - | - |
| Memory Usage (MB) | [TO BE MEASURED] | - | - |
| Thread Count | [TO BE MEASURED] | - | - |
| DB Query Time (ms) | [TO BE MEASURED] | - | - |

---

**Report Status:** 🟡 In Progress - Remaining write-path baseline blocked by zero inventory state  
**Last Updated:** 2026-03-01  
**Prepared By:** Performance Analysis Team
