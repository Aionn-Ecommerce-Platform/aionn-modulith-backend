-- Baseline late-delivery compensation campaign + voucher referenced by
-- LateDeliveryCompensationListener. Without these rows every late-delivery
-- shipment fails with VOUCHER_NOT_FOUND and retries forever.
INSERT INTO promotion_campaigns (
    campaign_id, name, type, budget, budget_remaining, currency,
    start_date, end_date, created_by, status, version, created_at, updated_at
) VALUES (
    'CMP_LATE_DELIVERY', 'Late delivery compensation', 'DISCOUNT',
    1000000000, 1000000000, 'VND',
    NOW() - INTERVAL '1 day', NOW() + INTERVAL '10 years', 'system', 'RUNNING',
    0, NOW(), NOW()
)
ON CONFLICT (campaign_id) DO NOTHING;

INSERT INTO vouchers (
    voucher_code, campaign_id, discount_amount, currency, usage_limit,
    used_count, reserved_count, valid_from, valid_until, scope, type, version,
    created_at, updated_at
) VALUES (
    'LATEDELIVERY15K', 'CMP_LATE_DELIVERY', 15000, 'VND', 1000000,
    0, 0, NOW() - INTERVAL '1 day', NOW() + INTERVAL '10 years', 'PLATFORM',
    'ORDER_DISCOUNT', 0, NOW(), NOW()
)
ON CONFLICT (voucher_code) DO NOTHING;
