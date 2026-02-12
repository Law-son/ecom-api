# Smart E-Commerce API

Base URL (dev): `http://localhost:8080`
REST API base path: `/api/v1`

## Security

Authentication uses JWT bearer tokens.

- Log in via `POST /api/v1/auth/login` to receive `accessToken`, `tokenType`, and `expiresAt`.
- Send the token on protected endpoints as `Authorization: Bearer <token>`.
- Roles: `CUSTOMER`, `ADMIN`.

Public endpoints:
- `POST /api/v1/auth/login`
- `POST /api/v1/users`
- `GET /api/v1/products/**`
- `GET /api/v1/categories/**`
- `GET /api/v1/reviews/**`
- Swagger/OpenAPI: `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`

Authenticated endpoints:
- `GET|POST|PATCH|DELETE /api/v1/cart/**`
- `POST /api/v1/orders`
- `GET /api/v1/orders/**`
- `POST /api/v1/reviews`
- `POST /graphql` (all GraphQL queries/mutations)

Admin-only endpoints:
- `GET|PUT|DELETE /api/v1/users/**` (user creation is public)
- `POST|PUT|DELETE /api/v1/products/**`
- `POST|PUT|DELETE /api/v1/categories/**`
- `GET|POST /api/v1/inventory/**`
- `PUT|PATCH /api/v1/orders/{id}/status`

## REST Endpoints

### Auth
- `POST /api/v1/auth/login`
  - Body:
    - `email` (string, required)
    - `password` (string, required)
  - Response: `{ status, message, data }`
  - `data` includes: `id`, `fullName`, `email`, `role`, `lastLogin`, `accessToken`, `tokenType`, `expiresAt`

### Users
- `POST /api/v1/users`
  - Body: `fullName`, `email`, `password`, `role`
  - Note: `role` is only honored for authenticated admins; otherwise defaults to `CUSTOMER`.
- `GET /api/v1/users`
- `GET /api/v1/users/{id}`
- `PUT /api/v1/users/{id}`
  - Body: any of `fullName`, `email`, `password`, `role`
- `DELETE /api/v1/users/{id}`

### Categories
- `POST /api/v1/categories`
  - Body: `name`
- `GET /api/v1/categories`
- `GET /api/v1/categories/{id}`
- `PUT /api/v1/categories/{id}`
  - Body: `name`
- `DELETE /api/v1/categories/{id}`

### Products
- `POST /api/v1/products`
  - Body: `categoryId`, `name`, `description`, `price`, `imageUrl`
- `GET /api/v1/products/all`
  - Returns all products without pagination
- `GET /api/v1/products`
  - Query: `categoryId`, `search`, `page`, `size`, `sortBy`, `sortDir`
- `GET /api/v1/products/{id}`
- `PUT /api/v1/products/{id}`
  - Body: `categoryId`, `name`, `description`, `price`, `imageUrl`
- `DELETE /api/v1/products/{id}`

Product response fields include:
- `stockQuantity`: available quantity (null if no inventory row).
- `inStock`: true when stockQuantity > 0.
- `stockStatus`: display string for UI—use as-is for labels/badges. One of:
  - `"Out of stock"` (0)
  - `"1 unit in stock"` (1)
  - `"N units in stock"` (2–10, e.g. `"5 units in stock"`)
  - `"Few units in stock"` (11–15)
  - `"In stock"` (16+)

### Cart
- `GET /api/v1/cart`
  - Returns the authenticated user's cart and totals.
- `POST /api/v1/cart/items`
  - Body: `productId`, `quantity`
- `PATCH /api/v1/cart/items/{productId}`
  - Body: `quantity`
- `DELETE /api/v1/cart/items/{productId}`
- `DELETE /api/v1/cart`
  - Clears all items from the cart.

### Inventory
- `POST /api/v1/inventory/adjust`
  - Body: `productId`, `quantity`
- `GET /api/v1/inventory/{productId}`

Inventory response fields include:
- `productId`, `quantity`, `lastUpdated`
- `stockStatus`: same display string as products (see Products above). Use for admin/stock UIs.

### Orders
- `POST /api/v1/orders`
  - Body:
    - `userId`
    - `items`: `[{ productId, quantity }]`
- `GET /api/v1/orders`
  - Query: `userId`, `page`, `size`, `sortBy`, `sortDir`
- `GET /api/v1/orders/{id}`
- `PATCH /api/v1/orders/{id}/status`
  - Body: `status` (PENDING, RECEIVED, SHIPPED, DELIVERED, CANCELLED)
- `PUT /api/v1/orders/{id}/status`
  - Body: `status` (PENDING, RECEIVED, SHIPPED, DELIVERED, CANCELLED)

Order status rules:
- **Terminal statuses:** Once an order is **CANCELLED** (by admin) or **RECEIVED** (by customer), its status cannot be changed again. Any further status update returns 400.
- **Cancellation:** When an admin sets status to **CANCELLED**, the quantities of all items in that order are returned to inventory (stock goes up per item quantity). Product/inventory caches are evicted.

### Reviews (MongoDB)
- `POST /api/v1/reviews`
  - Body: `userId`, `productId`, `rating`, `comment`, `metadata`
- `GET /api/v1/reviews`
  - Query: `productId`, `userId`

### Paged Response Shape
Paged endpoints return:
- `items`: list of resources
- `page`: current page index
- `size`: page size
- `totalElements`: total rows
- `totalPages`: total pages
- `hasNext`: boolean
- `hasPrevious`: boolean

## GraphQL

Endpoint: `POST /graphql`

### Queries
- `productById(id: ID!)`
- `products(categoryId: ID, search: String, page: Int, size: Int, sortBy: String, sortDir: String)`
- `categories`
- `users`
- `ordersByUser(userId: ID!, page: Int, size: Int)`
- `reviewsByProduct(productId: ID!)`

### Mutations
- `createProduct(input: ProductInput!)`
- `createCategory(input: CategoryInput!)`
- `createOrder(input: OrderCreateInput!)`
- `addReview(input: ReviewCreateInput!)`

## Swagger / OpenAPI
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`


