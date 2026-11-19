-- Keep the shorthand province rows hidden from new address pickers, but leave the
-- ward/district rows active so already-saved addresses that reference them remain
-- editable (address updates resolve wards through an active-only lookup).
UPDATE wards
SET active = TRUE
WHERE district_code IN (
    SELECT code FROM districts
    WHERE province_code IN ('VN-HN', 'VN-SG', 'VN-HP', 'VN-DN', 'VN-CT')
);

UPDATE districts
SET active = TRUE
WHERE province_code IN ('VN-HN', 'VN-SG', 'VN-HP', 'VN-DN', 'VN-CT');
