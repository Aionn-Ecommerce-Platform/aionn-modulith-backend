INSERT INTO settlement_ledger (
    entry_id, merchant_id, kind, gross, commission, net,
    pending_delta, available_delta, receivable_delta,
    currency, note, created_at
)
SELECT
    'SLE_DEMO_BASE_' || MD5(merchant_id || ':' || currency),
    merchant_id,
    'BALANCE_BASELINE',
    0,
    0,
    0,
    pending,
    available,
    receivable,
    currency,
    'Demo balance baseline',
    NOW()
FROM merchant_balances;
