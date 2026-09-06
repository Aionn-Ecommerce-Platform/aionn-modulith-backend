-- Demo behavioural data for the recommendation module.
--
-- Interactions are normally produced by integration events as users browse and buy. Seeding them
-- directly gives a freshly reset environment something to recommend from, instead of an empty home
-- feed until someone clicks around.
--
-- The generated pattern is deliberately structured rather than uniformly random: each demo buyer is
-- assigned two preferred categories and only interacts within them. Without that structure,
-- collaborative filtering finds no co-occurrence and content affinities come out flat, so the demo
-- would exercise the plumbing while telling you nothing about the ranking.

DO $$
DECLARE
    buyer            RECORD;
    product          RECORD;
    buyer_index      INTEGER := 0;
    preferred        TEXT[];
    all_categories   TEXT[] := ARRAY[
        'CAT_ELE', 'CAT_FAS', 'CAT_HBE', 'CAT_HOK', 'CAT_SMH'];
    view_count       INTEGER;
    days_ago         NUMERIC;
BEGIN
    FOR buyer IN
        SELECT user_id FROM users WHERE username LIKE 'buyer_%' ORDER BY username LIMIT 40
    LOOP
        buyer_index := buyer_index + 1;

        -- Two adjacent categories per buyer, rotating through the list, so buyers overlap partially.
        -- Complete overlap would make every recommendation identical; none would leave the item-item
        -- matrix empty.
        preferred := ARRAY[
            all_categories[(buyer_index % array_length(all_categories, 1)) + 1],
            all_categories[((buyer_index + 1) % array_length(all_categories, 1)) + 1]];

        view_count := 0;

        FOR product IN
            SELECT p.product_id
            FROM products p
            WHERE p.status = 'PUBLISHED'
              AND EXISTS (
                  SELECT 1 FROM jsonb_array_elements_text(p.category_ids) cat
                  WHERE cat = ANY(preferred))
            ORDER BY md5(p.product_id || buyer.user_id)
            LIMIT 12
        LOOP
            view_count := view_count + 1;
            days_ago := (view_count * 1.7)::NUMERIC;

            -- Views: the broad, low-weight signal.
            INSERT INTO recommendation_interactions
                (interaction_id, user_id, product_id, interaction_type, weight, occurred_at, created_at)
            VALUES (
                'RIN_' || lpad(buyer_index::TEXT, 3, '0') || '_V' || lpad(view_count::TEXT, 2, '0'),
                buyer.user_id, product.product_id, 'VIEW', 1.00,
                NOW() - (days_ago || ' days')::INTERVAL, NOW());

            -- Roughly every third viewed product reaches the cart, and every sixth is bought. This
            -- funnel shape is what gives the item-item matrix pairs to work with: buyers who share a
            -- category end up sharing purchases.
            IF view_count % 3 = 0 THEN
                INSERT INTO recommendation_interactions
                    (interaction_id, user_id, product_id, interaction_type, weight, occurred_at, created_at)
                VALUES (
                    'RIN_' || lpad(buyer_index::TEXT, 3, '0') || '_C' || lpad(view_count::TEXT, 2, '0'),
                    buyer.user_id, product.product_id, 'CART_ADD', 4.00,
                    NOW() - (days_ago || ' days')::INTERVAL + INTERVAL '5 minutes', NOW());
            END IF;

            IF view_count % 6 = 0 THEN
                INSERT INTO recommendation_interactions
                    (interaction_id, user_id, product_id, interaction_type, weight, occurred_at, created_at)
                VALUES (
                    'RIN_' || lpad(buyer_index::TEXT, 3, '0') || '_P' || lpad(view_count::TEXT, 2, '0'),
                    buyer.user_id, product.product_id, 'PURCHASE', 5.00,
                    NOW() - (days_ago || ' days')::INTERVAL + INTERVAL '15 minutes', NOW());
            END IF;
        END LOOP;
    END LOOP;
END $$;

-- Popularity and item similarity are left for the schedulers to compute on first run: seeding them by
-- hand would diverge from what the real jobs produce, and the difference would be invisible until
-- someone trusted the wrong numbers.
