# Performance Profiling Instructions

## 📋 Overview
This guide provides step-by-step instructions to profile the application and collect baseline performance metrics.

---

## 🚀 Quick Start

### 1. Start the Application
```bash
mvn spring-boot:run
```

Wait for the application to start completely (look for "Started EcomApplication" in console).

---

## 📊 Profiling Tasks

### Task 1: Verify Actuator Endpoints

1. Open your browser and navigate to: `http://localhost:8080/actuator`
2. You should see a list of available endpoints
3. **Screenshot:** Take a screenshot and save as `docs/screenshots/baseline/actuator-endpoints.png`

### Task 2: Collect JVM Memory Metrics

1. Navigate to: `http://localhost:8080/actuator/metrics/jvm.memory.used`
2. Note the values for:
   - `jvm.memory.used` (heap and non-heap)
3. Navigate to: `http://localhost:8080/actuator/metrics/jvm.memory.max`
4. **Screenshot:** Take screenshots and save as `docs/screenshots/baseline/jvm-memory.png`

### Task 3: Collect Thread Metrics

1. Navigate to: `http://localhost:8080/actuator/metrics/jvm.threads.live`
2. Note the number of live threads
3. Navigate to: `http://localhost:8080/actuator/metrics/jvm.threads.peak`
4. Navigate to: `http://localhost:8080/actuator/threaddump`
5. **Screenshot:** Take screenshots and save as `docs/screenshots/baseline/jvm-threads.png`

### Task 4: Collect CPU Metrics

1. Navigate to: `http://localhost:8080/actuator/metrics/system.cpu.usage`
2. Note the CPU usage percentage
3. Navigate to: `http://localhost:8080/actuator/metrics/process.cpu.usage`
4. **Screenshot:** Take screenshots and save as `docs/screenshots/baseline/cpu-usage.png`

### Task 5: Collect Garbage Collection Metrics

1. Navigate to: `http://localhost:8080/actuator/metrics/jvm.gc.pause`
2. Note the GC pause times
3. Navigate to: `http://localhost:8080/actuator/metrics/jvm.gc.memory.allocated`
4. **Screenshot:** Take screenshots and save as `docs/screenshots/baseline/gc-metrics.png`

---

## 🔍 API Response Time Testing

### Task 6: Test API Endpoints (Normal Load)

Use Postman or curl to test the following endpoints:

#### Products Endpoint
```bash
curl -X GET http://localhost:8080/api/v1/products
```
**Screenshot:** Note the response time in Postman (bottom-right corner) and save as `docs/screenshots/baseline/api-products.png`

#### Orders Endpoint
```bash
curl -X GET http://localhost:8080/api/v1/orders
```
**Screenshot:** Save as `docs/screenshots/baseline/api-orders.png`

#### Users Endpoint
```bash
curl -X GET http://localhost:8080/api/v1/users
```
**Screenshot:** Save as `docs/screenshots/baseline/api-users.png`

#### Categories Endpoint
```bash
curl -X GET http://localhost:8080/api/v1/categories
```
**Screenshot:** Save as `docs/screenshots/baseline/api-categories.png`

---

## 🗄️ Database Query Analysis

### Task 7: Capture SQL Logs

1. Keep the application running with SQL logging enabled
2. Execute the API requests from Task 6
3. Check the console output for SQL statements
4. Look for:
   - Multiple queries for the same data (N+1 problem)
   - Queries taking > 100ms
   - Queries without proper indexes
5. **Screenshot:** Take screenshots of the console showing SQL logs and save as `docs/screenshots/baseline/sql-logs.png`

### Task 8: Identify Slow Queries

1. Review the SQL logs in the console
2. Identify queries with high execution time
3. Copy the slow queries to `docs/baseline_performance_report.md` (Section 4.1)
4. **Screenshot:** Highlight slow queries and save as `docs/screenshots/baseline/slow-queries.png`

### Task 9: Check Hibernate Statistics

1. Navigate to: `http://localhost:8080/actuator/metrics`
2. Search for Hibernate-related metrics (if available)
3. Check console logs for Hibernate statistics output
4. **Screenshot:** Save as `docs/screenshots/baseline/hibernate-stats.png`

---

## 🧵 Thread Analysis

### Task 10: Analyze Thread Dump

1. Navigate to: `http://localhost:8080/actuator/threaddump`
2. Look for threads in states:
   - `BLOCKED` - Thread is blocked waiting for a lock
   - `WAITING` - Thread is waiting indefinitely
   - `TIMED_WAITING` - Thread is waiting for a specified time
3. Identify any thread contention issues
4. **Screenshot:** Save as `docs/screenshots/baseline/thread-dump-analysis.png`

---

## 📈 Load Testing (Optional but Recommended)

### Task 11: Install Apache JMeter

1. Download JMeter from: https://jmeter.apache.org/download_jmeter.cgi
2. Extract to a folder (e.g., `C:\jmeter`)
3. Run JMeter: `bin\jmeter.bat` (Windows) or `bin/jmeter.sh` (Linux/Mac)

### Task 12: Create JMeter Test Plan

1. In JMeter, create a new Test Plan
2. Add Thread Group:
   - Right-click Test Plan → Add → Threads → Thread Group
   - Number of Threads: 50
   - Ramp-up Period: 10 seconds
   - Loop Count: 100
3. Add HTTP Request:
   - Right-click Thread Group → Add → Sampler → HTTP Request
   - Server Name: `localhost`
   - Port: `8080`
   - Path: `/api/v1/products`
4. Add Listeners:
   - Right-click Thread Group → Add → Listener → Summary Report
   - Right-click Thread Group → Add → Listener → View Results Tree
   - Right-click Thread Group → Add → Listener → Aggregate Report
5. Save the test plan as `jmeter-baseline-test.jmx`

### Task 13: Run Load Test

1. Click the green "Start" button in JMeter
2. Wait for the test to complete (about 2-3 minutes)
3. Review the results in:
   - **Summary Report** - Shows throughput and average response time
   - **Aggregate Report** - Shows 90th and 95th percentile
   - **View Results Tree** - Shows individual request details
4. **Screenshot:** Take screenshots of all three reports and save as:
   - `docs/screenshots/baseline/jmeter-summary.png`
   - `docs/screenshots/baseline/jmeter-aggregate.png`
   - `docs/screenshots/baseline/jmeter-results-tree.png`

---

## 🔥 Java Flight Recorder Profiling (Advanced)

### Task 14: Profile with JFR

#### Option 1: Start with JFR
```bash
# Build the application
mvn clean package -DskipTests

# Start with JFR recording
java -XX:StartFlightRecording=duration=60s,filename=baseline.jfr -jar target/ecom-0.0.1-SNAPSHOT.jar
```

#### Option 2: Attach JFR to Running Application
```bash
# Find the process ID
jps -l

# Start JFR recording (replace <pid> with actual PID)
jcmd <pid> JFR.start duration=60s filename=baseline.jfr

# Execute API requests during recording
curl http://localhost:8080/api/v1/products
curl http://localhost:8080/api/v1/orders
curl http://localhost:8080/api/v1/users

# Wait for recording to complete
```

### Task 15: Analyze JFR Recording

1. Download JDK Mission Control: https://www.oracle.com/java/technologies/jdk-mission-control.html
2. Open JMC and load `baseline.jfr`
3. Analyze:
   - **Method Profiling** - Hot methods tab
   - **Memory** - Memory tab
   - **Threads** - Threads tab
   - **I/O** - File and Socket I/O tabs
4. **Screenshot:** Take screenshots of key findings and save as:
   - `docs/screenshots/baseline/jfr-hot-methods.png`
   - `docs/screenshots/baseline/jfr-memory.png`
   - `docs/screenshots/baseline/jfr-threads.png`

---

## 📝 Document Findings

### Task 16: Update Baseline Report

1. Open `docs/baseline_performance_report.md`
2. Fill in all `[TO BE MEASURED]` placeholders with actual values
3. Add descriptions for identified bottlenecks
4. Document slow queries and N+1 problems
5. Add recommendations for optimization

### Task 17: Create Summary

Create a summary of findings:
- Top 3 critical bottlenecks
- Top 3 slow database queries
- Thread contention issues
- Memory usage concerns
- CPU usage patterns

---

## ✅ Checklist

After completing all tasks, verify you have:

- [ ] Actuator endpoints screenshot
- [ ] JVM memory metrics screenshot
- [ ] Thread metrics screenshot
- [ ] CPU usage screenshot
- [ ] GC metrics screenshot
- [ ] API response times screenshots (all endpoints)
- [ ] SQL logs screenshot
- [ ] Slow queries identified and documented
- [ ] Thread dump analysis screenshot
- [ ] JMeter test results (if performed)
- [ ] JFR analysis screenshots (if performed)
- [ ] Updated `baseline_performance_report.md` with all metrics
- [ ] Documented all identified bottlenecks

---

## 🎯 Expected Outcomes

After profiling, you should have:
1. Clear understanding of current performance baseline
2. Identified database query bottlenecks
3. Identified blocking service methods
4. Thread contention analysis
5. Memory and CPU usage patterns
6. Documented recommendations for optimization

---

## 🆘 Troubleshooting

### Application won't start
- Check if PostgreSQL and MongoDB are running
- Verify database connection settings in `.env` file
- Check if port 8080 is available

### Actuator endpoints return 404
- Verify `management.endpoints.web.exposure.include` in `application.properties`
- Restart the application

### No SQL logs in console
- Verify Hibernate logging is enabled in `application.properties`
- Check log level is set to DEBUG for `org.hibernate.SQL`

### JFR file not generated
- Ensure you're using JDK 11 or higher
- Check file permissions in the target directory
- Verify JFR is supported: `java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -version`

---

## 📞 Next Steps

After completing profiling:
1. Review all collected metrics
2. Prioritize bottlenecks by impact
3. Move to Feature 2: Asynchronous Programming Implementation
4. Implement optimizations
5. Re-run profiling to measure improvements

---

**Good luck with profiling! 🚀**
