# Client-Side Plan (React + Tailwind CSS)

## Overview
Build a modern, responsive web client for the Smart E-Commerce API using React and Tailwind CSS. The client will support:
- Customer shopping experience (browse, search, cart, order history).
- Admin dashboard (products, categories, inventory, orders, reviews).
- Authentication flows for both roles.
- Local API connectivity to `http://localhost:8080`.

## Tech Stack
- React (Vite)
- Tailwind CSS
- React Router
- Axios (or Fetch)
- React Query (optional, recommended for caching)
- Zustand (or Context) for session/cart state
- Zod + React Hook Form for validation

## Tasks (Complete in Order)

### 1) Project Setup
- STATUS: NOT_STARTED
- Create React app with Vite.
- Install Tailwind CSS and configure.
- Add routing, Axios, and state management libs.
- Configure environment variables (`VITE_API_BASE_URL=http://localhost:8080`).

### 2) App Shell + Layout
- STATUS: NOT_STARTED
- Build global layout (header, footer, sidebar).
- Implement responsive navigation.
- Add light theme with modern card-based UI.

### 3) Auth Flows
- STATUS: NOT_STARTED
- Create login + signup pages.
- Store session state (role + user).
- Add route guards for admin vs customer pages.

### 4) Customer Shopping UI
- STATUS: NOT_STARTED
- Catalog page with search, filters, sorting, pagination.
- Product details view.
- Cart state with quantity updates.

### 5) Checkout & Orders
- STATUS: NOT_STARTED
- Place order flow using `/api/orders`.
- Order history list and details for customer.

### 6) Reviews
- STATUS: NOT_STARTED
- Review submission and listing for products.
- Display avg rating and review counts.

### 7) Admin Dashboard
- STATUS: NOT_STARTED
- Admin landing dashboard with summaries.
- Manage categories (CRUD).
- Manage products (CRUD).
- Adjust inventory.
- View all orders and details.
- Review moderation view (list only).

### 8) API Integration Layer
- STATUS: NOT_STARTED
- Build API client module with typed requests.
- Add error handling, retry, loading states.
- Use React Query for cache + invalidation.

### 9) UI Polish
- STATUS: NOT_STARTED
- Tailwind styling refinements.
- Empty states, error states, loading skeletons.
- Accessibility checks for forms.

### 10) Final QA
- STATUS: NOT_STARTED
- End-to-end manual testing on all flows.
- Ensure admin and customer routes are locked correctly.

## Suggested Routes
- `/login`, `/signup`
- `/catalog`, `/product/:id`, `/cart`, `/orders`
- `/admin`, `/admin/products`, `/admin/categories`, `/admin/orders`, `/admin/inventory`, `/admin/reviews`

## Notes
- Authentication is currently username/password only. If token-based auth is added, use Axios interceptors for auth headers.
- Use Tailwind component patterns (cards, tables, badges).

