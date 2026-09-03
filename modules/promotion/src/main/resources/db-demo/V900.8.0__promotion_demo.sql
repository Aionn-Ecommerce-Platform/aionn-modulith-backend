-- Seed Campaigns and Vouchers

INSERT INTO promotion_campaigns (campaign_id, name, type, budget, budget_remaining, currency, start_date, end_date, created_by, status, min_order_value, max_claims_per_user, max_uses_per_voucher, version, created_at, updated_at) VALUES ('CAMP_MEGA', 'Summer Mega Sale', 'DISCOUNT', 500000000.00, 500000000.00, 'VND', '2026-06-13 16:42:22', '2026-09-11 16:42:22', 'sys_admin_01', 'ACTIVE', 200000.00, NULL, 1000, 0, NOW(), NOW());

INSERT INTO vouchers (voucher_code, campaign_id, discount_amount, currency, usage_limit, used_count, reserved_count, valid_from, valid_until, version, created_at, updated_at) VALUES
('SUMMER50', 'CAMP_MEGA', 500000.0, 'VND', 500, 0, 0, '2026-06-13 16:42:22', '2026-09-11 16:42:22', 0, NOW(), NOW()),
('SUMMER10', 'CAMP_MEGA', 100000.0, 'VND', 1000, 0, 0, '2026-06-13 16:42:22', '2026-09-11 16:42:22', 0, NOW(), NOW()),
('FREESHIP', 'CAMP_MEGA', 30000.0, 'VND', 1000, 0, 0, '2026-06-13 16:42:22', '2026-09-11 16:42:22', 0, NOW(), NOW());

-- Seed Initial Banners
INSERT INTO promotion_banners (banner_id, title, image_url, link_url, display_order, active, version, created_at, updated_at) VALUES
('BAN_001', 'Aionn Siêu Sale Công Nghệ', '/images/banners/tech_sale.png', '/categories/CAT_ELE', 1, TRUE, 0, NOW(), NOW()),
('BAN_002', 'Aionn Siêu Sale Thời Trang', '/images/banners/fashion_sale.png', '/categories/CAT_FAS', 2, TRUE, 0, NOW(), NOW()),
('BAN_003', 'Aionn Trang Trí Nhà Cửa', '/images/banners/home_decor.png', '/categories/CAT_HOK', 3, TRUE, 0, NOW(), NOW());

-- Update existing banner links to point to search page with category filter instead of non-existent category routes
UPDATE promotion_banners SET link_url = '/products?categoryId=CAT_ELE' WHERE banner_id = 'BAN_001';

UPDATE promotion_banners SET link_url = '/products?categoryId=CAT_FAS' WHERE banner_id = 'BAN_002';

UPDATE promotion_banners SET link_url = '/products?categoryId=CAT_HOK' WHERE banner_id = 'BAN_003';

-- Seed the 4th banner (Beauty / Health & Beauty)
INSERT INTO promotion_banners (banner_id, title, image_url, link_url, display_order, active, version, created_at, updated_at)
VALUES ('BAN_004', 'Aionn Siêu Sale Mỹ Phẩm', '/images/banners/beauty_sale.png', '/products?categoryId=CAT_HBE', 4, TRUE, 0, NOW(), NOW())
ON CONFLICT (banner_id) DO NOTHING;

-- Demo data so the storefront has a live flash-sale to render. Campaign
-- runs for 7 days starting now; SKUs registered at varied discount rates
-- (-25% → -50%) so the storefront doesn't look like a flat 50% banner.

INSERT INTO promotion_campaigns
    (campaign_id, name, type, budget, budget_remaining, currency,
     start_date, end_date, created_by, status, version, created_at, updated_at)
VALUES
    ('CAMP_FLASH_DEMO', 'Flash Sale Cuối Tuần', 'FLASH_SALE',
     100000000.00, 100000000.00, 'VND',
     NOW() - INTERVAL '1 hour', NOW() + INTERVAL '7 days',
     'sys_admin_01', 'RUNNING', 0, NOW(), NOW());

INSERT INTO flash_sale_registrations
    (registration_id, campaign_id, merchant_id, product_id, sku_id,
     sale_price, currency, sale_stock, sold_count, status,
     submitted_at, decided_at, decided_by, updated_at, version)
VALUES
    ('FSR_DEMO_001', 'CAMP_FLASH_DEMO', 'MER_002', 'PR_0001', 'SKU_PR0001_BLK',
     403000.00, 'VND', 100, 12, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_002', 'CAMP_FLASH_DEMO', 'MER_003', 'PR_0002', 'SKU_PR0002_M_GRY',
     90000.00, 'VND', 200, 45, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_003', 'CAMP_FLASH_DEMO', 'MER_004', 'PR_0003', 'SKU_PR0003_39_WHT',
     370000.00, 'VND', 80, 8, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_004', 'CAMP_FLASH_DEMO', 'MER_005', 'PR_0004', 'SKU_PR0004_M_GRY',
     444000.00, 'VND', 150, 30, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_005', 'CAMP_FLASH_DEMO', 'MER_006', 'PR_0005', 'SKU_PR0005_256_BLK',
     5600000.00, 'VND', 50, 5, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_006', 'CAMP_FLASH_DEMO', 'MER_007', 'PR_0006', 'SKU_00006',
     341000.00, 'VND', 120, 60, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_007', 'CAMP_FLASH_DEMO', 'MER_008', 'PR_0007', 'SKU_00007',
     496000.00, 'VND', 90, 22, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_008', 'CAMP_FLASH_DEMO', 'MER_009', 'PR_0008', 'SKU_00008',
     3250000.00, 'VND', 60, 18, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_009', 'CAMP_FLASH_DEMO', 'MER_010', 'PR_0009', 'SKU_00009',
     9750000.00, 'VND', 40, 7, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_010', 'CAMP_FLASH_DEMO', 'MER_011', 'PR_0010', 'SKU_00011',
     348000.00, 'VND', 110, 78, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_011', 'CAMP_FLASH_DEMO', 'MER_012', 'PR_0011', 'SKU_00012',
     182000.00, 'VND', 180, 95, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_012', 'CAMP_FLASH_DEMO', 'MER_013', 'PR_0012', 'SKU_00014',
     180000.00, 'VND', 100, 40, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_013', 'CAMP_FLASH_DEMO', 'MER_014', 'PR_0013', 'SKU_00015',
     195000.00, 'VND', 140, 33, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_014', 'CAMP_FLASH_DEMO', 'MER_015', 'PR_0014', 'SKU_00017',
     121000.00, 'VND', 220, 110, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_015', 'CAMP_FLASH_DEMO', 'MER_001', 'PR_0015', 'SKU_00018',
     14400000.00, 'VND', 25, 4, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_016', 'CAMP_FLASH_DEMO', 'MER_002', 'PR_0016', 'SKU_00019',
     507000.00, 'VND', 100, 50, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_017', 'CAMP_FLASH_DEMO', 'MER_003', 'PR_0017', 'SKU_00020',
     255000.00, 'VND', 130, 28, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_018', 'CAMP_FLASH_DEMO', 'MER_004', 'PR_0018', 'SKU_00023',
     480000.00, 'VND', 80, 17, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_019', 'CAMP_FLASH_DEMO', 'MER_005', 'PR_0019', 'SKU_00024',
     275000.00, 'VND', 150, 92, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_020', 'CAMP_FLASH_DEMO', 'MER_006', 'PR_0020', 'SKU_00026',
     20800000.00, 'VND', 20, 3, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_021', 'CAMP_FLASH_DEMO', 'MER_007', 'PR_0021', 'SKU_00027',
     120000.00, 'VND', 250, 200, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_022', 'CAMP_FLASH_DEMO', 'MER_008', 'PR_0022', 'SKU_00028',
     56000.00, 'VND', 200, 130, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_023', 'CAMP_FLASH_DEMO', 'MER_009', 'PR_0023', 'SKU_00029',
     11250000.00, 'VND', 30, 6, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_024', 'CAMP_FLASH_DEMO', 'MER_010', 'PR_0024', 'SKU_00030',
     3900000.00, 'VND', 40, 14, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_025', 'CAMP_FLASH_DEMO', 'MER_011', 'PR_0025', 'SKU_00032',
     4800000.00, 'VND', 35, 9, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_026', 'CAMP_FLASH_DEMO', 'MER_012', 'PR_0026', 'SKU_00034',
     352000.00, 'VND', 110, 70, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_027', 'CAMP_FLASH_DEMO', 'MER_013', 'PR_0027', 'SKU_00036',
     75000.00, 'VND', 300, 250, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_028', 'CAMP_FLASH_DEMO', 'MER_014', 'PR_0028', 'SKU_00037',
     532000.00, 'VND', 90, 25, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_029', 'CAMP_FLASH_DEMO', 'MER_015', 'PR_0029', 'SKU_00039',
     150000.00, 'VND', 220, 180, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_030', 'CAMP_FLASH_DEMO', 'MER_001', 'PR_0030', 'SKU_00041',
     416000.00, 'VND', 100, 55, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_031', 'CAMP_FLASH_DEMO', 'MER_002', 'PR_0031', 'SKU_00043',
     10400000.00, 'VND', 25, 5, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_032', 'CAMP_FLASH_DEMO', 'MER_003', 'PR_0032', 'SKU_00044',
     336000.00, 'VND', 95, 48, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_033', 'CAMP_FLASH_DEMO', 'MER_004', 'PR_0033', 'SKU_00045',
     99000.00, 'VND', 200, 165, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_034', 'CAMP_FLASH_DEMO', 'MER_005', 'PR_0034', 'SKU_00046',
     9750000.00, 'VND', 20, 4, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_035', 'CAMP_FLASH_DEMO', 'MER_006', 'PR_0035', 'SKU_00047',
     4900000.00, 'VND', 30, 11, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_036', 'CAMP_FLASH_DEMO', 'MER_007', 'PR_0036', 'SKU_00048',
     273000.00, 'VND', 80, 36, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_037', 'CAMP_FLASH_DEMO', 'MER_008', 'PR_0037', 'SKU_00050',
     310000.00, 'VND', 130, 88, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_038', 'CAMP_FLASH_DEMO', 'MER_009', 'PR_0038', 'SKU_00052',
     9750000.00, 'VND', 25, 7, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_039', 'CAMP_FLASH_DEMO', 'MER_010', 'PR_0039', 'SKU_00053',
     396000.00, 'VND', 90, 42, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0),
    ('FSR_DEMO_040', 'CAMP_FLASH_DEMO', 'MER_011', 'PR_0040', 'SKU_00054',
     14000000.00, 'VND', 15, 2, 'APPROVED',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 'sys_admin_01', NOW(), 0);

-- Issue 3 collectible vouchers for CAMP_FLASH_DEMO so the storefront
-- has real voucher cards to display on the Promotions page.
-- Discount tiers mirror Shopee-style platform voucher structure:
--   FLASH50K  — light discount, high usage limit (wide availability)
--   FLASH100K — mid discount, moderate limit
--   FLASH200K — heavy discount, scarce (VIP-feel)
INSERT INTO vouchers
    (voucher_code, campaign_id, discount_amount, currency,
     usage_limit, used_count, reserved_count,
     valid_from, valid_until,
     version, created_at, updated_at)
VALUES
    ('FLASH50K',  'CAMP_FLASH_DEMO', 50000.00,  'VND',
     500, 0, 0,
     NOW() - INTERVAL '1 hour', NOW() + INTERVAL '7 days',
     0, NOW(), NOW()),

    ('FLASH100K', 'CAMP_FLASH_DEMO', 100000.00, 'VND',
     300, 0, 0,
     NOW() - INTERVAL '1 hour', NOW() + INTERVAL '7 days',
     0, NOW(), NOW()),

    ('FLASH200K', 'CAMP_FLASH_DEMO', 200000.00, 'VND',
     100, 0, 0,
     NOW() - INTERVAL '1 hour', NOW() + INTERVAL '7 days',
     0, NOW(), NOW())
ON CONFLICT (voucher_code) DO NOTHING;

-- Platform Vouchers for CAMP_FLASH_DEMO
INSERT INTO vouchers
    (voucher_code, campaign_id, discount_amount, currency,
     usage_limit, used_count, reserved_count,
     valid_from, valid_until, version, created_at, updated_at, scope, merchant_id)
VALUES
    ('FLASH30K', 'CAMP_FLASH_DEMO', 30000.00, 'VND',
     600, 0, 0, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '7 days',
     0, NOW(), NOW(), 'PLATFORM', NULL),

    ('FLASH75K', 'CAMP_FLASH_DEMO', 75000.00, 'VND',
     400, 0, 0, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '7 days',
     0, NOW(), NOW(), 'PLATFORM', NULL)
ON CONFLICT (voucher_code) DO NOTHING;

-- Shop Vouchers for Merchants (MER_001 to MER_005)
INSERT INTO vouchers
    (voucher_code, campaign_id, discount_amount, currency,
     usage_limit, used_count, reserved_count,
     valid_from, valid_until, version, created_at, updated_at, scope, merchant_id)
VALUES
    ('TECH100K', NULL, 100000.00, 'VND',
     150, 0, 0, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '15 days',
     0, NOW(), NOW(), 'SHOP', 'MER_001'),
    ('TECH200K', NULL, 200000.00, 'VND',
     50, 0, 0, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '15 days',
     0, NOW(), NOW(), 'SHOP', 'MER_001'),

    ('APEX50K', NULL, 50000.00, 'VND',
     250, 0, 0, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '15 days',
     0, NOW(), NOW(), 'SHOP', 'MER_002'),
    ('APEX150K', NULL, 150000.00, 'VND',
     100, 0, 0, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '15 days',
     0, NOW(), NOW(), 'SHOP', 'MER_002'),

    ('NEXUS20K', NULL, 20000.00, 'VND',
     300, 0, 0, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '15 days',
     0, NOW(), NOW(), 'SHOP', 'MER_003'),
    ('NEXUS50K', NULL, 50000.00, 'VND',
     150, 0, 0, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '15 days',
     0, NOW(), NOW(), 'SHOP', 'MER_003'),

    ('VINA30K', NULL, 30000.00, 'VND',
     200, 0, 0, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '15 days',
     0, NOW(), NOW(), 'SHOP', 'MER_004'),

    ('FASHION10K', NULL, 10000.00, 'VND',
     500, 0, 0, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '15 days',
     0, NOW(), NOW(), 'SHOP', 'MER_005'),
    ('FASHION35K', NULL, 35000.00, 'VND',
     200, 0, 0, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '15 days',
     0, NOW(), NOW(), 'SHOP', 'MER_005')
ON CONFLICT (voucher_code) DO NOTHING;

UPDATE promotion_campaigns
SET    status     = 'RUNNING',
       updated_at = NOW()
WHERE  campaign_id = 'CAMP_MEGA'
  AND  status      = 'ACTIVE';

INSERT INTO vouchers
    (voucher_code, campaign_id, discount_amount, currency,
     usage_limit, used_count, reserved_count,
     valid_from, valid_until, version, created_at, updated_at, scope, merchant_id)
VALUES
    ('MEGA100', 'CAMP_MEGA', 100000.00, 'VND',
     800, 0, 0, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '30 days',
     0, NOW(), NOW(), 'PLATFORM', NULL),

    ('MEGA300', 'CAMP_MEGA', 300000.00, 'VND',
     200, 0, 0, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '30 days',
     0, NOW(), NOW(), 'PLATFORM', NULL),

    ('AIONNNEW', 'CAMP_MEGA', 50000.00, 'VND',
     1000, 0, 0, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '30 days',
     0, NOW(), NOW(), 'PLATFORM', NULL),

    ('FREESHIP50', 'CAMP_MEGA', 50000.00, 'VND',
     1500, 0, 0, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '30 days',
     0, NOW(), NOW(), 'PLATFORM', NULL)
ON CONFLICT (voucher_code) DO NOTHING;

INSERT INTO user_vouchers (
    user_voucher_id, voucher_code, user_id, status,
    claimed_at, version, updated_at
) VALUES
('UV_DEMO_001', 'FLASH50K', '01KV05RTC7NA4KZMMP8KXX7461', 'CLAIMED', NOW() - INTERVAL '2 days', 0, NOW() - INTERVAL '2 days'),
('UV_DEMO_002', 'FLASH100K', '01KV05RTC836YVQ3DF9FZFT2A7', 'CLAIMED', NOW() - INTERVAL '1 day', 0, NOW() - INTERVAL '1 day'),
('UV_DEMO_003', 'MEGA100', '01KV05RTC9AYX7XR183X8EQBF0', 'CLAIMED', NOW() - INTERVAL '8 hours', 0, NOW() - INTERVAL '8 hours'),
('UV_DEMO_004', 'AIONNNEW', '01KV05RTCATYB3M97J3TJ1NEAP', 'CLAIMED', NOW() - INTERVAL '4 hours', 0, NOW() - INTERVAL '4 hours'),
('UV_DEMO_005', 'FREESHIP50', '01KV05RTCBBPV0GBS7DFTEHY1T', 'CLAIMED', NOW() - INTERVAL '1 hour', 0, NOW() - INTERVAL '1 hour');
