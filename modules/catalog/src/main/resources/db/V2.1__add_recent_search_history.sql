ALTER TABLE user_browsing_history
    ADD COLUMN recent_searches JSONB NOT NULL DEFAULT '[]'::jsonb;
