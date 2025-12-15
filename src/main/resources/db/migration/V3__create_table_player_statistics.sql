CREATE TABLE player_statistics (
                                   id SERIAL PRIMARY KEY,
                                   user_id BIGINT NOT NULL UNIQUE,
                                   total_matches_played INT NOT NULL DEFAULT 0,
                                   total_matches_won INT NOT NULL DEFAULT 0,
                                   total_battles_fought INT NOT NULL DEFAULT 0,
                                   total_pieces_purchased INT NOT NULL DEFAULT 0,
                                   total_gold_spent INT NOT NULL DEFAULT 0,
                                   avg_match_duration_seconds NUMERIC(10,2) NOT NULL DEFAULT 0,
                                   highest_round_reached INT NOT NULL DEFAULT 0,
                                   win_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
                                   last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_player_statistics_user_id ON player_statistics(user_id);
