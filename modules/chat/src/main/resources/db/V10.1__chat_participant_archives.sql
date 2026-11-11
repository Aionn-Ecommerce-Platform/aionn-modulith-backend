ALTER TABLE chat_conversations
    ADD COLUMN archived_participants JSONB NOT NULL DEFAULT '[]'::jsonb;

UPDATE chat_conversations
   SET archived_participants = jsonb_build_array(buyer_id, merchant_id)
 WHERE is_archived = TRUE;
