# Quick Screenshot Guide for Performance Profiling

## 🚀 Getting Started

### Step 1: Start the Application
```bash
cd c:\Users\Bluewave\Desktop\ecom
mvn spring-boot:run
```

Wait until you see: `Started EcomApplication in X.XXX seconds`

---

## 📸 Screenshots to Capture

### 1. Actuator Endpoints List
**URL:** `http://localhost:8080/actuator`

**What to capture:** The JSON response showing all available endpoints

**Save as:** `docs/screenshots/baseline/actuator-endpoints.png`

---

### 2. JVM Memory Metrics
**URLs to visit:**
- `http://localhost:8080/actuator/metrics/jvm.memory.used`
- `http://localhost:8080/actuator/metrics/jvm.memory.max`

**What to capture:** Both JSON responses showing memory values

**Save as:** `docs/screenshots/baseline/jvm-memory.png`

---

### 3. Thread Metrics
**URLs to visit:**
- `http://localhost:8080/actuator/metrics/jvm.threads.live`
- `http://localhost:8080/actuator/metrics/jvm.threads.peak`
- `http://localhost:8080/actuator/threaddump`

**What to capture:** Thread counts and thread dump JSON

**Save as:** `docs/screenshots/baseline/jvm-threads.png`

---

### 4. CPU Metrics
**URLs to visit:**
- `http://localhost:8080/actuator/metrics/system.cpu.usage`
- `http://localhost:8080/actuator/metrics/process.cpu.usage`

**What to capture:** CPU usage percentages

**Save as:** `docs/screenshots/baseline/cpu-usage.png`

---

### 5. Garbage Collection Metrics
**URLs to visit:**
- `http://localhost:8080/actuator/metrics/jvm.gc.pause`
- `http://localhost:8080/actuator/metrics/jvm.gc.memory.allocated`

**What to capture:** GC pause times and memory allocation

**Save as:** `docs/screenshots/baseline/gc-metrics.png`

---

### 6. API Response Times (Postman)

Open Postman and test these endpoints:

#### Products
**Request:** `GET http://localhost:8080/api/v1/products`

**What to capture:** Response time shown in bottom-right corner of Postman

**Save as:** `docs/screenshots/baseline/api-products.png`

#### Orders
**Request:** `GET http://localhost:8080/api/v1/orders`

**Save as:** `docs/screenshots/baseline/api-orders.png`

#### Users
**Request:** `GET http://localhost:8080/api/v1/users`

**Save as:** `docs/screenshots/baseline/api-users.png`

#### Categories
**Request:** `GET http://localhost:8080/api/v1/categories`

**Save as:** `docs/screenshots/baseline/api-categories.png`

---

### 7. SQL Logs (Console)

**Where to look:** Check your console/terminal where the application is running

**What to capture:** 
- SQL queries with execution times
- Multiple queries (showing N+1 patterns if any)
- Any queries taking > 100ms

**Save as:** `docs/screenshots/baseline/sql-logs.png`

**Tip:** Use Windows Snipping Tool (Win + Shift + S) to capture the console window

---

## 🛠️ How to Take Screenshots

### Windows (Recommended)
1. Press `Win + Shift + S`
2. Select the area to capture
3. Screenshot is copied to clipboard
4. Open Paint or any image editor
5. Paste (Ctrl + V)
6. Save as PNG with the correct filename

### Alternative: Snipping Tool
1. Search for "Snipping Tool" in Start Menu
2. Click "New"
3. Select area
4. Save with correct filename

### Browser Screenshots
1. Open the URL
2. Press `Win + Shift + S`
3. Capture the browser window showing the JSON response
4. Save with correct filename

---

## ✅ Checklist

After capturing all screenshots, verify you have:

- [ ] `actuator-endpoints.png`
- [ ] `jvm-memory.png`
- [ ] `jvm-threads.png`
- [ ] `cpu-usage.png`
- [ ] `gc-metrics.png`
- [ ] `api-products.png`
- [ ] `api-orders.png`
- [ ] `api-users.png`
- [ ] `api-categories.png`
- [ ] `sql-logs.png`

All files should be in: `c:\Users\Bluewave\Desktop\ecom\docs\screenshots\baseline\`

---

## 📝 After Screenshots

1. Open `docs/baseline_performance_report.md`
2. Replace all `[TO BE MEASURED]` with actual values from your screenshots
3. Document any bottlenecks you notice
4. Save the file

---

## 🔄 Optional: Load Testing (Advanced)

If you want to do load testing with JMeter:

1. Download JMeter: https://jmeter.apache.org/download_jmeter.cgi
2. Follow instructions in `docs/PROFILING_INSTRUCTIONS.md` (Task 11-13)
3. Capture JMeter results screenshots

---

## 🆘 Need Help?

- Full instructions: `docs/PROFILING_INSTRUCTIONS.md`
- JFR guide: `docs/jfr_profiling_guide.md`
- Screenshot guidelines: `docs/screenshots/README.md`

---

**Estimated Time:** 15-20 minutes for basic profiling  
**Estimated Time with JMeter:** 30-40 minutes
