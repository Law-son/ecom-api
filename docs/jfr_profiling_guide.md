# Java Flight Recorder (JFR) Profiling Guide

## Overview
Java Flight Recorder (JFR) is a built-in profiling tool in JDK 11+ that provides low-overhead performance monitoring and profiling capabilities.

---

## 1. Starting JFR with Application

### Method 1: Start with JFR Enabled
```bash
mvn clean package
java -XX:StartFlightRecording=duration=60s,filename=baseline.jfr -jar target/ecom-0.0.1-SNAPSHOT.jar
```

### Method 2: Start JFR on Running Application
```bash
# Find the Java process ID
jps -l

# Start JFR recording (replace <pid> with actual process ID)
jcmd <pid> JFR.start duration=60s filename=baseline.jfr

# Check recording status
jcmd <pid> JFR.check

# Stop recording manually
jcmd <pid> JFR.stop
```

### Method 3: Continuous Recording
```bash
java -XX:StartFlightRecording=settings=profile,filename=continuous.jfr -jar target/ecom-0.0.1-SNAPSHOT.jar
```

### Method 4: Runtime API (Implemented)

The app now exposes admin-only endpoints to start/stop JFR recordings at runtime.

```bash
# Requires ADMIN bearer token
TOKEN="<admin-access-token>"

# Start recording (60s profile settings)
curl -X POST "http://localhost:8080/api/v1/profiling/jfr/start?durationSeconds=60&settings=profile" \
  -H "Authorization: Bearer $TOKEN"

# Check status
curl "http://localhost:8080/api/v1/profiling/jfr/status" \
  -H "Authorization: Bearer $TOKEN"

# Stop and dump file (optional fileName)
curl -X POST "http://localhost:8080/api/v1/profiling/jfr/stop?fileName=api-load.jfr" \
  -H "Authorization: Bearer $TOKEN"
```

By default dumps are written to `target/profiling` (configurable with `app.profiling.jfr.output-dir`).

---

## 2. Recording Profiles

### Default Profile (Low Overhead)
```bash
-XX:StartFlightRecording=settings=default,duration=60s,filename=default.jfr
```

### Profile Mode (More Detailed)
```bash
-XX:StartFlightRecording=settings=profile,duration=60s,filename=profile.jfr
```

### Custom Settings
```bash
-XX:StartFlightRecording=duration=60s,filename=custom.jfr,settings=custom.jfc
```

---

## 3. Analyzing JFR Files

### Using JDK Mission Control (Recommended)
1. Download JDK Mission Control: https://www.oracle.com/java/technologies/jdk-mission-control.html
2. Open JMC
3. File → Open File → Select `.jfr` file
4. Analyze:
   - **Method Profiling** - Hot methods consuming CPU
   - **Memory** - Allocation patterns and GC activity
   - **Threads** - Thread activity and contention
   - **I/O** - File and network I/O operations
   - **Exceptions** - Exception frequency and types

### Using VisualVM
1. Install VisualVM: https://visualvm.github.io/
2. Install JFR plugin: Tools → Plugins → Available Plugins → Java Flight Recorder
3. File → Load → Select `.jfr` file
4. Analyze performance data

### Using Command Line
```bash
# Print summary
jfr print baseline.jfr

# Print specific events
jfr print --events jdk.CPULoad baseline.jfr
jfr print --events jdk.GarbageCollection baseline.jfr
jfr print --events jdk.JavaMonitorWait baseline.jfr

# Export to JSON
jfr print --json baseline.jfr > baseline.json
```

---

## 4. Key Metrics to Capture

### CPU Usage
- System CPU load
- Process CPU load
- Hot methods (methods consuming most CPU time)

### Memory
- Heap usage over time
- Object allocation rate
- GC frequency and duration
- Memory leaks

### Threads
- Thread count
- Thread states (RUNNABLE, BLOCKED, WAITING)
- Thread contention (synchronized blocks, locks)
- Deadlocks

### I/O Operations
- File I/O (read/write operations)
- Network I/O (socket operations)
- Database connections

### Garbage Collection
- GC events (Young Gen, Old Gen)
- GC pause times
- GC frequency
- Memory reclaimed

---

## 5. Profiling Workflow

### Step 1: Baseline Recording (Normal Load)
```bash
# Start application with JFR
mvn spring-boot:run &

# Wait for application to start
sleep 10

# Find process ID
PID=$(jps -l | grep ecom | awk '{print $1}')

# Start JFR recording
jcmd $PID JFR.start duration=60s filename=baseline-normal.jfr

# Execute normal API requests (Postman)
# - GET /api/v1/products
# - GET /api/v1/orders
# - POST /api/v1/orders
# - etc.

# Wait for recording to complete
sleep 60

echo "Baseline recording saved to baseline-normal.jfr"
```

### Step 2: Concurrent Load Recording
```bash
# Start JFR recording
jcmd $PID JFR.start duration=120s filename=baseline-concurrent.jfr

# Run JMeter load test (50 concurrent users)
# Let it run for 2 minutes

# Recording will stop automatically
echo "Concurrent load recording saved to baseline-concurrent.jfr"
```

### Step 3: Analyze Recordings
1. Open both `.jfr` files in JDK Mission Control
2. Compare normal vs concurrent load
3. Identify bottlenecks:
   - Methods with high CPU usage
   - Threads in BLOCKED/WAITING state
   - High GC activity
   - Slow database queries
4. Document findings in `baseline_performance_report.md`

---

## 6. Common Issues and Solutions

### Issue: JFR file not generated
**Solution:** Check if JFR is enabled in your JDK:
```bash
java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -version
```

### Issue: High overhead during recording
**Solution:** Use `settings=default` instead of `settings=profile`

### Issue: Cannot open JFR file
**Solution:** Ensure you're using JDK 11+ and JMC 8+

---

## 7. Screenshot Checklist

After profiling, capture screenshots of:

1. **CPU Usage Graph** - Shows CPU load over time
2. **Memory Usage Graph** - Shows heap usage and GC events
3. **Hot Methods Table** - Top 10 methods by CPU time
4. **Thread Activity** - Thread states and contention
5. **GC Statistics** - GC frequency and pause times
6. **Method Profiling Flame Graph** - Visual representation of call stacks

Save all screenshots to: `docs/screenshots/baseline/`

---

## 8. Integration with Maven

Add to `pom.xml` for automated profiling:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <jvmArguments>
                    -XX:StartFlightRecording=duration=60s,filename=target/baseline.jfr
                </jvmArguments>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Then run:
```bash
mvn spring-boot:run
```

---

## 9. Automated Profiling Script

Create `scripts/profile.sh`:

```bash
#!/bin/bash

echo "Starting application with JFR profiling..."

# Build application
mvn clean package -DskipTests

# Start application in background
java -XX:StartFlightRecording=duration=120s,filename=baseline.jfr -jar target/ecom-0.0.1-SNAPSHOT.jar &
APP_PID=$!

echo "Application started with PID: $APP_PID"
echo "JFR recording for 120 seconds..."

# Wait for application to start
sleep 15

# Run API tests
echo "Executing API requests..."
curl -s http://localhost:8080/api/v1/products > /dev/null
curl -s http://localhost:8080/api/v1/orders > /dev/null
curl -s http://localhost:8080/api/v1/users > /dev/null

# Wait for recording to complete
sleep 105

# Stop application
kill $APP_PID

echo "Profiling complete! JFR file: baseline.jfr"
echo "Open with: jmc baseline.jfr"
```

Make executable:
```bash
chmod +x scripts/profile.sh
./scripts/profile.sh
```

---

**Note:** JFR recordings are production-safe with minimal overhead (< 1% in default mode).
