-- Expand mfa_secret column to store AES-GCM encrypted TOTP secret (Base64 encoded ciphertext with salt and IV)
ALTER TABLE users ALTER COLUMN mfa_secret TYPE VARCHAR(255);

