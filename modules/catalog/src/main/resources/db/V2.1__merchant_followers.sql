CREATE TABLE merchant_followers (
    merchant_id VARCHAR(50) NOT NULL,
    user_id     VARCHAR(50) NOT NULL,
    followed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (merchant_id, user_id),
    CONSTRAINT fk_merchant_followers_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(merchant_id) ON DELETE CASCADE
);

CREATE INDEX idx_merchant_followers_user ON merchant_followers(user_id);
