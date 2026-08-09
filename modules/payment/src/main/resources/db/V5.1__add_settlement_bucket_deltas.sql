ALTER TABLE merchant_balances
    ADD COLUMN receivable NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_merchant_balance_receivable_non_negative CHECK (receivable >= 0);

ALTER TABLE settlement_ledger
    ADD COLUMN pending_delta NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN available_delta NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN receivable_delta NUMERIC(18,2) NOT NULL DEFAULT 0;

ALTER TABLE settlement_ledger DROP CONSTRAINT chk_ledger_kind;
ALTER TABLE settlement_ledger
    ADD CONSTRAINT chk_ledger_kind CHECK (kind IN (
        'SALE', 'MOVE_AVAILABLE', 'REVERSAL', 'REFUND',
        'PAYOUT_DEBIT', 'PAYOUT_REVERSAL', 'BALANCE_BASELINE'
    ));

UPDATE settlement_ledger
SET pending_delta = CASE
        WHEN kind = 'SALE' THEN net
        WHEN kind = 'MOVE_AVAILABLE' THEN -net
        ELSE 0
    END,
    available_delta = CASE
        WHEN kind IN ('MOVE_AVAILABLE', 'REFUND', 'PAYOUT_DEBIT', 'PAYOUT_REVERSAL') THEN net
        ELSE 0
    END,
    receivable_delta = 0;

INSERT INTO settlement_ledger (
    entry_id, merchant_id, kind, gross, commission, net,
    pending_delta, available_delta, receivable_delta,
    currency, note, created_at
)
WITH reconstructed AS (
    SELECT merchant_id,
           currency,
           COALESCE(SUM(pending_delta), 0) AS pending,
           COALESCE(SUM(available_delta), 0) AS available,
           COALESCE(SUM(receivable_delta), 0) AS receivable
    FROM settlement_ledger
    GROUP BY merchant_id, currency
)
SELECT
    'SLE_BASE_' || MD5(balance.merchant_id || ':' || balance.currency),
    balance.merchant_id,
    'BALANCE_BASELINE',
    0,
    0,
    0,
    balance.pending - COALESCE(reconstructed.pending, 0),
    balance.available - COALESCE(reconstructed.available, 0),
    balance.receivable - COALESCE(reconstructed.receivable, 0),
    balance.currency,
    'Balance baseline introduced with bucket reconciliation',
    NOW()
FROM merchant_balances balance
LEFT JOIN reconstructed
  ON reconstructed.merchant_id = balance.merchant_id
 AND reconstructed.currency = balance.currency;
