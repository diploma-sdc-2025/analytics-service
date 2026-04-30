-- Keep original V5 checksum stable; remap demo user ids in a new migration.
-- This allows Flyway to continue on environments where V5 already ran.

UPDATE gameplay_events
SET user_id = 1
WHERE user_id = 101;

UPDATE gameplay_events
SET user_id = 2
WHERE user_id = 102;

UPDATE player_statistics
SET user_id = 1
WHERE user_id = 101;

UPDATE player_statistics
SET user_id = 2
WHERE user_id = 102;
