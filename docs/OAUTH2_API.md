# OAuth2 Authentication API Documentation

## Overview

OAuth2 authentication via Google is fully supported and **automatically handles both login and signup**. New users are automatically created when they log in with Google for the first time - no separate signup endpoint is required.

## Endpoints

### OAuth2 Login/Signup

**Endpoint:** `GET /oauth2/authorization/google`

**Description:** Initiates OAuth2 authentication flow with Google. This endpoint handles both login (for existing users) and signup (for new users) automatically.

**Request:**
- **Method:** `GET`
- **URL:** `http://localhost:8080/oauth2/authorization/google`
- **Headers:** None required
- **Body:** None

**Response:**
- **Type:** Redirect (302)
- **Flow:**
  1. User is redirected to Google's consent screen
  2. After authorization, Google redirects back to: `http://localhost:8080/login/oauth2/code/google`
  3. Backend processes the OAuth2 callback
  4. User is redirected to frontend with tokens: `{OAUTH2_REDIRECT_URI}?accessToken=...&refreshToken=...&tokenType=Bearer`

**Success Response (via redirect):**
```
HTTP/1.1 302 Found
Location: http://localhost:5173/oauth2/redirect?accessToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...&refreshToken=550e8400-e29b-41d4-a716-446655440000&tokenType=Bearer
```

**Error Responses:**
- `403 Forbidden`: CSRF protection blocking (should not occur with proper configuration)
- `401 Unauthorized`: Invalid OAuth2 credentials or Google authentication failure
- `500 Internal Server Error`: Server error during user creation or token generation

## User Account Creation (Automatic Signup)

### How It Works

When a user logs in with Google for the first time:

1. **User doesn't exist:**
   - New user account is automatically created
   - Email, full name, and role are set from Google profile
   - Password is set to a random UUID (not usable for password login)
   - User can set a password later via `PUT /api/v1/users/{id}`

2. **User already exists:**
   - Existing user account is found by email
   - Full name and role are updated from Google profile
   - Existing password is preserved (user can still use email/password login)

### Account Details

**New User Creation:**
- **Email:** From Google profile (required)
- **Full Name:** From Google profile (or email if name not available)
- **Role:** Determined by email allowlists:
  - `ADMIN` if email in `OAUTH2_ADMIN_EMAILS`
  - `STAFF` if email in `OAUTH2_STAFF_EMAILS`
  - `CUSTOMER` (default) if email not in any allowlist
- **Password:** Random UUID (not usable for password login until user sets one)

## Integration with Email/Password Authentication

OAuth2 and email/password authentication work seamlessly together using **email as the unique identifier**.

### Account Linking Scenarios

#### Scenario 1: Signup via OAuth2, Then Add Password

1. **User signs up via OAuth2:**
   ```
   GET /oauth2/authorization/google
   → User completes Google login
   → Account created automatically
   → Tokens issued
   ```

2. **User sets password (optional):**
   ```
   PUT /api/v1/users/{id}
   Authorization: Bearer <accessToken>
   {
     "password": "user-chosen-password"
   }
   ```

3. **Result:** User can now use both OAuth2 and email/password login

#### Scenario 2: Signup via Email/Password, Then Use OAuth2

1. **User registers via email/password:**
   ```
   POST /api/v1/users
   {
     "email": "user@example.com",
     "password": "password123",
     "fullName": "User Name"
   }
   ```

2. **User logs in via OAuth2 (same email):**
   ```
   GET /oauth2/authorization/google
   → User completes Google login with user@example.com
   → Existing account found by email
   → Account linked automatically
   → Tokens issued
   ```

3. **Result:** User can use both authentication methods

### Important Notes

- **Email is the unique identifier** - one email = one user account
- **Automatic account linking** - accounts are linked by email automatically
- **Password preservation** - existing passwords are preserved when linking accounts
- **Role updates** - roles are updated based on current OAuth2 allowlists on each login

## Configuration

### Environment Variables

Set these in your `.env` file or system environment:

```bash
# Required
GOOGLE_OAUTH_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_OAUTH_CLIENT_SECRET=your-client-secret

# Optional
OAUTH2_REDIRECT_URI=http://localhost:5173/oauth2/redirect
OAUTH2_ADMIN_EMAILS=admin@example.com,owner@example.com
OAUTH2_STAFF_EMAILS=staff@example.com
```

### Google Cloud Console Setup

1. **Authorized JavaScript Origins:**
   - Development: `http://localhost:8080`
   - Production: `https://your-domain.com`

2. **Authorized Redirect URIs:**
   - Development: `http://localhost:8080/login/oauth2/code/google`
   - Production: `https://your-domain.com/login/oauth2/code/google`

## Frontend Integration

### Handling the Redirect

After successful OAuth2 login, the user is redirected to your frontend with tokens in the query string:

```javascript
// Example: Extract tokens from URL
const urlParams = new URLSearchParams(window.location.search);
const accessToken = urlParams.get('accessToken');
const refreshToken = urlParams.get('refreshToken');
const tokenType = urlParams.get('tokenType'); // "Bearer"

// Store tokens securely (e.g., in localStorage or httpOnly cookie)
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);

// Use access token for API requests
fetch('/api/v1/cart', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});
```

### React Example

```jsx
// OAuth2 Login Button
const handleGoogleLogin = () => {
  window.location.href = 'http://localhost:8080/oauth2/authorization/google';
};

// Handle OAuth2 Redirect
useEffect(() => {
  const urlParams = new URLSearchParams(window.location.search);
  const accessToken = urlParams.get('accessToken');
  const refreshToken = urlParams.get('refreshToken');
  
  if (accessToken && refreshToken) {
    // Store tokens
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    
    // Clear URL parameters
    window.history.replaceState({}, document.title, '/oauth2/redirect');
    
    // Redirect to dashboard or home
    navigate('/dashboard');
  }
}, []);
```

## Testing

### Using Browser

1. Navigate to: `http://localhost:8080/oauth2/authorization/google`
2. Complete Google login
3. Verify redirect to frontend with tokens in URL

### Using Postman

1. Create a new GET request
2. URL: `http://localhost:8080/oauth2/authorization/google`
3. Send request
4. Follow redirects (Postman will show the redirect chain)
5. Extract tokens from final redirect URL

### Using cURL

```bash
# Follow redirects and show headers
curl -L -i http://localhost:8080/oauth2/authorization/google
```

## Error Handling

### Common Issues

1. **403 Forbidden:**
   - Check CSRF configuration (should be disabled for OAuth2 endpoints)
   - Verify OAuth2 filter chain order

2. **401 Unauthorized:**
   - Verify Google OAuth2 credentials are correct
   - Check that redirect URI matches Google Cloud Console configuration

3. **User Creation Fails:**
   - Check database connection
   - Verify user table exists and is accessible
   - Check application logs for detailed error messages

## Security Considerations

- **CSRF Protection:** Disabled for OAuth2 endpoints (OAuth2 uses state parameters for security)
- **Account Linking:** Automatic by email (ensure email ownership is verified by Google)
- **Role Assignment:** Based on email allowlists (configure carefully)
- **Token Storage:** Store tokens securely on frontend (consider httpOnly cookies for production)

## Related Documentation

- `docs/AUTHENTICATION_FLOW.md` - Detailed account linking scenarios
- `docs/api.md` - Complete API reference
- `README.md` - OAuth2 setup and configuration

