# OAuth2 Google Login Flow - Complete Technical Guide

This document explains exactly how your application handles OAuth2 authentication with Google, from the moment a user clicks "Login with Google" to when they're fully authenticated with JWT tokens.

---

## Table of Contents
1. [Overview](#overview)
2. [Configuration Files](#configuration-files)
3. [Step-by-Step Flow](#step-by-step-flow)
4. [Key Classes and Methods](#key-classes-and-methods)
5. [Database Operations](#database-operations)
6. [Security and Role Management](#security-and-role-management)

---

## Overview

**Authentication Flow Type:** Server-side OAuth2 Authorization Code Flow  
**Provider:** Google OAuth2  
**Token Type:** JWT (JSON Web Tokens)  
**Session Management:** Stateless (JWT-based)

**Key Points:**
- OAuth2 login automatically handles both login AND signup
- New users are created automatically on first Google login
- Email is the unique identifier for account linking
- Supports both OAuth2 and email/password authentication

---

## Configuration Files

### 1. Security Configuration
**File:** `src/main/java/com/eyarko/ecom/config/SecurityConfig.java`

**Relevant Method:** `oauth2SecurityFilterChain()` (lines 138-169)

```java
@Bean
@Order(1)
public SecurityFilterChain oauth2SecurityFilterChain(
    HttpSecurity http,
    CustomOAuth2UserService customOAuth2UserService,
    CustomOidcUserService customOidcUserService,
    OAuth2AuthenticationSuccessHandler successHandler,
    CorsConfigurationSource corsConfigurationSource,
    RateLimitFilter rateLimitFilter
) throws Exception
```

**What it does:**
- Configures OAuth2 login endpoints (`/oauth2/**`, `/login/oauth2/**`)
- Disables CSRF for OAuth2 (uses state parameter instead)
- Registers custom user services for loading user data
- Sets up success handler for post-authentication

### 2. Application Properties
**File:** `src/main/resources/application.properties` or `.env`

**Required Environment Variables:**
```properties
GOOGLE_OAUTH_CLIENT_ID=your-client-id
GOOGLE_OAUTH_CLIENT_SECRET=your-client-secret
OAUTH2_REDIRECT_URI=http://localhost:5173/oauth2/redirect
OAUTH2_ADMIN_EMAILS=admin@example.com,owner@example.com
OAUTH2_STAFF_EMAILS=staff@example.com
```

---

## Step-by-Step Flow

### Step 1: User Clicks "Login with Google"

**Frontend Action:**
```
User navigates to: GET http://localhost:8080/oauth2/authorization/google
```

**What Happens:**
- Spring Security intercepts this request
- No custom code runs yet - this is handled by Spring Security OAuth2 Client
- Spring Security generates authorization URL with:
  - Client ID
  - Redirect URI (`http://localhost:8080/login/oauth2/code/google`)
  - Scope (profile, email)
  - State parameter (CSRF protection)

**Result:** User is redirected to Google's consent screen

---

### Step 2: User Authorizes on Google

**Google's Actions:**
- User sees consent screen asking for permission to share profile and email
- User clicks "Allow"
- Google generates authorization code

**Result:** Google redirects back to your application

---

### Step 3: Google Redirects Back with Authorization Code

**Redirect URL:**
```
GET http://localhost:8080/login/oauth2/code/google?code=AUTHORIZATION_CODE&state=STATE_VALUE
```

**What Happens:**
- Spring Security intercepts this callback
- Validates state parameter (CSRF protection)
- Exchanges authorization code for access token (behind the scenes)
- Calls your custom user service to load user details

---

### Step 4: Load User from Google (Custom User Service)

**Two Possible Paths:**

#### Path A: Standard OAuth2 (without "openid" scope)
**File:** `src/main/java/com/eyarko/ecom/security/CustomOAuth2UserService.java`  
**Method:** `loadUser(OAuth2UserRequest userRequest)` (lines 48-145)

#### Path B: OIDC (with "openid" scope)
**File:** `src/main/java/com/eyarko/ecom/security/CustomOidcUserService.java`  
**Method:** `loadUser(OidcUserRequest userRequest)` (lines 49-147)

**Both paths do the same thing:**

1. **Extract user info from Google:**
   ```java
   String email = oauth2User.getAttribute("email");
   String name = oauth2User.getAttribute("name");
   ```

2. **Validate email exists:**
   ```java
   if (email == null || email.isBlank()) {
       throw new OAuth2AuthenticationException(
           new OAuth2Error("invalid_user"), 
           "Google account has no email"
       );
   }
   ```

3. **Resolve user role based on email allowlists:**
   
   **File:** `src/main/java/com/eyarko/ecom/security/OAuth2RoleResolver.java`  
   **Method:** `resolveRole(String email)` (lines 33-45)
   
   ```java
   public UserRole resolveRole(String email) {
       String normalized = email.trim().toLowerCase();
       if (adminEmails.contains(normalized)) {
           return UserRole.ADMIN;
       }
       if (staffEmails.contains(normalized)) {
           return UserRole.STAFF;
       }
       return UserRole.CUSTOMER;
   }
   ```

4. **Check if user exists in database:**
   
   **Repository:** `src/main/java/com/eyarko/ecom/repository/UserRepository.java`  
   **Method:** `findByEmailIgnoreCase(String email)`
   
   ```java
   User user = userRepository.findByEmailIgnoreCase(email)
       .map(existing -> {
           // User exists - update and preserve role
       })
       .orElseGet(() -> {
           // User doesn't exist - create new user
       });
   ```

---

### Step 5: User Provisioning (Create or Update)

#### Scenario A: Existing User (Login)

**What Happens:**
1. Reload user from database to get latest data
2. Store original role from database
3. Update full name from Google
4. Apply role preservation logic:
   - **Never downgrade** (ADMIN/STAFF → CUSTOMER)
   - **Only upgrade** if email is in allowlists (CUSTOMER → STAFF/ADMIN)
   - **Preserve existing role** if not in allowlists

**Code Reference:** `CustomOAuth2UserService.java` (lines 66-95)

```java
User freshUser = userRepository.findById(existing.getId()).orElse(existing);
UserRole originalRole = freshUser.getRole();

// Update name
freshUser.setFullName(name != null && !name.isBlank() ? name : freshUser.getFullName());

// Role preservation logic
if (shouldUpgradeRole(originalRole, resolvedRole)) {
    freshUser.setRole(resolvedRole);  // Upgrade
} else {
    freshUser.setRole(originalRole);  // Preserve
}
```

**Role Upgrade Logic:** `shouldUpgradeRole()` method (lines 147-171)
- CUSTOMER → STAFF: ✅ Allowed
- CUSTOMER → ADMIN: ✅ Allowed
- STAFF → ADMIN: ✅ Allowed
- ADMIN → STAFF: ❌ Blocked (preserve ADMIN)
- STAFF → CUSTOMER: ❌ Blocked (preserve STAFF)
- ADMIN → CUSTOMER: ❌ Blocked (preserve ADMIN)

#### Scenario B: New User (Signup)

**What Happens:**
1. Create new User entity with:
   - Email from Google
   - Full name from Google (or email if name is blank)
   - **Password: NULL** (OAuth users don't need password)
   - Role from allowlists (ADMIN/STAFF/CUSTOMER)

**Code Reference:** `CustomOAuth2UserService.java` (lines 96-102)

```java
User.builder()
    .email(email)
    .fullName((name == null || name.isBlank()) ? email : name)
    .passwordHash(null)  // OAuth2 users have no password
    .role(resolvedRole)
    .build()
```

---

### Step 6: Save User to Database

**What Happens:**
1. Save user entity to database
2. Flush to ensure immediate persistence
3. Evict user cache to prevent stale data
4. Verify role was persisted correctly (defensive check)

**Code Reference:** `CustomOAuth2UserService.java` (lines 104-143)

```java
user = userRepository.save(user);
userRepository.flush();

// Evict cache
Cache userCache = cacheManager.getCache("userById");
if (userCache != null) {
    userCache.evict(user.getId());
}

// Verify role persistence
User persistedUser = userRepository.findById(user.getId()).orElse(null);
if (!persistedUser.getRole().equals(roleToPreserve)) {
    // Fix role mismatch (should never happen)
    persistedUser.setRole(roleToPreserve);
    persistedUser = userRepository.saveAndFlush(persistedUser);
}
```

**Database Table:** `users`
```sql
INSERT INTO users (email, full_name, password_hash, role, created_at, last_login, version)
VALUES ('user@gmail.com', 'John Doe', NULL, 'CUSTOMER', NOW(), NULL, 0);
```

---

### Step 7: Wrap User in Security Principal

**What Happens:**
- User entity is wrapped in `UserPrincipal` (application's security principal)
- Then wrapped in `UserPrincipalOAuth2User` (OAuth2-compatible wrapper)
- This allows Spring Security to work with our custom User entity

**Code Reference:** `CustomOAuth2UserService.java` (line 145)

```java
return new UserPrincipalOAuth2User(UserPrincipal.fromUser(user), attrs);
```

**Classes Involved:**
- `src/main/java/com/eyarko/ecom/security/UserPrincipal.java`
- `src/main/java/com/eyarko/ecom/security/UserPrincipalOAuth2User.java`

---

### Step 8: Authentication Success Handler

**File:** `src/main/java/com/eyarko/ecom/security/OAuth2AuthenticationSuccessHandler.java`  
**Method:** `onAuthenticationSuccess()` (lines 47-103)

**What Happens:**

1. **Extract user from authentication:**
   ```java
   Object principal = authentication.getPrincipal();
   
   if (principal instanceof UserPrincipalOAuth2User) {
       UserPrincipalOAuth2User oauth2User = (UserPrincipalOAuth2User) principal;
       Long userId = oauth2User.getPrincipal().getId();
       user = userRepository.findById(userId).orElseThrow(...);
   }
   ```

2. **Generate JWT access token:**
   
   **File:** `src/main/java/com/eyarko/ecom/security/JwtService.java`  
   **Method:** `generateToken(User user)`
   
   ```java
   String accessToken = jwtService.generateToken(user);
   ```
   
   **Token Claims:**
   - `sub`: User email
   - `userId`: User ID
   - `role`: User role (CUSTOMER/STAFF/ADMIN)
   - `fullName`: User's full name
   - `iat`: Issued at timestamp
   - `exp`: Expiration timestamp (60 minutes)

3. **Generate refresh token:**
   
   **File:** `src/main/java/com/eyarko/ecom/service/RefreshTokenService.java`  
   **Method:** `createRefreshToken(User user)`
   
   ```java
   RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
   ```
   
   **Refresh Token:**
   - Random UUID string
   - Stored in database (`refresh_tokens` table)
   - Expires in 7 days
   - Used to get new access tokens without re-authentication

4. **Build redirect URL with tokens:**
   ```java
   String url = redirectUri
       + "?accessToken=" + URLEncoder.encode(accessToken)
       + "&refreshToken=" + URLEncoder.encode(refreshToken.getToken())
       + "&tokenType=Bearer";
   ```

5. **Redirect to frontend:**
   ```java
   response.sendRedirect(url);
   ```

**Result:** User is redirected to frontend with tokens in URL

---

### Step 9: Frontend Receives Tokens

**Redirect URL:**
```
http://localhost:5173/oauth2/redirect?accessToken=eyJhbGc...&refreshToken=550e8400-e29b-41d4-a716-446655440000&tokenType=Bearer
```

**Frontend Actions:**
1. Extract tokens from URL query parameters
2. Store tokens securely (localStorage, sessionStorage, or memory)
3. Remove tokens from URL (for security)
4. Redirect user to dashboard/home page

**Example Frontend Code:**
```javascript
const params = new URLSearchParams(window.location.search);
const accessToken = params.get('accessToken');
const refreshToken = params.get('refreshToken');

// Store tokens
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);

// Clean URL
window.history.replaceState({}, document.title, '/dashboard');
```

---

### Step 10: Using Access Token for API Requests

**Frontend sends authenticated requests:**
```javascript
fetch('http://localhost:8080/api/v1/orders', {
    headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
    }
})
```

**Backend validates token:**

**File:** `src/main/java/com/eyarko/ecom/security/JwtAuthenticationFilter.java`  
**Method:** `doFilterInternal()` (lines 66-157)

1. Extract token from Authorization header
2. Check if token is blacklisted (revoked)
3. Validate token signature and expiration
4. Load user details from token
5. Set authentication in SecurityContext
6. Log token usage for security monitoring

---

## Key Classes and Methods

### 1. Security Configuration
**File:** `SecurityConfig.java`
- `oauth2SecurityFilterChain()` - Configures OAuth2 endpoints and handlers

### 2. User Services
**File:** `CustomOAuth2UserService.java`
- `loadUser(OAuth2UserRequest)` - Loads/creates user from OAuth2 data

**File:** `CustomOidcUserService.java`
- `loadUser(OidcUserRequest)` - Loads/creates user from OIDC data

### 3. Role Resolution
**File:** `OAuth2RoleResolver.java`
- `resolveRole(String email)` - Maps email to role (ADMIN/STAFF/CUSTOMER)

### 4. Success Handler
**File:** `OAuth2AuthenticationSuccessHandler.java`
- `onAuthenticationSuccess()` - Generates tokens and redirects to frontend

### 5. JWT Service
**File:** `JwtService.java`
- `generateToken(User)` - Creates JWT access token
- `extractUsername(String)` - Extracts email from token
- `isTokenValid(String, String)` - Validates token

### 6. Refresh Token Service
**File:** `RefreshTokenService.java`
- `createRefreshToken(User)` - Creates refresh token
- `validateRefreshToken(String)` - Validates and returns refresh token

### 7. User Repository
**File:** `UserRepository.java`
- `findByEmailIgnoreCase(String)` - Finds user by email (case-insensitive)

### 8. Security Principals
**File:** `UserPrincipal.java`
- `fromUser(User)` - Converts User entity to Spring Security principal

**File:** `UserPrincipalOAuth2User.java`
- Wrapper combining UserPrincipal with OAuth2User attributes

---

## Database Operations

### Tables Involved

#### 1. `users` Table
**Schema:** `docs/schema.sql` (lines 44-54)

```sql
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255),  -- NULL for OAuth users
    role user_role NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
```

**Operations:**
- `INSERT` - New OAuth user signup
- `UPDATE` - Existing user login (update name, role)
- `SELECT` - Find user by email

#### 2. `refresh_tokens` Table
**Operations:**
- `INSERT` - Create new refresh token
- `SELECT` - Validate refresh token
- `DELETE` - Revoke refresh token on logout

---

## Security and Role Management

### Role Hierarchy
```
CUSTOMER < STAFF < ADMIN
```

### Role Assignment Rules

1. **New Users:**
   - Check email against `OAUTH2_ADMIN_EMAILS` → ADMIN
   - Check email against `OAUTH2_STAFF_EMAILS` → STAFF
   - Default → CUSTOMER

2. **Existing Users:**
   - **Never downgrade** (preserve higher roles)
   - **Only upgrade** if email is in allowlists
   - **Preserve role** if not in allowlists

### Account Linking

**Email is the unique identifier:**
- One email = One user account
- OAuth2 and email/password share the same user record

**Scenarios:**

1. **OAuth First, Then Email/Password:**
   - User signs up with Google (password = NULL)
   - User sets password via `PUT /api/v1/users/{id}`
   - User can now use both authentication methods

2. **Email/Password First, Then OAuth:**
   - User registers with email/password
   - User logs in with Google (same email)
   - Account automatically linked (same user record)
   - Password is preserved

---

## Security Logging

All OAuth2 authentication events are logged for security monitoring:

**File:** `SecurityEventLogger.java`

**Events Logged:**
- Authentication success/failure
- Token validation
- Endpoint access
- Role changes

**Example Logs:**
```
SECURITY_EVENT: AUTH_SUCCESS | email=user@gmail.com | ip=192.168.1.1 | userAgent=Mozilla/5.0 | timestamp=2024-01-15T10:30:00Z
SECURITY_EVENT: TOKEN_VALID | email=user@gmail.com | ip=192.168.1.1 | endpoint=/api/v1/orders | timestamp=2024-01-15T10:30:00Z
```

---

## Flow Diagram

```
User Clicks "Login with Google"
         ↓
GET /oauth2/authorization/google
         ↓
Spring Security generates authorization URL
         ↓
Redirect to Google consent screen
         ↓
User authorizes on Google
         ↓
Google redirects back with authorization code
         ↓
GET /login/oauth2/code/google?code=...
         ↓
Spring Security exchanges code for access token
         ↓
CustomOAuth2UserService.loadUser()
         ↓
Extract email and name from Google
         ↓
OAuth2RoleResolver.resolveRole(email)
         ↓
Check if user exists in database
         ↓
┌─────────────────┬─────────────────┐
│  User Exists    │  User New       │
│  (Login)        │  (Signup)       │
├─────────────────┼─────────────────┤
│ Update name     │ Create user     │
│ Preserve role   │ Set role        │
│ Keep password   │ password = NULL │
└─────────────────┴─────────────────┘
         ↓
Save user to database
         ↓
Wrap in UserPrincipalOAuth2User
         ↓
OAuth2AuthenticationSuccessHandler.onAuthenticationSuccess()
         ↓
Generate JWT access token (60 min)
         ↓
Generate refresh token (7 days)
         ↓
Build redirect URL with tokens
         ↓
Redirect to frontend
         ↓
http://localhost:5173/oauth2/redirect?accessToken=...&refreshToken=...
         ↓
Frontend extracts and stores tokens
         ↓
User is fully authenticated ✅
```

---

## Testing the Flow

### 1. Start OAuth2 Login
```
GET http://localhost:8080/oauth2/authorization/google
```

### 2. After Redirect, Check Tokens
```javascript
// Frontend receives:
const accessToken = params.get('accessToken');
const refreshToken = params.get('refreshToken');
```

### 3. Decode Access Token
Visit https://jwt.io and paste the access token to see claims:
```json
{
  "sub": "user@gmail.com",
  "userId": 123,
  "role": "CUSTOMER",
  "fullName": "John Doe",
  "iat": 1705320600,
  "exp": 1705324200
}
```

### 4. Use Token for API Requests
```bash
curl -H "Authorization: Bearer <accessToken>" \
     http://localhost:8080/api/v1/orders
```

### 5. Refresh Token When Expired
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
     -H "Content-Type: application/json" \
     -d '{"refreshToken": "<refreshToken>"}'
```

---

## Troubleshooting

### Common Issues

1. **"Google account has no email"**
   - Ensure Google OAuth2 scope includes `email`
   - Check Google account has verified email

2. **"OAuth2 user not found after login"**
   - Database transaction issue
   - Check logs for save errors

3. **Role not preserved correctly**
   - Check `OAUTH2_ADMIN_EMAILS` and `OAUTH2_STAFF_EMAILS` configuration
   - Review logs for role upgrade/preservation messages

4. **Redirect URI mismatch**
   - Ensure Google Cloud Console redirect URI matches exactly
   - Must be: `http://localhost:8080/login/oauth2/code/google`

---

## Summary

Your OAuth2 implementation is a **complete, production-ready authentication system** that:

✅ Handles both login and signup automatically  
✅ Links OAuth2 and email/password accounts  
✅ Preserves user roles (never downgrades)  
✅ Generates JWT tokens for stateless authentication  
✅ Logs all security events for monitoring  
✅ Supports role-based access control (RBAC)  
✅ Implements refresh token rotation  
✅ Validates tokens on every request  

**Key Files to Remember:**
1. `SecurityConfig.java` - OAuth2 configuration
2. `CustomOAuth2UserService.java` - User loading and provisioning
3. `OAuth2RoleResolver.java` - Role assignment
4. `OAuth2AuthenticationSuccessHandler.java` - Token generation and redirect
5. `JwtService.java` - JWT token operations
6. `RefreshTokenService.java` - Refresh token management
