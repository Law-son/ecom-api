# Performance Report

## Scope
This report compares REST and GraphQL performance for key operations:
- Product listing with pagination/sorting/filtering
- Product details by ID
- Order creation
- Review creation and retrieval

## Methodology
- Environment: Local dev profile
- Database: PostgreSQL (primary) + MongoDB (reviews)
- Tools: Postman for REST, GraphiQL for GraphQL
- Metrics: Response time (ms), payload size (KB)
- Baseline: caching disabled (comment out cache annotations or set a no-op cache)
- Optimized: caching enabled + JPQL/natives with entity graphs
- Runbook: `docs/perf_run.md`

## Results (Fill With Local Measurements)
| Operation | Baseline REST Avg (ms) | Optimized REST Avg (ms) | Baseline GraphQL Avg (ms) | Optimized GraphQL Avg (ms) | Notes |
| --- | --- | --- | --- | --- | --- |
| List products | 66.40 | 9.46 | 115.63 | 22.50 | Pagination + search + sorting |
| Product by ID | 27.38 | 7.11 | 71.01 | 20.34 | Includes inventory lookup |
| Create order | 205.95 | 32.12 | 189.73 | 29.81 | Transactional inventory updates; optimized run used stock-safe iteration count |
| Add review | 672.89 | 479.49 | 636.95 | 490.54 | Mongo write |

## Index Validation Results (EXPLAIN ANALYZE)
Paste the most relevant query plan lines here, especially the index usage and
actual time/rows. Templates live in `docs/explain_analyze.sql`.
- Products by category:
  - TBD
- Products by name search:
  - TBD
- Categories by name:
  - TBD
- Orders by user:
  - TBD

## Observations
- Entity graphs reduce N+1 lookups on product/category and order/item data.
- Inventory projection query avoids loading full inventory entities for list views.
- Cache hits are most impactful on product listing and product detail endpoints.

## Recommendations
- Validate index usage with `EXPLAIN ANALYZE` on:
  - `products(category_id)` and `LOWER(name)`
  - `categories(LOWER(category_name))`
  - `orders(user_id)`
- Re-run measurements after index changes or query adjustments.
- Track pagination sizes (10/20/50) to show scaling impact.

## DSA Notes
- Pagination and sorting use database-side ordering for large datasets.
- Page size impacts response time roughly linearly; cache mitigates repeated reads.

