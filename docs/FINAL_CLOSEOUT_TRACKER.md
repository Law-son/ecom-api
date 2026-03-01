# Final Closeout Tracker

This tracker maps each remaining checklist item to concrete actions, commands, evidence files, and done criteria.

## Current Branch/Repo State

- Target branch for closeout commits: `master`
- Performance baseline report: `docs/baseline_performance_report.md`
- Feature summaries:
  - `docs/FEATURE_2_SUMMARY.md`
  - `docs/FEATURE_3_SUMMARY.md`
  - `docs/FEATURE_4_SUMMARY.md`
  - `docs/FEATURE_5_SUMMARY.md`

---

## A) Unblock Write-Path Profiling (Critical)

**Status:** ✅ Completed (2026-03-01, dev seeded-stock run)

### Why

Order-write metrics and race/data-consistency checks are currently blocked because inventory is zero across products.

### Action

Use one of the following:

1) Admin API flow (preferred):
- Login as an ADMIN user
- Call `POST /api/v1/inventory/adjust` for active products

2) DB seed flow:
- Update `inventory.quantity` directly in PostgreSQL for test products

### Evidence

- Screenshot(s) or logs showing non-zero stock after seeding
- At least one successful `POST /api/v1/orders` response

### Done Criteria

- At least 3 products have stock > 20
- `POST /api/v1/orders` returns `200` in normal-load run

---

## B) Feature 1 Remaining (Baseline Profiling Completion)

**Status:** 🟡 In progress (write-path + runtime snapshots updated; consolidated comparison rows still pending)

### 1. Complete Normal/Concurrent Write Metrics

### Commands (examples)

```bash
mvn -Dmaven.test.skip=true spring-boot:run -Dspring-boot.run.profiles=dev
```

Then run:
- `POST /api/v1/orders` normal loop (10 requests)
- concurrent order run (25-50 users equivalent)

### Evidence Files

- `docs/baseline_performance_report.md` (filled fields)
- `docs/screenshots/baseline/api-response-times-normal.png`
- `docs/screenshots/baseline/api-response-times-concurrent.png`

### Done Criteria

- No `[TO BE MEASURED]` placeholders in Sections 3-7 for core scoped endpoints
- Write-path latency numbers present (avg + percentile)

### 2. Capture Remaining Runtime Details

### Commands

```bash
curl http://localhost:8080/actuator/metrics/jvm.gc.pause
curl http://localhost:8080/actuator/metrics/system.cpu.usage
curl http://localhost:8080/actuator/threaddump
```

### Evidence Files

- `docs/screenshots/baseline/gc-metrics.png`
- `docs/screenshots/baseline/cpu-usage.png`
- `docs/screenshots/baseline/thread-dump.png`

### Done Criteria

- JVM/GC/thread sections fully populated with measured values

---

## C) Feature 2 Remaining

**Status:** 🟡 In progress (race/data consistency completed; full JMeter run pending)

### 1. Full JMeter Run

### Action

Run planned JMeter scenario (50 users, 100 loops or agreed equivalent).

### Evidence Files

- `docs/screenshots/baseline/jmeter-results.png`
- Optional: raw JMeter report export

### Done Criteria

- Throughput, p90, p95, error rate captured and documented

### 2. Race/Data Consistency on Order Writes

### Action

With stock seeded:
- Run concurrent order creates against same product(s)
- Verify no negative inventory and no oversell

### Evidence

- Result summary in `docs/FEATURE_2_SUMMARY.md`
- Optional screenshot of inventory before/after

### Done Criteria

- Explicit statement: no race-induced inconsistencies observed

---

## D) Feature 3 Remaining

**Status:** ✅ Completed (thread-pool matrix captured and selected config documented)

### 1. Thread-Pool Tuning Matrix

### Action

Test 2-3 async executor configs (small/medium/large) by changing:
- `ASYNC_CORE_POOL_SIZE`
- `ASYNC_MAX_POOL_SIZE`
- `ASYNC_QUEUE_CAPACITY`

### Evidence

- Table in `docs/FEATURE_3_SUMMARY.md`:
  - config
  - CPU
  - memory
  - throughput
  - p95 latency

### Done Criteria

- One chosen config + rationale documented

---

## E) Feature 4 Remaining

**Status:** 🟡 In progress (baseline-vs-optimized deltas completed; EXPLAIN ANALYZE execution pending local DB CLI)

### 1. Strict Baseline vs Optimized Delta Table

### Action

Compute percentage improvements using same endpoint + load shape between baseline and optimized runs.

### Formula

```text
Improvement % = ((Baseline - Optimized) / Baseline) * 100
```

### Evidence Files

- `docs/FEATURE_4_SUMMARY.md` final table
- `docs/explain_analyze.sql` execution outputs/screenshots

### Done Criteria

- Before/after table complete with percentages
- At least one `EXPLAIN ANALYZE` sample per key query class

---

## F) Feature 5 Remaining

**Status:** ✅ Completed (consolidated comparison table and evidence references added)

### 1. Consolidated Metrics Report

### Action

Compile a single report section with:
- CPU comparison
- memory comparison
- throughput comparison
- latency comparison

### Evidence Files

- `docs/FEATURE_5_SUMMARY.md` (or new final report doc)
- Screenshots from actuator/JMeter/profiler

### Done Criteria

- Comparison table complete and references evidence artifacts

---

## G) Final Deliverables Closeout

**Status:** 🟡 In progress (`plan.md` reconciled; PR creation/review step pending workflow choice)

### Action

Mark final checklist in `plan.md` after all evidence is complete.

### Mandatory Final Artifacts

- Baseline + optimized comparison report
- Concurrency implementation documentation
- Algorithmic optimization documentation
- Metrics/reporting documentation
- Load/performance evidence set (screenshots + values)

### Done Criteria

- Every final checklist item in `plan.md` is either completed or explicitly marked as user-only with evidence links.

---

## Execution Order (Recommended)

1. Unblock stock for write-path tests (Section A)
2. Finish Feature 1 write-path baseline (Section B)
3. Run JMeter + race/data consistency checks (Section C)
4. Complete Feature 3 tuning matrix (Section D)
5. Finalize Feature 4 deltas + explain evidence (Section E)
6. Finalize Feature 5 consolidated report (Section F)
7. Close final checklist (Section G)
