-- Erasure marks.
--
-- Deleting an account removes this module's interactions and affinity profile, but two things can put
-- them back afterwards: the profile refresh sweep, which reads its batch of user IDs before processing
-- them and so can refresh an account erased in between, and at-least-once outbox delivery, which can
-- replay a view or purchase event for an account that no longer exists. Neither is stopped by the
-- deletes themselves, because there is nothing left to distinguish "erased" from "never had any
-- behavioural data".
--
-- A mark is the bare user identifier and the time erasure was honoured. It holds no behavioural data
-- and is retained indefinitely: pruning it would reopen the leak it exists to close, and at one row
-- per deleted account it is bounded by account deletions rather than by activity.
CREATE TABLE recommendation_erased_users (
    user_id   VARCHAR(50) PRIMARY KEY,
    erased_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
