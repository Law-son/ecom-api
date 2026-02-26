# Performance Profiling Screenshots

This directory contains screenshots captured during performance profiling and analysis.

## Directory Structure

```
screenshots/
├── baseline/           # Baseline performance metrics (before optimization)
│   ├── actuator-endpoints.png
│   ├── jvm-memory.png
│   ├── jvm-threads.png
│   ├── cpu-usage.png
│   ├── gc-metrics.png
│   ├── api-products.png
│   ├── api-orders.png
│   ├── api-users.png
│   ├── api-categories.png
│   ├── sql-logs.png
│   ├── slow-queries.png
│   ├── hibernate-stats.png
│   ├── thread-dump-analysis.png
│   ├── jmeter-summary.png
│   ├── jmeter-aggregate.png
│   ├── jmeter-results-tree.png
│   ├── jfr-hot-methods.png
│   ├── jfr-memory.png
│   └── jfr-threads.png
└── optimized/          # Performance metrics after optimization (to be added later)
```

## Screenshot Guidelines

### 1. File Naming Convention
- Use lowercase with hyphens: `jvm-memory.png`
- Be descriptive: `api-products-response-time.png`
- Include context: `baseline-` or `optimized-` prefix

### 2. Screenshot Quality
- Use PNG format for clarity
- Capture full browser window or relevant section
- Ensure text is readable
- Include timestamps when available

### 3. What to Capture

#### Actuator Metrics
- Full JSON response showing metric values
- Include the URL in the screenshot
- Capture multiple related metrics together

#### API Response Times
- Show the request URL
- Show the response time (Postman: bottom-right corner)
- Show the response status code
- Include request method (GET, POST, etc.)

#### SQL Logs
- Capture multiple queries to show patterns
- Include execution times
- Highlight slow queries (> 100ms)
- Show N+1 query patterns if present

#### JMeter Results
- Capture Summary Report with throughput
- Capture Aggregate Report with percentiles
- Capture View Results Tree with sample requests
- Include test configuration (threads, loops)

#### JFR Analysis
- Capture hot methods table (top 10)
- Capture memory allocation graph
- Capture thread activity timeline
- Capture GC events

### 4. Annotations
- Use arrows or highlights to point out important data
- Add text annotations for clarity
- Circle or highlight bottlenecks

### 5. Tools for Screenshots
- **Windows:** Snipping Tool, Snip & Sketch (Win + Shift + S)
- **Mac:** Command + Shift + 4
- **Linux:** Flameshot, GNOME Screenshot
- **Browser:** Built-in screenshot tools (F12 → ... → Capture screenshot)

## How to Take Screenshots

### Browser Screenshots (Actuator Endpoints)
1. Open the URL in your browser
2. Press `F12` to open Developer Tools
3. Click the three dots (⋮) → More tools → Capture screenshot
4. Or use Windows Snipping Tool (Win + Shift + S)

### Postman Screenshots
1. Send the request
2. Wait for response
3. Note the response time in bottom-right corner
4. Use Snipping Tool to capture the entire Postman window

### Console/Terminal Screenshots
1. Ensure the relevant logs are visible
2. Use Snipping Tool to capture the console window
3. Make sure SQL queries and execution times are visible

### JMeter Screenshots
1. Run the test completely
2. Click on each listener (Summary Report, Aggregate Report, etc.)
3. Use Snipping Tool to capture each report
4. Ensure all columns are visible

## Checklist

Before considering profiling complete, ensure you have:

- [ ] All Actuator metrics screenshots
- [ ] All API endpoint response time screenshots
- [ ] SQL logs showing queries and execution times
- [ ] Thread dump analysis screenshot
- [ ] JMeter test results (if performed)
- [ ] JFR analysis screenshots (if performed)
- [ ] All screenshots are clear and readable
- [ ] All screenshots are properly named
- [ ] All screenshots are referenced in `baseline_performance_report.md`

## Notes

- Screenshots are for documentation and analysis purposes
- They will be used to compare baseline vs optimized performance
- Keep original screenshots; do not edit or crop excessively
- If a screenshot is unclear, retake it

---

**For detailed profiling instructions, see:** `docs/PROFILING_INSTRUCTIONS.md`
