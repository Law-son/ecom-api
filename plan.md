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
- [ ] Test and document sorting/pagination performance (requires local metrics).

## Transaction Management
- [x] Apply `@Transactional` to order creation and inventory update workflows.
- [x] Demonstrate correct propagation and isolation levels.
- [x] Verify rollback behavior for failure scenarios (e.g., insufficient stock).

## Query Optimization and Index Validation
- [x] Optimize complex JPQL queries for order history and reporting.
- [ ] Validate index usage for frequently accessed columns (requires EXPLAIN ANALYZE).
- [ ] Record execution times before and after optimizations (requires local metrics).

## Caching and Performance Enhancements
- [x] Enable caching with `@EnableCaching`.
- [x] Cache products, categories, and user profiles using Spring Cache.
- [x] Evict caches correctly after create/update/delete operations.
- [ ] Measure and report performance improvements from caching (requires local metrics).

## Documentation and Reporting
- [x] Document repository structure and query logic.
- [x] Document transaction handling and rollback strategies.
- [x] Update `README` with caching configuration and testing steps.
- [x] Extend OpenAPI docs for repository-backed endpoints.
- [ ] Provide performance report comparing pre/post caching and query tuning (requires local metrics).
- [x] Confirm API testing via Postman, GraphQL Playground, or JavaFX.
- [x] Note DSA-related sorting/pagination efficiency impact in performance notes.

