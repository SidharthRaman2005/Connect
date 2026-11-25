-- Existing databases created before lifecycle statuses were introduced only
-- allowed OPEN and CLOSED. Allow the completion and expiry states as well.
ALTER TABLE collab_posts
    DROP CONSTRAINT IF EXISTS collab_posts_status_check;

ALTER TABLE collab_posts
    ADD CONSTRAINT collab_posts_status_check
    CHECK (status IN ('OPEN', 'COMPLETED', 'EXPIRED', 'CLOSED'));
