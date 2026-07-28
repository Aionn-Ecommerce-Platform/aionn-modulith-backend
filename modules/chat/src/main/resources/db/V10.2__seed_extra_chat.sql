-- ============================================================================
-- V10.2 — Seed chat conversations + messages so merchant inbox has activity.
-- Pairs random buyers with each merchant and writes a 3-5 message thread.
-- Skip if conversation already exists (unique on buyer_id, merchant_id).
-- ============================================================================

DO $$
DECLARE
    merchants TEXT[] := ARRAY['MER_001','MER_002','MER_003','MER_004','MER_005','MER_006','MER_007','MER_008','MER_009','MER_010','MER_011','MER_012','MER_013','MER_014','MER_015'];
    buyers TEXT[];
    v_merchant_id TEXT;
    v_buyer_id TEXT;
    new_conv_id TEXT;
    new_msg_id TEXT;
    last_preview TEXT;
    last_sender TEXT;
    last_at TIMESTAMPTZ;
    conv_seq INT := 1000;
    msg_seq INT := 1000;
    i INT;
    j INT;
    n_msgs INT;
    msg_time TIMESTAMPTZ;
    msg_body TEXT;
    msg_sender TEXT;
    msg_role TEXT;
    buyer_lines TEXT[] := ARRAY[
        'Shop ơi sản phẩm còn không ạ?',
        'Mình muốn đặt 2 cái, ship Hà Nội bao lâu?',
        'Có size lớn hơn không shop?',
        'Cho mình xin thêm ảnh thực tế với',
        'Bên mình bảo hành bao lâu vậy?',
        'Ok mình chốt đơn, cảm ơn shop nhé!'
    ];
    seller_lines TEXT[] := ARRAY[
        'Dạ shop còn hàng nhé, anh/chị đặt giúp ạ.',
        'Ship HN khoảng 1-2 ngày, sẽ có ngay khi đặt.',
        'Dạ shop có nhiều size, anh/chị xem chi tiết ở phần biến thể nhé.',
        'Em gửi thêm ảnh ngay, anh/chị check tin nhắn tiếp theo nhé.',
        'Bảo hành chính hãng 12 tháng tại các trung tâm nhé.',
        'Em cảm ơn anh/chị, đơn sẽ được xử lý trong hôm nay ạ!'
    ];
BEGIN
    SELECT array_agg(DISTINCT user_id) INTO buyers FROM orders;
    IF buyers IS NULL OR array_length(buyers, 1) IS NULL THEN
        RAISE NOTICE 'No buyers found; skipping chat seed.';
        RETURN;
    END IF;

    FOREACH v_merchant_id IN ARRAY merchants LOOP
        -- 6 conversations per merchant
        FOR i IN 1..6 LOOP
            v_buyer_id := buyers[1 + (random() * (array_length(buyers,1) - 1))::int];
            conv_seq := conv_seq + 1;
            new_conv_id := 'CONV_S' || lpad(conv_seq::text, 5, '0');

            -- Skip if same buyer↔merchant pair already has a conversation
            IF EXISTS (
                SELECT 1 FROM chat_conversations c
                 WHERE c.buyer_id = v_buyer_id AND c.merchant_id = v_merchant_id
            ) THEN
                CONTINUE;
            END IF;

            n_msgs := 3 + (random() * 3)::int;
            msg_time := NOW() - (random() * INTERVAL '10 days');
            last_preview := NULL;
            last_sender := NULL;
            last_at := NULL;

            INSERT INTO chat_conversations (
                conversation_id, buyer_id, merchant_id, participants,
                is_archived, version, created_at, updated_at
            ) VALUES (
                new_conv_id, v_buyer_id, v_merchant_id,
                jsonb_build_array(v_buyer_id, v_merchant_id),
                FALSE, 0, msg_time, msg_time
            );

            FOR j IN 1..n_msgs LOOP
                msg_seq := msg_seq + 1;
                new_msg_id := 'MSG_S' || lpad(msg_seq::text, 6, '0');

                IF j % 2 = 1 THEN
                    msg_role := 'BUYER';
                    msg_sender := v_buyer_id;
                    msg_body := buyer_lines[1 + (random() * (array_length(buyer_lines,1) - 1))::int];
                ELSE
                    msg_role := 'MERCHANT';
                    msg_sender := v_merchant_id;
                    msg_body := seller_lines[1 + (random() * (array_length(seller_lines,1) - 1))::int];
                END IF;

                msg_time := msg_time + (5 + random() * 120) * INTERVAL '1 minute';

                INSERT INTO chat_messages (
                    message_id, conversation_id, sender_id, sender_role,
                    type, body, status, delivered_to, read_by,
                    is_recalled, version, sent_at, updated_at
                ) VALUES (
                    new_msg_id, new_conv_id, msg_sender, msg_role,
                    'TEXT', msg_body, 'READ',
                    jsonb_build_array(CASE WHEN msg_role = 'BUYER' THEN v_merchant_id ELSE v_buyer_id END),
                    jsonb_build_array(CASE WHEN msg_role = 'BUYER' THEN v_merchant_id ELSE v_buyer_id END),
                    FALSE, 0, msg_time, msg_time
                );

                last_preview := msg_body;
                last_sender := msg_sender;
                last_at := msg_time;
            END LOOP;

            UPDATE chat_conversations
               SET last_message_id = new_msg_id,
                   last_message_preview = last_preview,
                   last_message_type = 'TEXT',
                   last_message_sender_id = last_sender,
                   last_message_at = last_at,
                   updated_at = last_at
             WHERE conversation_id = new_conv_id;
        END LOOP;
    END LOOP;
END $$;
