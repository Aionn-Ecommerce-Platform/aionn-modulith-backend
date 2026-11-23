ALTER TABLE products
    ADD COLUMN IF NOT EXISTS description TEXT;

ALTER TABLE product_translations
    ADD COLUMN IF NOT EXISTS description TEXT;

-- Existing rows only had the shared ai_description field. Keep old products
-- readable on the storefront until merchants fill in the new description.
UPDATE products
   SET description = ai_description
 WHERE description IS NULL
   AND ai_description IS NOT NULL;

UPDATE product_translations
   SET description = ai_description
 WHERE description IS NULL
   AND ai_description IS NOT NULL;
