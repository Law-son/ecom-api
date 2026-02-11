# Performance Measurement Runbook

This runbook documents how to collect local metrics for pagination, query
optimization, and caching.

## Baseline vs Optimized Runs
1) Baseline (caching disabled):
   - Set `app.cache.enabled=false` in `application-dev.properties` or export
     `APP_CACHE_ENABLED=false`.
   - Start the app: `mvn spring-boot:run`.
2) Optimized (caching enabled):
   - Set `app.cache.enabled=true` (default).
   - Restart the app before capturing metrics.

## REST Requests To Measure
- List products (pagination + sorting):
  - `GET /api/products?page=0&size=20&sortBy=price&sortDir=asc`
  - `GET /api/products?page=1&size=20&sortBy=avgRating&sortDir=desc`
- Product by ID:
  - `GET /api/products/{id}`
- Create order (transactional):
  - `POST /api/orders`
- Add review:
  - `POST /api/reviews`

Capture response time averages in Postman or a CLI tool and record in
`docs/performance_report.md`.

## GraphQL Requests To Measure
```graphql
query ProductsPage {
  products(page: 0, size: 20, sortBy: "price", sortDir: "asc") {
    id
    name
    price
  }
}
```

```graphql
query ProductById {
  product(id: 1) {
    id
    name
    price
  }
}
```

## Index Validation (PostgreSQL)
Run the statements in `docs/explain_analyze.sql` and paste the query plans into
the report.

