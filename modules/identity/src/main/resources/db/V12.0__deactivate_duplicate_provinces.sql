-- The identity schema seeds both the official 63 province codes (01..96) and
-- five shorthand duplicates for the centrally-run cities (VN-HN, VN-SG, ...).
-- Only the official rows are used by merchants and addresses, so deactivate the
-- duplicates to keep the public geography list at the standard 63 units.
UPDATE wards
SET active = FALSE
WHERE district_code IN (
    SELECT code FROM districts
    WHERE province_code IN ('VN-HN', 'VN-SG', 'VN-HP', 'VN-DN', 'VN-CT')
);

UPDATE districts
SET active = FALSE
WHERE province_code IN ('VN-HN', 'VN-SG', 'VN-HP', 'VN-DN', 'VN-CT');

UPDATE provinces
SET active = FALSE
WHERE code IN ('VN-HN', 'VN-SG', 'VN-HP', 'VN-DN', 'VN-CT');
