# Authentication Flow Documentation

This document explains how the dual authentication system works, covering both **Email/Password** and **OAuth2 (Google)** authentication methods.

## Overview

The application supports two authentication methods:
1. **Email/Password Authentication** - Traditional username/password login
2. **OAuth2 (Google) Authentication** - Social login via Google

Both methods create and authenticate users in the same `users` table, using **email as the unique identifier** to link accounts.

---

## Authentication Methods

### 1. Email/Password Authentication

#### Registration
- **Endpoint**: `POST /api/v1/users`
- **Process**:
  1. User provides: `email`, `password`, `fullName`
  2. System checks if email already exists
  3. If email exists → Returns `409 Conflict`
  4. If email is new → Creates user with BCrypt-hashed password
  5. Default role: `CUSTOMER` (unless admin creates with different role)

#### Login
- **Endpoint**: `POST /api/v1/auth/login`
- **Process**:
  1. User provides: `email`, `password`
  2. System finds user by email
  3. Validates password using BCrypt
  4. If valid → Issues JWT access token + refresh token
  5. If invalid → Returns `401 Unauthorized`

---

### 2. OAuth2 (Google) Authentication

#### Login Flow
- **Endpoint**: `GET /oauth2/authorization/google`
- **Process**:
  1. User is redirected to Google's consent screen
  2. User authorizes the application
  3. Google redirects back to: `http://localhost:8080/login/oauth2/code/google`
  4. System fetches user profile (email, name) from Google
  5. System checks if user exists by email:
     - **If user exists**: Updates `fullName` and `role`, **keeps existing password**
     - **If user doesn't exist**: Creates new user with:
       - Email from Google
       - Full name from Google
       - Role based on email allowlists (ADMIN/STAFF/CUSTOMER)
       - **Random UUID as password hash** (password not used for OAuth2)
  6. System issues JWT access token + refresh token
  7. User is redirected to frontend with tokens in query params

---

## Account Linking Behavior

Since both authentication methods use **email as the unique identifier**, here's what happens in different scenarios:

### Scenario 1: User Registers with Email/Password First

1. User registers: `POST /api/v1/users` with `email: "user@example.com"`
2. User account is created with password hash
3. **Later, user logs in via OAuth2 with same email:**
   - System finds existing user by email
   - Updates `fullName` from Google profile
   - **Preserves existing role** (never downgrades)
   - **Only upgrades role** if email is in OAuth2 allowlists (e.g., CUSTOMER → ADMIN/STAFF)
   - **Keeps existing password hash** (user can still login with password)
   - Issues JWT tokens
   - ✅ **Result**: User can use BOTH authentication methods, role is preserved

### Scenario 2: User Registers with OAuth2 First

1. User logs in via OAuth2: `GET /oauth2/authorization/google` with `email: "user@example.com"`
2. User account is created with:
   - Email from Google
   - Random UUID as password hash (not usable for password login)
3. **Later, user tries to register with email/password:**
   - `POST /api/v1/users` with same email
   - System checks if email exists → **Email already exists**
   - Returns `409 Conflict: "Email already exists"`
   - ❌ **Result**: Registration fails

4. **Later, user tries to login with email/password:**
   - `POST /api/v1/auth/login` with same email
   - System finds user by email
   - Validates password → Password is random UUID (won't match)
   - Returns `401 Unauthorized: "Invalid credentials"`
   - ❌ **Result**: Password login fails

### Scenario 3: User Wants to Add Password to OAuth2 Account

If a user registered via OAuth2 and wants to enable password login:

1. User must be authenticated (via OAuth2 or existing session)
2. User calls: `PUT /api/v1/users/{id}` with `password` field
3. System updates password hash
4. ✅ **Result**: User can now use BOTH authentication methods

**Note**: The `UserService.updateUser()` method allows updating the password if the user is authenticated.

---

## Important Considerations

### 1. Password Management for OAuth2 Users

- **OAuth2-created users** have a random UUID as their password hash
- They **cannot login with email/password** until they set a password
- They can set a password via the user update endpoint: `PUT /api/v1/users/{id}`

### 2. Email Uniqueness

- Email is the **unique identifier** across both authentication methods
- One email = One user account
- Both authentication methods share the same user record

### 3. Role Assignment

- **Email/Password Registration**: Default role is `CUSTOMER` (unless admin specifies)
- **OAuth2 Registration**: Role is assigned based on email allowlists:
  - `OAUTH2_ADMIN_EMAILS` → `ADMIN`
  - `OAUTH2_STAFF_EMAILS` → `STAFF`
  - Otherwise → `CUSTOMER`

### 4. Account Merging

When an existing user logs in via OAuth2:
- ✅ `fullName` is updated from Google profile
- ✅ `role` is **preserved** (never downgraded)
- ✅ `role` is **only upgraded** if email is in OAuth2 allowlists (e.g., CUSTOMER → ADMIN/STAFF)
- ✅ `passwordHash` is **preserved** (existing password remains valid)
- ✅ User can continue using both authentication methods

**Role Preservation Rules:**
- **Never downgrade**: ADMIN/STAFF users remain ADMIN/STAFF even if not in allowlists
- **Only upgrade**: CUSTOMER users can be upgraded to STAFF/ADMIN if email is in allowlists
- **Preserve existing**: If user already has a role, it's preserved unless upgraded

---

## Recommended User Flows

### Flow 1: Primary Email/Password, Optional OAuth2
1. User registers with email/password
2. User can optionally use OAuth2 later (account will be linked automatically)
3. ✅ User can use either method

### Flow 2: Primary OAuth2, Add Password Later
1. User logs in via OAuth2
2. User sets password via `PUT /api/v1/users/{id}` (while authenticated)
3. ✅ User can use either method

### Flow 3: OAuth2 Only
1. User logs in via OAuth2
2. User never sets a password
3. ✅ User can only use OAuth2 (password login will fail)

---

## Security Considerations

1. **Password Hashing**: All passwords are hashed using BCrypt (10 rounds)
2. **OAuth2 Token Security**: JWT tokens are signed with HMAC SHA-256
3. **Account Linking**: Email-based linking is secure as long as email ownership is verified (Google verifies this)
4. **Role Updates**: OAuth2 login can update user roles based on allowlists (be careful with this)

---

## Future Enhancements (Optional)

Potential improvements to consider:

1. **Password Setup Endpoint**: Dedicated endpoint for OAuth2 users to set passwords
2. **Account Linking UI**: Frontend UI to link OAuth2 and password accounts
3. **Password Reset**: Allow OAuth2 users to set/reset passwords via email
4. **Authentication Method Tracking**: Track which method was used to create the account
5. **Multiple OAuth2 Providers**: Support for GitHub, Facebook, etc. (already possible with Spring Security)

---

## Testing Scenarios

### Test 1: Email/Password → OAuth2
```bash
# 1. Register with email/password
POST /api/v1/users
{ "email": "test@example.com", "password": "password123", "fullName": "Test User" }

# 2. Login with OAuth2 (same email)
GET /oauth2/authorization/google
# Complete Google login with test@example.com

# 3. Verify: Can still login with password
POST /api/v1/auth/login
{ "email": "test@example.com", "password": "password123" }
# Should succeed
```

### Test 2: OAuth2 → Add Password
```bash
# 1. Login with OAuth2
GET /oauth2/authorization/google
# Complete Google login

# 2. Get user ID from JWT token or user profile
# 3. Set password (authenticated request)
PUT /api/v1/users/{id}
{ "password": "newpassword123" }
Authorization: Bearer <token>

# 4. Verify: Can login with password
POST /api/v1/auth/login
{ "email": "user@example.com", "password": "newpassword123" }
# Should succeed
```

### Test 3: OAuth2 → Try Password Login (Should Fail)
```bash
# 1. Login with OAuth2
GET /oauth2/authorization/google
# Complete Google login

# 2. Try password login (without setting password)
POST /api/v1/auth/login
{ "email": "user@example.com", "password": "anypassword" }
# Should fail with 401 Unauthorized
```

---

## Summary

- **Email is the unique identifier** linking both authentication methods
- **Email/Password users** can use OAuth2 later (account linking works)
- **OAuth2 users** cannot use password login until they set a password
- **OAuth2 users** can set a password via the user update endpoint
- Both methods issue the same JWT tokens and use the same authorization system

