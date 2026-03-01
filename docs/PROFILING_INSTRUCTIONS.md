# Profiling Instructions (Feature 1)

This guide completes the "Performance Bottleneck Analysis" checklist in `plan.md`.

## 1) Start the application with profiling-friendly settings

Use the dev profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 2) Verify required Actuator endpoints

Open these URLs in your browser for screenshots:

- `http://localhost:8080/actuator/health`
- `http://localhost:8080/actuator/metrics`
- `http://localhost:8080/actuator/threaddump`
- `http://localhost:8080/actuator/heapdump` (downloads a dump file)

## 3) Java Flight Recorder (JFR)

JFR is built into JDK 17+.

Find Java process id:

```bash
jcmd | rg ecom
```

Start recording:

```bash
jcmd <PID> JFR.start name=baseline settings=profile duration=120s filename=baseline.jfr
```

Start recording for concurrent load:

```bash
jcmd <PID> JFR.start name=concurrent settings=profile duration=180s filename=concurrent.jfr
```

After each run, inspect in Java Mission Control and capture screenshots of:

- Hottest methods (CPU)
- Blocking methods / lock instances
- Thread state distribution

## 4) Normal load profiling

Run a small request set (Postman collection or cURL calls):

- `GET /api/products?page=0&size=20`
- `GET /api/products/1`
- `POST /api/orders`

Collect:

- response times
- CPU usage (`/actuator/metrics/system.cpu.usage`)
- memory usage (`/actuator/metrics/jvm.memory.used`)
- live threads (`/actuator/metrics/jvm.threads.live`)

## 5) Concurrent load profiling

Use Postman runner or JMeter with 25-50 virtual users hitting product/order endpoints.

Collect:

- p50/p95 response times
- CPU/memory/thread metrics while load is running
- one JFR recording during the load window

## 6) Identify bottlenecks

- Slow SQL queries: capture from app logs and/or DB `EXPLAIN ANALYZE`
- Blocking service-layer methods: inspect JFR call tree + lock view
- Thread contention: inspect JFR locks/threads and `/actuator/threaddump`

## 7) Screenshot checklist

Capture and store screenshots for:

1. `/actuator/metrics` overview
2. `/actuator/metrics/system.cpu.usage`
3. `/actuator/metrics/jvm.memory.used`
4. `/actuator/metrics/jvm.threads.live`
5. `/actuator/threaddump`
6. JFR hotspots
7. JFR lock contention view
8. Slow query evidence (logs or explain plan)

## 8) Final documentation

Fill `docs/baseline_performance_report.md` with measured values and screenshot references.
