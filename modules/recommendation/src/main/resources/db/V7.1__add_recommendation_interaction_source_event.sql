-- Idempotent interaction ingest.
--
-- Outbox delivery is at-least-once: the consumer inbox records one receipt per listener, but the
-- ingest listener commits in its own transaction (REQUIRES_NEW) nested inside the inbox aspect's
-- transaction, so a lost receipt allows the same event to be applied twice. Without a database-level
-- guard the duplicate rows are indistinguishable from genuine repeat behaviour, and because
-- popularity and affinity both sum over this log every duplicate permanently inflates the scores it
-- touches.
--
-- The identifier is nullable. Rows written by anything other than an event - seeded fixtures, a
-- manual backfill - have no originating event, and the partial index below leaves them unconstrained
-- so this migration succeeds against a table that already holds data.
ALTER TABLE recommendation_interactions
    ADD COLUMN source_event_id VARCHAR(100);

-- One signal per (event, user, product, type). An order carrying two lines for the same product
-- reaches the listener as one event that produces two rows, and collapses to a single purchase signal
-- here. That matches the rule already applied to quantity: needing five of something is not liking it
-- five times more.
--
-- Leading on source_event_id also serves a lookup by event alone - what a dead-letter replay already
-- wrote - so no second index is needed.
CREATE UNIQUE INDEX uq_rec_interactions_source_event
    ON recommendation_interactions (source_event_id, user_id, product_id, interaction_type)
    WHERE source_event_id IS NOT NULL;
