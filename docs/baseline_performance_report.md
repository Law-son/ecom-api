# Baseline Performance Report (Before Optimization)

**Date:** [To be filled after profiling]  
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
Initial Heap Size: [TO BE MEASURED]
Max Heap Size: [TO BE MEASURED]
Used Heap: [TO BE MEASURED]
Committed Heap: [TO BE MEASURED]
```

**Screenshot Location:** `docs/screenshots/baseline/jvm-memory.png`

#### Thread Metrics
```
Active Threads: [TO BE MEASURED]
Peak Threads: [TO BE MEASURED]
Daemon Threads: [TO BE MEASURED]
Thread States Distribution: [TO BE MEASURED]
```

**Screenshot Location:** `docs/screenshots/baseline/jvm-threads.png`

#### CPU Usage
```
System CPU Usage: [TO BE MEASURED]
Process CPU Usage: [TO BE MEASURED]
CPU Load Average: [TO BE MEASURED]
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
| `/api/v1/products` | GET | [TO BE MEASURED] | - | - | - |
| `/api/v1/products/{id}` | GET | [TO BE MEASURED] | - | - | - |
| `/api/v1/orders` | GET | [TO BE MEASURED] | - | - | - |
| `/api/v1/orders` | POST | [TO BE MEASURED] | - | - | - |
| `/api/v1/users` | GET | [TO BE MEASURED] | - | - | - |
| `/api/v1/categories` | GET | [TO BE MEASURED] | - | - | - |
| `/api/v1/reviews` | GET | [TO BE MEASURED] | - | - | - |

**Screenshot Location:** `docs/screenshots/baseline/api-response-times-normal.png`

---

### 3.3 API Response Times (Concurrent Load)

Test using JMeter with 50 concurrent users, 100 requests each:

| Endpoint | Method | Avg Response Time | Throughput (req/s) | Error Rate | 90th Percentile | 95th Percentile |
|----------|--------|-------------------|-------------------|------------|-----------------|-----------------|
| `/api/v1/products` | GET | [TO BE MEASURED] | - | - | - | - |
| `/api/v1/orders` | POST | [TO BE MEASURED] | - | - | - | - |
| `/api/v1/users` | GET | [TO BE MEASURED] | - | - | - | - |

**Screenshot Location:** `docs/screenshots/baseline/api-response-times-concurrent.png`

---

## 4. Database Query Analysis

### 4.1 Slow Queries Identified

#### Query 1: [Query Description]
```sql
[SQL QUERY TO BE CAPTURED FROM LOGS]
```
- **Execution Time:** [TO BE MEASURED]
- **Rows Returned:** [TO BE MEASURED]
- **Issue:** [N+1 query / Missing index / Inefficient join / etc.]

**Screenshot Location:** `docs/screenshots/baseline/slow-query-1.png`

#### Query 2: [Query Description]
```sql
[SQL QUERY TO BE CAPTURED FROM LOGS]
```
- **Execution Time:** [TO BE MEASURED]
- **Rows Returned:** [TO BE MEASURED]
- **Issue:** [N+1 query / Missing index / Inefficient join / etc.]

**Screenshot Location:** `docs/screenshots/baseline/slow-query-2.png`

### 4.2 N+1 Query Problems
- **Location:** [Service/Repository class and method]
- **Description:** [What causes the N+1 problem]
- **Impact:** [Performance degradation details]

**Screenshot Location:** `docs/screenshots/baseline/n-plus-1-queries.png`

### 4.3 Hibernate Statistics
```
Total Queries Executed: [TO BE MEASURED]
Total Query Execution Time: [TO BE MEASURED]
Cache Hit Ratio: [TO BE MEASURED]
Second Level Cache Hits: [TO BE MEASURED]
Second Level Cache Misses: [TO BE MEASURED]
```

**Screenshot Location:** `docs/screenshots/baseline/hibernate-stats.png`

---

## 5. Service Layer Analysis

### 5.1 Blocking Methods Identified

#### Method 1: [Class.methodName()]
- **Execution Time:** [TO BE MEASURED]
- **Blocking Reason:** [Database call / External API / File I/O / etc.]
- **Impact:** [Thread blocking / Response delay / etc.]

**Screenshot Location:** `docs/screenshots/baseline/blocking-method-1.png`

#### Method 2: [Class.methodName()]
- **Execution Time:** [TO BE MEASURED]
- **Blocking Reason:** [Database call / External API / File I/O / etc.]
- **Impact:** [Thread blocking / Response delay / etc.]

**Screenshot Location:** `docs/screenshots/baseline/blocking-method-2.png`

---

## 6. Thread Contention Analysis

### 6.1 Thread Dump Analysis
```
Blocked Threads: [TO BE MEASURED]
Waiting Threads: [TO BE MEASURED]
Runnable Threads: [TO BE MEASURED]
```

**Screenshot Location:** `docs/screenshots/baseline/thread-dump.png`

### 6.2 Contention Points Identified
- **Location:** [Class and method]
- **Type:** [Synchronized block / Lock / etc.]
- **Wait Time:** [TO BE MEASURED]

**Screenshot Location:** `docs/screenshots/baseline/thread-contention.png`

---

## 7. Identified Bottlenecks Summary

### 7.1 Critical Bottlenecks (High Priority)
1. **[Bottleneck Name]**
   - **Type:** Database / Service / Thread / Memory
   - **Impact:** [Performance impact description]
   - **Recommendation:** [Optimization approach]

2. **[Bottleneck Name]**
   - **Type:** Database / Service / Thread / Memory
   - **Impact:** [Performance impact description]
   - **Recommendation:** [Optimization approach]

### 7.2 Moderate Bottlenecks (Medium Priority)
1. **[Bottleneck Name]**
   - **Type:** Database / Service / Thread / Memory
   - **Impact:** [Performance impact description]
   - **Recommendation:** [Optimization approach]

### 7.3 Minor Bottlenecks (Low Priority)
1. **[Bottleneck Name]**
   - **Type:** Database / Service / Thread / Memory
   - **Impact:** [Performance impact description]
   - **Recommendation:** [Optimization approach]

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

**Report Status:** 🟡 In Progress - Awaiting Profiling Data  
**Last Updated:** [TO BE FILLED]  
**Prepared By:** Performance Analysis Team
