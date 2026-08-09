CREATE TABLE payment_methods (
    method_id     VARCHAR(50) PRIMARY KEY,
    user_id       VARCHAR(50) NOT NULL,
    provider      VARCHAR(50) NOT NULL,
    last_4_digits VARCHAR(4),
    gateway_token VARCHAR(255) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'LINKED',
    version       BIGINT      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    verified_at   TIMESTAMPTZ
);
CREATE INDEX idx_payment_methods_user ON payment_methods(user_id);

CREATE TABLE payments (
    payment_id        VARCHAR(50) PRIMARY KEY,
    order_id          VARCHAR(50) NOT NULL,
    user_id           VARCHAR(50) NOT NULL,
    payment_method_id VARCHAR(50),
    amount            NUMERIC(18,2) NOT NULL,
    refunded_amount   NUMERIC(18,2) NOT NULL DEFAULT 0,
    currency          VARCHAR(3)  NOT NULL,
    gateway           VARCHAR(20) NOT NULL,
    idempotency_key   VARCHAR(100) NOT NULL,
    transaction_no    VARCHAR(100),
    invoice_url       TEXT,
    error_code        VARCHAR(50),
    error_reason      TEXT,
    status            VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    version           BIGINT      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    paid_at           TIMESTAMPTZ,
    failed_at         TIMESTAMPTZ,
    CONSTRAINT chk_payments_refund_le_amount CHECK (refunded_amount <= amount)
);
CREATE INDEX idx_payments_order      ON payments(order_id);
CREATE INDEX idx_payments_status     ON payments(status);
CREATE UNIQUE INDEX idx_payments_idem ON payments(idempotency_key);

CREATE TABLE payment_refund_operations (
    idempotency_key   VARCHAR(100) PRIMARY KEY,
    payment_id        VARCHAR(50)   NOT NULL,
    amount            NUMERIC(18,2) NOT NULL,
    currency          VARCHAR(3)    NOT NULL,
    reason            VARCHAR(500)  NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    provider_refund_id VARCHAR(100),
    failure_reason    VARCHAR(500),
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    CONSTRAINT chk_refund_operations_amount CHECK (amount > 0),
    CONSTRAINT chk_refund_operations_status CHECK (status IN ('PENDING','SUCCEEDED','FAILED'))
);
CREATE INDEX idx_refund_operations_payment_status
    ON payment_refund_operations(payment_id, status);

CREATE TABLE transaction_ledgers (
    ledger_id              VARCHAR(50) PRIMARY KEY,
    payment_id             VARCHAR(50) NOT NULL,
    amount                 NUMERIC(18,2) NOT NULL,
    currency               VARCHAR(3)  NOT NULL,
    type                   VARCHAR(10) NOT NULL,
    gateway                VARCHAR(20) NOT NULL,
    gateway_transaction_no VARCHAR(100),
    occurred_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ledgers_type CHECK (type IN ('CREDIT', 'DEBIT'))
);
CREATE INDEX idx_ledgers_payment      ON transaction_ledgers(payment_id);
CREATE INDEX idx_ledgers_gateway_time ON transaction_ledgers(gateway, occurred_at);
CREATE INDEX idx_ledgers_gateway_txn  ON transaction_ledgers(gateway_transaction_no);

CREATE TABLE payment_preferences (
    user_id VARCHAR(50) PRIMARY KEY,
    payment_type VARCHAR(20) NOT NULL DEFAULT 'COD',
    payment_method_id VARCHAR(50),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payment_preferences_type CHECK (payment_type IN ('COD', 'SAVED_CARD', 'VNPAY'))
);

CREATE TABLE merchant_balances (
    merchant_id  VARCHAR(50)   NOT NULL,
    currency     VARCHAR(3)    NOT NULL,
    pending      NUMERIC(18,2) NOT NULL DEFAULT 0,
    available    NUMERIC(18,2) NOT NULL DEFAULT 0,
    version      BIGINT        NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    PRIMARY KEY (merchant_id, currency),
    CONSTRAINT chk_balances_pending_nonneg CHECK (pending >= 0),
    CONSTRAINT chk_balances_available_nonneg CHECK (available >= 0)
);
CREATE INDEX idx_merchant_balances_merchant ON merchant_balances(merchant_id);

CREATE TABLE merchant_payouts (
    payout_id         VARCHAR(50) PRIMARY KEY,
    merchant_id       VARCHAR(50) NOT NULL,
    amount            NUMERIC(18,2) NOT NULL,
    currency          VARCHAR(3)    NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    bank_name         VARCHAR(100),
    bank_account_no   VARCHAR(50),
    bank_account_name VARCHAR(150),
    external_ref      VARCHAR(100),
    note              TEXT,
    requested_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at      TIMESTAMPTZ,
    failed_at         TIMESTAMPTZ,
    failure_reason    TEXT,
    version           BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT chk_payouts_amount_pos CHECK (amount > 0),
    CONSTRAINT chk_payouts_status CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED'))
);
CREATE INDEX idx_payouts_merchant_status ON merchant_payouts(merchant_id, status);
CREATE INDEX idx_payouts_status_requested ON merchant_payouts(status, requested_at);

CREATE TABLE settlement_ledger (
    entry_id    VARCHAR(50) PRIMARY KEY,
    merchant_id VARCHAR(50) NOT NULL,
    order_id    VARCHAR(50),
    payment_id  VARCHAR(50),
    payout_id   VARCHAR(50),
    kind        VARCHAR(20) NOT NULL,
    gross       NUMERIC(18,2) NOT NULL,
    commission  NUMERIC(18,2) NOT NULL DEFAULT 0,
    net         NUMERIC(18,2) NOT NULL,
    currency    VARCHAR(3)  NOT NULL,
    note        TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ledger_kind CHECK (kind IN ('SALE','MOVE_AVAILABLE','REVERSAL','REFUND','PAYOUT_DEBIT','PAYOUT_REVERSAL'))
);
CREATE INDEX idx_settlement_ledger_merchant ON settlement_ledger(merchant_id, created_at);
CREATE INDEX idx_settlement_ledger_order ON settlement_ledger(order_id);
CREATE INDEX idx_settlement_ledger_payout ON settlement_ledger(payout_id);
