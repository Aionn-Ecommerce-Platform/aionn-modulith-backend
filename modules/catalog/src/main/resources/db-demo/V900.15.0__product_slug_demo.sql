UPDATE products
   SET slug = left(
       trim(both '-' FROM regexp_replace(
           regexp_replace(
               unaccent(lower(replace(name, 'đ', 'd'))),
               '[^a-z0-9]+', '-', 'g'),
           '-+', '-', 'g')),
       250)
 WHERE slug IS NULL;
