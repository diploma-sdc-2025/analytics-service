docker compose logs -f analytics-serviceALTER TABLE player_statistics
ADD COLUMN IF NOT EXISTS current_rating INT NOT NULL DEFAULT 1000;

-- Demo ratings for seeded players.
UPDATE player_statistics
SET current_rating = CASE
    WHEN user_id = 1 THEN 1240
    WHEN user_id = 2 THEN 1095
    ELSE current_rating
END
WHERE user_id IN (1, 2);
