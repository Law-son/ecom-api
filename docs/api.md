# Smart E-Commerce API

Base URL (dev): `http://localhost:8080`

## Security

Authentication uses JWT bearer tokens.

- Log in via `POST /api/auth/login` to receive `accessToken`, `tokenType`, and `expiresAt`.
- Send the token on protected endpoints as `Authorization: Bearer <token>`.
- Roles: `CUSTOMER`, `ADMIN`.

Public endpoints:
- `POST /api/auth/login`
- `POST /api/users`
- `GET /api/products/**`
- `GET /api/categories/**`
- `GET /api/reviews/**`
- Swagger/OpenAPI: `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`

Authenticated endpoints:
- `GET|POST|PATCH|DELETE /api/cart/**`
- `POST /api/orders`
- `GET /api/orders/**`
- `POST /api/reviews`
- `POST /graphql` (all GraphQL queries/mutations)

Admin-only endpoints:
- `GET|PUT|DELETE /api/users/**` (user creation is public)
- `POST|PUT|DELETE /api/products/**`
- `POST|PUT|DELETE /api/categories/**`
- `GET|POST /api/inventory/**`
- `PUT|PATCH /api/orders/{id}/status`

## REST Endpoints

### Auth
- `POST /api/auth/login`
  - Body:
    - `email` (string, required)
    - `password` (string, required)
  - Response: `{ status, message, data }`
  - `data` includes: `id`, `fullName`, `email`, `role`, `lastLogin`, `accessToken`, `tokenType`, `expiresAt`

### Users
- `POST /api/users`
  - Body: `fullName`, `email`, `password`, `role`
  - Note: `role` is only honored for authenticated admins; otherwise defaults to `CUSTOMER`.
- `GET /api/users`
- `GET /api/users/{id}`
- `PUT /api/users/{id}`
  - Body: any of `fullName`, `email`, `password`, `role`
- `DELETE /api/users/{id}`

### Categories
- `POST /api/categories`
  - Body: `name`
- `GET /api/categories`
- `GET /api/categories/{id}`
- `PUT /api/categories/{id}`
  - Body: `name`
- `DELETE /api/categories/{id}`

### Products
- `POST /api/products`
  - Body: `categoryId`, `name`, `description`, `price`, `imageUrl`
- `GET /api/products/all`
  - Returns all products without pagination
- `GET /api/products`
  - Query: `categoryId`, `search`, `page`, `size`, `sortBy`, `sortDir`
- `GET /api/products/{id}`
- `PUT /api/products/{id}`
  - Body: `categoryId`, `name`, `description`, `price`, `imageUrl`
- `DELETE /api/products/{id}`

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
- `GET /api/cart`
  - Returns the authenticated user's cart and totals.
- `POST /api/cart/items`
  - Body: `productId`, `quantity`
- `PATCH /api/cart/items/{productId}`
  - Body: `quantity`
- `DELETE /api/cart/items/{productId}`
- `DELETE /api/cart`
  - Clears all items from the cart.

### Inventory
- `POST /api/inventory/adjust`
  - Body: `productId`, `quantity`
- `GET /api/inventory/{productId}`

Inventory response fields include:
- `productId`, `quantity`, `lastUpdated`
- `stockStatus`: same display string as products (see Products above). Use for admin/stock UIs.

### Orders
- `POST /api/orders`
  - Body:
    - `userId`
    - `items`: `[{ productId, quantity }]`
- `GET /api/orders`
  - Query: `userId`, `page`, `size`, `sortBy`, `sortDir`
- `GET /api/orders/{id}`
- `PATCH /api/orders/{id}/status`
  - Body: `status` (PENDING, RECEIVED, SHIPPED, DELIVERED, CANCELLED)
- `PUT /api/orders/{id}/status`
  - Body: `status` (PENDING, RECEIVED, SHIPPED, DELIVERED, CANCELLED)

Order status rules:
- **Terminal statuses:** Once an order is **CANCELLED** (by admin) or **RECEIVED** (by customer), its status cannot be changed again. Any further status update returns 400.
- **Cancellation:** When an admin sets status to **CANCELLED**, the quantities of all items in that order are returned to inventory (stock goes up per item quantity). Product/inventory caches are evicted.

### Reviews (MongoDB)
- `POST /api/reviews`
  - Body: `userId`, `productId`, `rating`, `comment`, `metadata`
- `GET /api/reviews`
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


