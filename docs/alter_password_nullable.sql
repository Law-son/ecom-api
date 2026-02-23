-- Make password_hash nullable for OAuth2 users
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
