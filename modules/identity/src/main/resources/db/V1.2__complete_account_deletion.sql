ALTER TABLE account_deletion_requests
    ADD COLUMN completed_at TIMESTAMPTZ;

UPDATE users
SET email = NULL,
    phone = NULL,
    username = 'deleted_' || user_id,
    password_hash = NULL,
    display_name = 'Deleted user',
    avatar_url = NULL,
    email_verified_at = NULL,
    phone_verified_at = NULL,
    mfa_enabled = FALSE,
    mfa_secret = NULL,
    failed_login_attempts = 0,
    locked_until = NULL,
    deleted_at = COALESCE(deleted_at, NOW())
WHERE status = 'DELETED';

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_phone_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_username_key;

CREATE UNIQUE INDEX uq_users_active_email
    ON users (email)
    WHERE deleted_at IS NULL AND email IS NOT NULL;

CREATE UNIQUE INDEX uq_users_active_phone
    ON users (phone)
    WHERE deleted_at IS NULL AND phone IS NOT NULL;

CREATE UNIQUE INDEX uq_users_active_username
    ON users (username)
    WHERE deleted_at IS NULL AND username IS NOT NULL;
