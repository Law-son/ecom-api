# Smart E-Commerce API

Base URL (dev): `http://localhost:8080`

## REST Endpoints

### Auth
- `POST /api/auth/login`
  - Body:
    - `email` (string, required)
    - `password` (string, required)
  - Response: `{ status, message, data }`

### Users
- `POST /api/users`
  - Body: `fullName`, `email`, `password`, `role`
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
- `GET /api/products`
  - Query: `categoryId`, `search`, `page`, `size`, `sortBy`, `sortDir`
- `GET /api/products/{id}`
- `PUT /api/products/{id}`
  - Body: `categoryId`, `name`, `description`, `price`, `imageUrl`
- `DELETE /api/products/{id}`

### Inventory
- `POST /api/inventory/adjust`
  - Body: `productId`, `quantity`
- `GET /api/inventory/{productId}`

### Orders
- `POST /api/orders`
  - Body:
    - `userId`
    - `items`: `[{ productId, quantity }]`
- `GET /api/orders`
  - Query: `userId`, `page`, `size`, `sortBy`, `sortDir`
- `GET /api/orders/{id}`

### Reviews (MongoDB)
- `POST /api/reviews`
  - Body: `userId`, `productId`, `rating`, `comment`, `metadata`
- `GET /api/reviews`
  - Query: `productId`, `userId`

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


