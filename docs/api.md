# Smart E-Commerce API

Base URL (dev): `http://localhost:8080`
REST API base path: `/api/v1`

## Security

Authentication uses JWT bearer tokens signed with HMAC SHA-256.

- Log in via `POST /api/v1/auth/login` to receive a JWT token.
- Send the token on protected endpoints as `Authorization: Bearer <token>`.
- Roles: `CUSTOMER`, `ADMIN`. **[UPDATED]**

### OAuth2 Login and Signup (Google)

OAuth2 authentication is supported via Google using Spring Security OAuth2 Client. **OAuth2 login automatically handles both login and signup** - new users are automatically created on their first Google login.

#### Key Features

- **Automatic Signup**: New users are automatically created when they log in with Google for the first time
- **Account Linking**: Uses email as the unique identifier, linking OAuth2 and email/password accounts
- **Seamless Integration**: Works perfectly with email/password authentication (see Account Linking section below)

#### Google Cloud Console Configuration

When setting up OAuth2 credentials in Google Cloud Console, configure:

**Authorized JavaScript Origins:**
- Development: `http://localhost:8080`
- Production: `https://your-production-domain.com`

**Authorized Redirect URIs:**
- Development: `http://localhost:8080/login/oauth2/code/google`
- Production: `https://your-production-domain.com/login/oauth2/code/google`

**Important:** The redirect URI path `/login/oauth2/code/google` is Spring Security's default OAuth2 callback endpoint and must match exactly.

#### OAuth2 Login/Signup Flow **[UPDATED]**

**Endpoint:** `GET /oauth2/authorization/google`

**Flow:**

1. **Start the login/signup flow:**
   - Navigate to or request: `GET /oauth2/authorization/google`
   - User is redirected to Google's consent screen

2. **Google redirects back:**
   - Google redirects to: `http://localhost:8080/login/oauth2/code/google`
   - Spring Security handles the OAuth2 callback

3. **User provisioning (automatic signup for new users):**
   - User profile (name + email) is fetched from Google
   - **If user exists by email**: Updates `fullName` and `role`, preserves existing password
   - **If user doesn't exist**: Creates new user account with:
     - Email from Google
     - Full name from Google
     - Role based on email allowlists (ADMIN/CUSTOMER)
     - NULL password (user can set password later if needed)
   - User is saved to the database (`users` table)

4. **Token issuance and frontend redirect:**
   - **On Success:**
     - Access token (JWT) is generated
     - Refresh token is generated and set as HttpOnly cookie
     - User is redirected to: `OAUTH2_REDIRECT_URI` (default: `http://localhost:5173/oauth2/redirect`)
     - Access token passed as query parameter: `?accessToken=...`
     - Refresh token stored in HttpOnly cookie (secure, not accessible via JavaScript)
   - **On Failure:**
     - User is redirected to: `OAUTH2_REDIRECT_URI` with error parameter
     - Error is passed as query parameter: `?error={errorMessage}`
   - Frontend should check for `accessToken` (success) or `error` (failure) in URL parameters

**Success Response (via redirect):**
```
URL: http://localhost:5173/oauth2/redirect?accessToken=<jwt_token>
Cookie: refreshToken=<refresh_token>; HttpOnly; Path=/; Max-Age=604800
```

**Failure Response (via redirect):**
```
http://localhost:5173/oauth2/redirect?error=<error_message>
```

**Common OAuth2 Errors:**
- `No email from OAuth2 provider` - Google account has no email address
- `OAuth2 authentication failed` - Generic authentication failure
- `User not found: {email}` - User provisioning failed

#### Account Linking with Email/Password Authentication

OAuth2 and email/password authentication work seamlessly together:

**Scenario 1: OAuth2 First (Signup), Then Email/Password**
- User signs up via OAuth2 → Account created
- User can later set a password via `PUT /api/v1/users/{id}` (while authenticated)
- After setting password, user can use both OAuth2 and email/password login

**Scenario 2: Email/Password First (Signup), Then OAuth2**
- User registers via `POST /api/v1/users` → Account created
- User can later log in via OAuth2 with the same email
- Account is automatically linked (same user record)
- User can use both authentication methods

**Important Notes:**
- Email is the unique identifier linking accounts
- One email = One user account
- Both authentication methods share the same user record
- See `docs/AUTHENTICATION_FLOW.md` for detailed scenarios

#### Environment Variables

Configure OAuth2 credentials via environment variables (in `.env` file):
- `GOOGLE_OAUTH_CLIENT_ID`: Your Google OAuth2 Client ID
- `GOOGLE_OAUTH_CLIENT_SECRET`: Your Google OAuth2 Client Secret
- `OAUTH2_REDIRECT_URI`: Frontend redirect URI (default: `http://localhost:5173/oauth2/redirect`)
- `OAUTH2_ADMIN_EMAILS`: Comma-separated admin emails **[UPDATED]**

### RBAC Verification (Postman) **[UPDATED]**

Use these Postman checks to verify role-based access once OAuth2 credentials are configured:

1. **Configure role allowlists (env vars or `application.properties`):**
   - Set `OAUTH2_ADMIN_EMAILS` to include an admin Google email to test ADMIN access

2. **Login via Google OAuth2:**
   - Open `GET /oauth2/authorization/google` in a browser and complete login
   - Your frontend redirect page should:
     - Check for `accessToken` query parameter (success) or `error` query parameter (failure)
     - Store access token in memory (not localStorage)
     - Refresh token is automatically stored in HttpOnly cookie

3. **Call an ADMIN-only endpoint (should succeed only for ADMIN):**
   - `POST /api/v1/products`
   - Header: `Authorization: Bearer <accessToken>`
   - Expect:
     - `200 OK` / `201 Created` for `ADMIN`
     - `403 Forbidden` for `CUSTOMER`

Tip: You can decode the JWT token to confirm `role` claim matches the expected role.

### CSRF Protection

**CSRF is DISABLED for stateless JWT APIs** (default configuration):
- JWT tokens are sent in `Authorization` headers (not cookies)
- API is stateless (no server-side sessions)
- Same-origin policy and CORS provide protection against CSRF attacks

**When CSRF should be enabled:**
- Stateful session-based authentication (cookies)
- HTML form submissions (`application/x-www-form-urlencoded`)
- Browser-based applications using session cookies

**CSRF Demonstration Endpoints:**
- `GET /api/v1/demo/csrf-token` - Retrieve CSRF token for form submissions
- `POST /api/v1/demo/form-submit` - Submit form with CSRF protection (requires CSRF token)
- See README.md for detailed explanation of CORS vs CSRF interaction and testing instructions

### JWT Token Details **[UPDATED]**

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

**User Identification:**
- User ID is automatically extracted from JWT token by the authentication filter
- Clients should NOT send userId in request body or as query parameter
- All authenticated endpoints automatically use the userId from the token
- This prevents users from impersonating other users

### Testing JWT Tokens in Postman **[UPDATED]**

1. **Login to get tokens:**
   ```
   POST http://localhost:8080/api/v1/auth/login
   Body (JSON):
   {
     "email": "user@example.com",
     "password": "password123"
   }
   ```
   Response will contain `accessToken` in the `data` field.
   Refresh token is automatically set as HttpOnly cookie (not visible in response body).
   - Use `accessToken` for API requests (short-lived, expires in 60 minutes)
   - Refresh token stored securely in HttpOnly cookie (long-lived, expires in 7 days)

2. **Refresh access token:**
   ```
   POST http://localhost:8080/api/v1/auth/refresh
   ```
   No request body needed - refresh token is automatically sent from cookie.
   Returns new `accessToken` in response. New refresh token is set as HttpOnly cookie.
   Old refresh token is revoked.

3. **Decode token to view claims:**
   - Copy the access token from the login response
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
   - Modify any character in the token
   - Make a request with tampered token
   - Verify `401 Unauthorized` response with "Invalid token signature" message

7. **Logout:**
   ```
   POST http://localhost:8080/api/v1/auth/logout
   Headers:
   Authorization: Bearer <access-token>
   ```
   Revokes all refresh tokens for the authenticated user and clears the refresh token cookie.

**Important Notes:**
- Refresh tokens are stored in HttpOnly cookies for security (not accessible via JavaScript)
- Access tokens are returned in response body and should be stored in memory (not localStorage)
- Cookies are automatically sent with requests to the same domain
- For cross-origin requests, ensure CORS is configured with `credentials: 'include'`

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

### Orders **[UPDATED]**
- `POST /api/v1/orders`
  - Body:
    - `items`: `[{ productId, quantity }]`
  - Note: userId is automatically extracted from JWT token
- `GET /api/v1/orders`
  - Query: `page`, `size`, `sortBy`, `sortDir`
  - Returns orders for authenticated user (CUSTOMER) or all orders (ADMIN)
  - Note: userId is automatically extracted from JWT token
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

## Demo Endpoints (CSRF Demonstration)

These endpoints demonstrate CSRF protection for form submissions.

### CSRF Token
- `GET /api/v1/demo/csrf-token`
  - Returns CSRF token for form submissions
  - Response: `{ status, message, data }` where `data` contains the CSRF token
  
### Form Submission
- `POST /api/v1/demo/form-submit`
  - Form submission endpoint with CSRF protection enabled
  - Headers: `X-CSRF-TOKEN: <csrf-token>` (required)
  - Body (form-urlencoded): `message=<optional-message>`
  - Response: `{ status, message, data }`
  - Without CSRF token: `403 Forbidden`
  - With valid CSRF token: `200 OK`

**Testing Instructions:**
1. First, GET `/api/v1/demo/csrf-token` to retrieve the token
2. Include token in POST request as header `X-CSRF-TOKEN: <token>`
3. Without the token, request will be rejected with `403 Forbidden`
4. See README.md for detailed CORS vs CSRF explanation

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


