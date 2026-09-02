UPDATE users
SET password_hash = '$2a$10$WsgwuxAPXYM/5D8JB5XPe.ZUGdNIgBNjudsMlNzS1e7tbchI8erBq',
    updated_at = NOW()
WHERE email LIKE '%@example.test'
  AND password_hash IS NULL;
