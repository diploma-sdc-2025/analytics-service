-- Seed analytics test data for demo and edge-case checks.
-- Idempotent inserts using ON CONFLICT where unique keys exist.

INSERT INTO gameplay_events (time, event_type, user_id, match_id, service, metadata)
VALUES
  (CURRENT_TIMESTAMP - INTERVAL '10 minutes', 'match_created', 101, 1001, 'game-service', '{"mode":"1v1"}'),
  (CURRENT_TIMESTAMP - INTERVAL '9 minutes', 'queue_join', 101, 1001, 'matchmaking-service', '{"queue":"default"}'),
  (CURRENT_TIMESTAMP - INTERVAL '8 minutes', 'piece_bought', 101, 1001, 'game-service', '{"piece":"pawn","cost":1}'),
  (CURRENT_TIMESTAMP - INTERVAL '7 minutes', 'battle_evaluated', 101, 1001, 'battle-service', '{"centipawns":120}'),
  (CURRENT_TIMESTAMP - INTERVAL '6 minutes', 'queue_leave', 102, NULL, 'matchmaking-service', '{"reason":"matched"}');

INSERT INTO service_health_metrics (time, service, metric_name, metric_value, unit)
VALUES
  (CURRENT_TIMESTAMP - INTERVAL '5 minutes', 'game-service', 'response_time_ms', 42.5, 'ms'),
  (CURRENT_TIMESTAMP - INTERVAL '4 minutes', 'battle-service', 'response_time_ms', 88.0, 'ms'),
  (CURRENT_TIMESTAMP - INTERVAL '3 minutes', 'matchmaking-service', 'queue_size', 2, 'count'),
  (CURRENT_TIMESTAMP - INTERVAL '2 minutes', 'analytics-service', 'events_per_minute', 18, 'count');

INSERT INTO player_statistics (
  user_id,
  total_matches_played,
  total_matches_won,
  total_battles_fought,
  total_pieces_purchased,
  total_gold_spent,
  avg_match_duration_seconds,
  highest_round_reached,
  win_rate,
  last_updated
)
VALUES
  (1, 12, 7, 36, 58, 143, 512.4, 11, 58.33, CURRENT_TIMESTAMP),
  (2, 5, 1, 14, 19, 47, 438.8, 7, 20.00, CURRENT_TIMESTAMP)
ON CONFLICT (user_id) DO UPDATE SET
  total_matches_played = EXCLUDED.total_matches_played,
  total_matches_won = EXCLUDED.total_matches_won,
  total_battles_fought = EXCLUDED.total_battles_fought,
  total_pieces_purchased = EXCLUDED.total_pieces_purchased,
  total_gold_spent = EXCLUDED.total_gold_spent,
  avg_match_duration_seconds = EXCLUDED.avg_match_duration_seconds,
  highest_round_reached = EXCLUDED.highest_round_reached,
  win_rate = EXCLUDED.win_rate,
  last_updated = EXCLUDED.last_updated;

INSERT INTO piece_statistics (
  piece_id,
  times_purchased,
  times_in_winning_team,
  total_damage_dealt,
  total_kills,
  avg_survival_time_seconds,
  win_rate_when_used,
  last_updated
)
VALUES
  (1, 41, 21, 1390, 44, 31.5, 51.22, CURRENT_TIMESTAMP),
  (2, 17, 11, 920, 19, 28.2, 64.70, CURRENT_TIMESTAMP)
ON CONFLICT (piece_id) DO UPDATE SET
  times_purchased = EXCLUDED.times_purchased,
  times_in_winning_team = EXCLUDED.times_in_winning_team,
  total_damage_dealt = EXCLUDED.total_damage_dealt,
  total_kills = EXCLUDED.total_kills,
  avg_survival_time_seconds = EXCLUDED.avg_survival_time_seconds,
  win_rate_when_used = EXCLUDED.win_rate_when_used,
  last_updated = EXCLUDED.last_updated;
