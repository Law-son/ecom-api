# Async Implementation Testing Guide

## Quick Verification Steps

### 1. Verify Async Configuration

**Start the application:**
```bash
mvn spring-boot:run
```

**Check logs for async initialization:**
```
Look for: "ThreadPoolTaskExecutor" initialization
Thread name prefix: "async-"
```

---

### 2. Test Async Thread Pool Metrics

**Access Actuator metrics:**
```bash
# List all metrics
curl http://localhost:8080/actuator/metrics

# Check executor metrics (if available)
curl http://localhost:8080/actuator/metrics/executor.active
curl http://localhost:8080/actuator/metrics/executor.completed
```

---

### 3. Test Parallel Stream Performance

#### Test Product Listing

**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/products?page=0&size=100"
```

**What to observe:**
- Response time should be faster with parallel streams
- Check console logs for SQL queries
- Verify all products are returned correctly

**Expected Improvement:**
- Before: ~50ms for 100 products
- After: ~25ms for 100 products

---

#### Test Review Listing

**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/reviews?page=0&size=50"
```

**What to observe:**
- Faster response time
- Correct review data
- Proper pagination

---

### 4. Test Async Order Processing

#### Create Order (Async Inventory Reservation)

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "items": [
      {"productId": 1, "quantity": 2},
      {"productId": 2, "quantity": 1}
    ]
  }'
```

**What to observe in logs:**
```
INFO AsyncOrderService - Inventory reservation completed for 2 items
INFO AsyncOrderService - Cache eviction completed for 2 items
```

**Expected Behavior:**
- Order created successfully
- Inventory reserved asynchronously
- Caches evicted asynchronously
- Faster response time

---

### 5. Load Testing with Postman

#### Setup Postman Collection Runner

1. Create a collection with these requests:
   - GET `/api/v1/products`
   - GET `/api/v1/reviews`
   - POST `/api/v1/orders` (with auth)

2. Configure Runner:
   - Iterations: 50
   - Delay: 100ms
   - Data file: (optional)

3. Run and observe:
   - Average response time
   - Success rate
   - Any errors

---

### 6. Load Testing with JMeter (Recommended)

#### JMeter Test Plan Setup

**1. Create Thread Group:**
```
Number of Threads: 50
Ramp-up Period: 10 seconds
Loop Count: 100
```

**2. Add HTTP Requests:**

**Request 1: Product List**
- Method: GET
- Path: `/api/v1/products`
- Parameters: `page=0&size=50`

**Request 2: Review List**
- Method: GET
- Path: `/api/v1/reviews`
- Parameters: `page=0&size=50`

**3. Add Listeners:**
- Summary Report
- Aggregate Report
- View Results Tree

**4. Run Test**

**Expected Results:**
- Throughput: > 500 req/s
- Average Response Time: < 200ms
- Error Rate: < 1%
- 90th Percentile: < 300ms

---

### 7. Monitor Thread Pool Usage

#### During Load Test

**Check thread metrics:**
```bash
# Active threads
curl http://localhost:8080/actuator/metrics/jvm.threads.live

# Peak threads
curl http://localhost:8080/actuator/metrics/jvm.threads.peak

# Thread states
curl http://localhost:8080/actuator/threaddump
```

**What to look for:**
- Thread count should increase under load
- Thread names should include "async-" prefix
- No BLOCKED threads
- Minimal WAITING threads

---

### 8. Verify No Race Conditions

#### Concurrent Order Creation Test

**Create multiple orders simultaneously:**

```bash
# Terminal 1
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"items": [{"productId": 1, "quantity": 5}]}'

# Terminal 2 (run immediately)
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"items": [{"productId": 1, "quantity": 5}]}'
```

**Verify:**
- Both orders succeed OR one fails with "Insufficient stock"
- Inventory is correctly decremented
- No negative inventory values
- No data inconsistencies

---

### 9. Verify Transaction Boundaries

#### Test Order Cancellation (Async Inventory Restoration)

**1. Create an order:**
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"items": [{"productId": 1, "quantity": 3}]}'
```

**2. Note the order ID from response**

**3. Cancel the order:**
```bash
curl -X PATCH http://localhost:8080/api/v1/orders/{orderId}/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"status": "CANCELLED"}'
```

**4. Verify:**
- Order status changed to CANCELLED
- Inventory restored (check product inventory)
- Logs show: "Inventory restoration completed"
- Transaction committed successfully

---

### 10. Performance Comparison

#### Baseline vs Optimized

**Capture metrics for comparison:**

| Metric | Baseline | Optimized | Improvement |
|--------|----------|-----------|-------------|
| Product List (100 items) | [TO MEASURE] | [TO MEASURE] | [%] |
| Review List (50 items) | [TO MEASURE] | [TO MEASURE] | [%] |
| Order Creation | [TO MEASURE] | [TO MEASURE] | [%] |
| Throughput (req/s) | [TO MEASURE] | [TO MEASURE] | [%] |
| CPU Usage (%) | [TO MEASURE] | [TO MEASURE] | [%] |
| Thread Count | [TO MEASURE] | [TO MEASURE] | [%] |

**How to measure:**
1. Run JMeter test with baseline code
2. Record metrics
3. Run JMeter test with optimized code
4. Record metrics
5. Calculate improvement percentage

---

### 11. Troubleshooting

#### Issue: Async methods not executing

**Check:**
```bash
# Verify AsyncConfig is loaded
curl http://localhost:8080/actuator/beans | grep AsyncConfig

# Check logs for async initialization
grep "ThreadPoolTaskExecutor" logs/application.log
```

#### Issue: Poor performance

**Check:**
```bash
# Thread pool exhaustion
curl http://localhost:8080/actuator/metrics/executor.queued

# CPU usage
curl http://localhost:8080/actuator/metrics/system.cpu.usage

# Memory usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

---

### 12. Success Criteria

✅ **Async Configuration:**
- Thread pool initialized with correct settings
- Async executor bean present

✅ **Parallel Streams:**
- Product listing faster by ~50%
- Review listing faster by ~50%

✅ **Async Processing:**
- Order creation faster by ~50%
- Inventory operations non-blocking
- Cache eviction asynchronous

✅ **Stability:**
- No race conditions
- No data inconsistencies
- Transactions work correctly

✅ **Monitoring:**
- Thread pool metrics available
- Async operations logged
- No thread exhaustion

---

## Quick Test Script

```bash
#!/bin/bash

echo "Testing Async Implementation..."

# 1. Test product listing
echo "1. Testing product listing..."
time curl -s http://localhost:8080/api/v1/products?page=0&size=100 > /dev/null

# 2. Test review listing
echo "2. Testing review listing..."
time curl -s http://localhost:8080/api/v1/reviews?page=0&size=50 > /dev/null

# 3. Check thread metrics
echo "3. Checking thread metrics..."
curl -s http://localhost:8080/actuator/metrics/jvm.threads.live

# 4. Check CPU usage
echo "4. Checking CPU usage..."
curl -s http://localhost:8080/actuator/metrics/system.cpu.usage

echo "Testing complete!"
```

Save as `test-async.sh` and run:
```bash
chmod +x test-async.sh
./test-async.sh
```

---

**For detailed implementation details, see:** `docs/ASYNC_IMPLEMENTATION.md`
