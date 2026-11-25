-- Presence and last-seen timestamps are intentionally not part of Connect.
ALTER TABLE students DROP COLUMN IF EXISTS last_login_at;
