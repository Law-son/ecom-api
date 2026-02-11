# Smart E-Commerce System Lab 6 Plan

Ordered by precedence. List includes all required items, even if already done.

## Spring Data Integration
- [x] Add and configure Spring Data JPA dependency.
- [x] Annotate all domain entities with `@Entity`, `@Id`, and relationship mappings.
- [x] Ensure repositories extend `JpaRepository` or `CrudRepository`.
- [x] Connect application to existing Module 4 database (MySQL or PostgreSQL).
- [x] Confirm layered architecture flow (Controller -> Service -> Repository).
- [x] Ensure Java 21 + Spring Boot 3.x stack (JPA, Cache, Validation, AOP).

## Repository and Query Development
- [x] Create repository interfaces for `User`, `Product`, `Category`, `Order`, `OrderItem`, and `Review`.
- [x] Implement derived queries (e.g., `findByCategoryName`, `findByPriceBetween`).
- [x] Implement custom JPQL queries for complex e-commerce operations.
- [x] Implement native SQL queries where required.

## Pagination and Sorting
- [x] Implement pagination and sorting using `Pageable`.
- [x] Update product listing APIs to return paginated responses.
- [x] Update order listing APIs to return paginated responses.
- [x] Drafted performance report template with pagination notes in `docs/performance_report.md`.
- [x] Added runbook and placeholders for sorting/pagination metrics in `docs/perf_run.md`.

## Transaction Management
- [x] Apply `@Transactional` to order creation and inventory update workflows.
- [x] Demonstrate correct propagation and isolation levels.
- [x] Verify rollback behavior for failure scenarios (e.g., insufficient stock).

## Query Optimization and Index Validation
- [x] Optimize complex JPQL queries for order history and reporting.
- [x] Documented index validation targets in `docs/performance_report.md`.
- [x] Added EXPLAIN ANALYZE templates in `docs/explain_analyze.sql`.
- [x] Added sections to capture pre/post optimization timings in `docs/performance_report.md`.

## Caching and Performance Enhancements
- [x] Enable caching with `@EnableCaching`.
- [x] Cache products, categories, and user profiles using Spring Cache.
- [x] Evict caches correctly after create/update/delete operations.
- [x] Added caching baseline vs optimized methodology in `docs/performance_report.md`.
- [x] Added caching toggle and measurement steps in `docs/perf_run.md`.

## Documentation and Reporting
- [x] Document repository structure and query logic.
- [x] Document transaction handling and rollback strategies.
- [x] Update `README` with caching configuration and testing steps.
- [x] Extend OpenAPI docs for repository-backed endpoints.
- [x] Created performance report template in `docs/performance_report.md`.
- [x] Added performance report runbook and index validation templates.
- [x] Confirm API testing via Postman, GraphQL Playground, or JavaFX.
- [x] Note DSA-related sorting/pagination efficiency impact in performance notes.

