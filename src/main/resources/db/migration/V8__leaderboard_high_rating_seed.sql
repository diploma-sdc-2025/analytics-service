-- Leaderboard pool comes from gameplay_events; ordering into the pool uses player_statistics.current_rating.
-- Seed events + stats for demo user_ids 3-10 (auth test_data refresh tokens) and bump 1-2 to match high ratings.

INSERT INTO gameplay_events (time, event_type, user_id, match_id, service, metadata)
VALUES (CURRENT_TIMESTAMP - INTERVAL '45 minutes', 'player_join', 3, NULL, 'matchmaking-service', '{}'),
       (CURRENT_TIMESTAMP - INTERVAL '44 minutes', 'player_join', 4, NULL, 'matchmaking-service', '{}'),
       (CURRENT_TIMESTAMP - INTERVAL '43 minutes', 'player_join', 5, NULL, 'matchmaking-service', '{}'),
       (CURRENT_TIMESTAMP - INTERVAL '42 minutes', 'player_join', 6, NULL, 'matchmaking-service', '{}'),
       (CURRENT_TIMESTAMP - INTERVAL '41 minutes', 'player_join', 7, NULL, 'matchmaking-service', '{}'),
       (CURRENT_TIMESTAMP - INTERVAL '40 minutes', 'player_join', 8, NULL, 'matchmaking-service', '{}'),
       (CURRENT_TIMESTAMP - INTERVAL '39 minutes', 'player_join', 9, NULL, 'matchmaking-service', '{}'),
       (CURRENT_TIMESTAMP - INTERVAL '38 minutes', 'player_join', 10, NULL, 'matchmaking-service', '{}');

UPDATE player_statistics
SET current_rating = CASE user_id
                        WHEN 1 THEN 2540
                        WHEN 2 THEN 2525
                        ELSE current_rating
    END,
    last_updated   = CURRENT_TIMESTAMP
WHERE user_id IN (1, 2);

INSERT INTO player_statistics (user_id,
                               total_matches_played,
                               total_matches_won,
                               total_battles_fought,
                               total_pieces_purchased,
                               total_gold_spent,
                               avg_match_duration_seconds,
                               highest_round_reached,
                               win_rate,
                               last_updated,
                               current_rating)
VALUES (3, 28, 17, 84, 70, 180, 495.5, 12, 60.71, CURRENT_TIMESTAMP, 2510),
       (4, 22, 13, 66, 55, 140, 480.0, 11, 59.09, CURRENT_TIMESTAMP, 2498),
       (5, 35, 20, 105, 90, 220, 520.3, 13, 57.14, CURRENT_TIMESTAMP, 2485),
       (6, 19, 12, 57, 48, 120, 455.2, 10, 63.16, CURRENT_TIMESTAMP, 2533),
       (7, 31, 18, 93, 75, 195, 505.8, 12, 58.06, CURRENT_TIMESTAMP, 2507),
       (8, 26, 14, 78, 62, 160, 470.4, 9, 53.85, CURRENT_TIMESTAMP, 2472),
       (9, 40, 25, 120, 100, 260, 540.0, 14, 62.50, CURRENT_TIMESTAMP, 2560),
       (10, 18, 10, 54, 44, 110, 445.0, 8, 55.56, CURRENT_TIMESTAMP, 2455)
ON CONFLICT (user_id) DO UPDATE SET
    total_matches_played       = EXCLUDED.total_matches_played,
    total_matches_won          = EXCLUDED.total_matches_won,
    total_battles_fought       = EXCLUDED.total_battles_fought,
    total_pieces_purchased     = EXCLUDED.total_pieces_purchased,
    total_gold_spent           = EXCLUDED.total_gold_spent,
    avg_match_duration_seconds = EXCLUDED.avg_match_duration_seconds,
    highest_round_reached      = EXCLUDED.highest_round_reached,
    win_rate                   = EXCLUDED.win_rate,
    last_updated               = EXCLUDED.last_updated,
    current_rating             = EXCLUDED.current_rating;
