CREATE TABLE product_questions (
    question_id  VARCHAR(50) PRIMARY KEY,
    product_id   VARCHAR(50) NOT NULL,
    user_id      VARCHAR(50) NOT NULL,
    question     TEXT        NOT NULL,
    answer       TEXT,
    answered_by  VARCHAR(50),
    answered_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_product_questions_product
        FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE INDEX idx_product_questions_product ON product_questions(product_id, created_at DESC);
CREATE INDEX idx_product_questions_user ON product_questions(user_id);
