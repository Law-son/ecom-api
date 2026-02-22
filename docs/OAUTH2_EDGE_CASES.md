# OAuth2 and Email/Password Authentication - Edge Cases

This document details all edge cases and how they are handled in the authentication system.

## Role Preservation

### Problem
When an existing user with a role (e.g., ADMIN) logs in via OAuth2, their role was being overwritten to CUSTOMER if their email wasn't in the OAuth2 allowlists.

### Solution
**Role preservation logic** ensures that:
- ✅ **Existing roles are never downgraded** (ADMIN/STAFF → CUSTOMER)
- ✅ **Roles are only upgraded** if email is in allowlists (CUSTOMER → STAFF/ADMIN)
- ✅ **New users** get roles based on allowlists (default: CUSTOMER)

### Role Hierarchy
```
CUSTOMER < STAFF < ADMIN
```

### Rules

1. **New User (First OAuth2 Login)**
   - Role assigned from allowlists
   - Default: CUSTOMER if not in any allowlist

2. **Existing User - Role Preservation**
   - **ADMIN user**: Stays ADMIN (never downgraded)
   - **STAFF user**: Stays STAFF (never downgraded)
   - **CUSTOMER user**: Can be upgraded to STAFF/ADMIN if email is in allowlists

3. **Role Upgrades Only**
   - CUSTOMER → STAFF (if email in `OAUTH2_STAFF_EMAILS`)
   - CUSTOMER → ADMIN (if email in `OAUTH2_ADMIN_EMAILS`)
   - STAFF → ADMIN (if email in `OAUTH2_ADMIN_EMAILS`)

## Edge Cases

### Case 1: Admin User Logs In via OAuth2

**Scenario:**
- User has ADMIN role (created via email/password or manually)
- User logs in via OAuth2
- Email is NOT in `OAUTH2_ADMIN_EMAILS`

**Behavior:**
- ✅ Role remains ADMIN (preserved)
- ✅ Full name updated from Google profile
- ✅ Password preserved (can still use email/password login)

**Result:** User keeps ADMIN role, can use both authentication methods

---

### Case 2: Customer User Upgraded via OAuth2

**Scenario:**
- User has CUSTOMER role (created via email/password)
- User logs in via OAuth2
- Email IS in `OAUTH2_ADMIN_EMAILS`

**Behavior:**
- ✅ Role upgraded to ADMIN
- ✅ Full name updated from Google profile
- ✅ Password preserved

**Result:** User upgraded to ADMIN, can use both authentication methods

---

### Case 3: OAuth2 User Sets Password

**Scenario:**
- User created via OAuth2 (has CUSTOMER role)
- User sets password via `PUT /api/v1/users/{id}`

**Behavior:**
- ✅ Password is set
- ✅ Role remains CUSTOMER (unless upgraded via allowlists)
- ✅ User can now use both OAuth2 and email/password login

**Result:** User can use both authentication methods

---

### Case 4: Email/Password User Uses OAuth2

**Scenario:**
- User registered via email/password (has ADMIN role)
- User logs in via OAuth2 with same email

**Behavior:**
- ✅ Account linked automatically (same email)
- ✅ Role preserved (ADMIN)
- ✅ Full name updated from Google profile
- ✅ Password preserved

**Result:** User can use both authentication methods, role preserved

---

### Case 5: OAuth2 User Tries to Register with Email/Password

**Scenario:**
- User created via OAuth2 (has CUSTOMER role)
- User tries to register via `POST /api/v1/users` with same email

**Behavior:**
- ❌ Registration fails with `409 Conflict: "Email already exists"`
- ✅ User must use OAuth2 login or set password via `PUT /api/v1/users/{id}`

**Result:** Registration blocked, user must use existing account

---

### Case 6: OAuth2 User Tries Password Login (Before Setting Password)

**Scenario:**
- User created via OAuth2 (has random UUID as password)
- User tries to login via `POST /api/v1/auth/login` with any password

**Behavior:**
- ❌ Login fails with `401 Unauthorized: "Invalid credentials"`
- ✅ User must set password first via `PUT /api/v1/users/{id}`

**Result:** Password login fails until password is set

---

### Case 7: Staff User Logs In via OAuth2

**Scenario:**
- User has STAFF role (created manually or via email/password)
- User logs in via OAuth2
- Email is NOT in any allowlist

**Behavior:**
- ✅ Role remains STAFF (preserved)
- ✅ Full name updated from Google profile
- ✅ Password preserved

**Result:** User keeps STAFF role, can use both authentication methods

---

### Case 8: Customer User Not in Allowlists Logs In via OAuth2

**Scenario:**
- User has CUSTOMER role (created via email/password)
- User logs in via OAuth2
- Email is NOT in any allowlist

**Behavior:**
- ✅ Role remains CUSTOMER (no change)
- ✅ Full name updated from Google profile
- ✅ Password preserved

**Result:** User keeps CUSTOMER role, can use both authentication methods

---

### Case 9: User in Multiple Allowlists

**Scenario:**
- User logs in via OAuth2
- Email is in BOTH `OAUTH2_ADMIN_EMAILS` and `OAUTH2_STAFF_EMAILS`

**Behavior:**
- ✅ ADMIN role takes precedence (highest priority)
- ✅ If existing user has STAFF role, upgraded to ADMIN
- ✅ If existing user has ADMIN role, stays ADMIN

**Result:** User gets ADMIN role (highest priority)

---

### Case 10: OAuth2 User Changes Email

**Scenario:**
- User created via OAuth2 with email `user@example.com`
- User changes Google account email to `newuser@example.com`
- User logs in with new email

**Behavior:**
- ✅ New account created with `newuser@example.com`
- ✅ Old account (`user@example.com`) remains in database
- ⚠️ **Note**: This creates a separate account (email is unique identifier)

**Result:** Two separate accounts exist (old and new email)

---

## Implementation Details

### Role Preservation Logic

The `shouldUpgradeRole()` method in both `CustomOAuth2UserService` and `CustomOidcUserService` implements the following logic:

```java
private static boolean shouldUpgradeRole(UserRole existingRole, UserRole resolvedRole) {
    // Never downgrade
    if (resolvedRole == UserRole.CUSTOMER) {
        return false; // Never downgrade to CUSTOMER
    }
    
    // Upgrade if resolved role is higher
    if (existingRole == UserRole.CUSTOMER && resolvedRole != UserRole.CUSTOMER) {
        return true; // Upgrade CUSTOMER to STAFF or ADMIN
    }
    if (existingRole == UserRole.STAFF && resolvedRole == UserRole.ADMIN) {
        return true; // Upgrade STAFF to ADMIN
    }
    
    // If roles are equal or existing is higher, don't change
    return false;
}
```

### Account Linking

- **Email is the unique identifier** - one email = one user account
- **Automatic linking** - accounts are linked by email automatically
- **Password preservation** - existing passwords are preserved when linking
- **Role preservation** - existing roles are preserved (never downgraded)

## Best Practices

1. **Role Management:**
   - Use OAuth2 allowlists for initial role assignment
   - Use admin endpoints to manually manage roles for existing users
   - OAuth2 login will not downgrade existing roles

2. **Account Security:**
   - Users should set passwords after OAuth2 signup if they want password login
   - Email verification is handled by OAuth2 provider (Google)

3. **User Management:**
   - Monitor accounts created via OAuth2
   - Use role allowlists to grant elevated permissions
   - Existing roles are preserved, so manual role changes persist

## Testing Scenarios

### Test 1: Admin Role Preservation
```bash
# 1. Create admin user via email/password
POST /api/v1/users
{ "email": "admin@example.com", "password": "pass", "fullName": "Admin", "role": "ADMIN" }

# 2. Login via OAuth2 (email not in allowlists)
GET /oauth2/authorization/google
# Complete Google login with admin@example.com

# 3. Verify: User still has ADMIN role
GET /api/v1/users/{id}
Authorization: Bearer <token>
# Should return role: ADMIN
```

### Test 2: Role Upgrade
```bash
# 1. Create customer user via email/password
POST /api/v1/users
{ "email": "user@example.com", "password": "pass", "fullName": "User" }

# 2. Add email to admin allowlist in .env
OAUTH2_ADMIN_EMAILS=user@example.com

# 3. Login via OAuth2
GET /oauth2/authorization/google
# Complete Google login

# 4. Verify: User upgraded to ADMIN
GET /api/v1/users/{id}
Authorization: Bearer <token>
# Should return role: ADMIN
```

### Test 3: Password Preservation
```bash
# 1. Create user via email/password
POST /api/v1/users
{ "email": "user@example.com", "password": "originalpass", "fullName": "User" }

# 2. Login via OAuth2
GET /oauth2/authorization/google
# Complete Google login

# 3. Verify: Can still login with original password
POST /api/v1/auth/login
{ "email": "user@example.com", "password": "originalpass" }
# Should succeed
```

## Summary

All edge cases are handled with the following principles:

1. **Role Preservation**: Existing roles are never downgraded
2. **Role Upgrades**: Roles are only upgraded if email is in allowlists
3. **Account Linking**: Accounts are automatically linked by email
4. **Password Preservation**: Existing passwords are always preserved
5. **Security**: Email verification handled by OAuth2 provider

