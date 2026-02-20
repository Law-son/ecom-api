# Smart E-Commerce API

Base URL (dev): `http://localhost:8080`
REST API base path: `/api/v1`

## Security

Authentication uses JWT bearer tokens signed with HMAC SHA-256.

- Log in via `POST /api/v1/auth/login` to receive a JWT token.
- Send the token on protected endpoints as `Authorization: Bearer <token>`.
- Roles: `CUSTOMER`, `ADMIN`.

### JWT Token Details

**Token Claims:**
- `sub` (subject): User email address
- `iat` (issued at): Token creation timestamp
- `exp` (expiration): Token expiration timestamp
- `userId`: User ID
- `role`: User role (CUSTOMER, ADMIN)
- `fullName`: User's full name
- `lastLogin`: Last login timestamp

**Signature Algorithm:** HMAC SHA-256 (HS256)

**Token Validation:**
- Tokens are validated on each protected request
- Tampered tokens are rejected with `401 Unauthorized` and message "Invalid token signature"
- Expired tokens are rejected with `401 Unauthorized` and message "Token expired"
- Invalid token format returns `401 Unauthorized` with message "Invalid or expired token"

### Testing JWT Tokens in Postman

1. **Login to get tokens:**
   ```
   POST http://localhost:8080/api/v1/auth/login
   Body (JSON):
   {
     "email": "user@example.com",
     "password": "password123"
   }
   ```
   Response will contain both `accessToken` and `refreshToken` in the `data` field.
   - Use `accessToken` for API requests (short-lived, expires in 60 minutes)
   - Store `refreshToken` securely (long-lived, expires in 7 days)

2. **Refresh access token:**
   ```
   POST http://localhost:8080/api/v1/auth/refresh
   Body (JSON):
   {
     "refreshToken": "<your-refresh-token>"
   }
   ```
   Returns new `accessToken` and `refreshToken`. Old refresh token is revoked.

3. **Decode token to view claims:**
   - Copy the token from the login response
   - Use Postman's built-in JWT decoder:
     - Go to Authorization tab → Type: Bearer Token
     - Paste token → Click "Preview" to see decoded claims
   - Or use jwt.io:
     - Paste token in the "Encoded" section
     - View decoded payload (claims) in the "Decoded" section
     - Note: Signature verification requires the secret key

4. **Use access token in protected requests:**
   ```
   Authorization: Bearer <your-token-here>
   ```

5. **Test token expiration:**
   - Wait for token to expire (default: 60 minutes)
   - Make a request with expired token
   - Verify `401 Unauthorized` response with "Token expired" message

6. **Test tampered token:**

7. **Logout:**
   ```
   POST http://localhost:8080/api/v1/auth/logout
   Headers:
   Authorization: Bearer <access-token>
   ```
   Revokes all refresh tokens for the authenticated user.
   - Modify any character in the token
   - Make a request with tampered token
   - Verify `401 Unauthorized` response with "Invalid token signature" message

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
  - `data` is an `AuthResponse` object containing:
    - `accessToken` (string): Short-lived JWT token (default: 60 minutes)
    - `refreshToken` (string): Long-lived refresh token (default: 7 days)
    - `tokenType` (string): Always "Bearer"

- `POST /api/v1/auth/refresh`
  - Body:
    - `refreshToken` (string, required)
  - Response: `{ status, message, data }`
  - `data` is an `AuthResponse` object with new `accessToken` and `refreshToken`
  - Note: Refresh token is rotated (old one is revoked, new one is issued)

- `POST /api/v1/auth/logout`
  - Requires authentication (Bearer token)
  - Response: `{ status, message, data }`
  - Revokes all refresh tokens for the authenticated user

### Users
- `POST /api/v1/users`
  - Body: `fullName`, `email`, `password`, `role`
  - Note: `role` is only honored for authenticated admins; otherwise defaults to `CUSTOMER`.
- `GET /api/v1/users`
-  - Query: `page`, `size`, `sortBy`, `sortDir`
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
  - Returns `409 Conflict` if the category has linked products.

### Products
- `POST /api/v1/products`
  - Body: `categoryId`, `name`, `description`, `price`, `imageUrl`
- `GET /api/v1/products/all`
  - Query: `page`, `size`, `sortBy`, `sortDir`
- `GET /api/v1/products`
  - Query: `categoryId`, `search`, `page`, `size`, `sortBy`, `sortDir`
- `GET /api/v1/products/{id}`
- `PUT /api/v1/products/{id}`
  - Body: `categoryId`, `name`, `description`, `price`, `imageUrl`
- `DELETE /api/v1/products/{id}`
  - Returns `409 Conflict` if the product is linked to carts, orders, or inventory.

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
  - Query: `productId`, `userId`, `page`, `size`, `sortBy`, `sortDir`

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


