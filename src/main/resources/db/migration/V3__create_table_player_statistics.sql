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
                                   last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT chk_player_stats_matches_played CHECK (total_matches_played >= 0),
                                   CONSTRAINT chk_player_stats_matches_won CHECK (total_matches_won >= 0),
                                   CONSTRAINT chk_player_stats_won_vs_played CHECK (total_matches_won <= total_matches_played),
                                   CONSTRAINT chk_player_stats_battles CHECK (total_battles_fought >= 0),
                                   CONSTRAINT chk_player_stats_pieces CHECK (total_pieces_purchased >= 0),
                                   CONSTRAINT chk_player_stats_gold CHECK (total_gold_spent >= 0),
                                   CONSTRAINT chk_player_stats_avg_duration CHECK (avg_match_duration_seconds >= 0),
                                   CONSTRAINT chk_player_stats_highest_round CHECK (highest_round_reached >= 0),
                                   CONSTRAINT chk_player_stats_win_rate CHECK (win_rate >= 0 AND win_rate <= 100)
);

CREATE INDEX idx_player_statistics_user_id ON player_statistics(user_id);
CREATE INDEX idx_player_statistics_win_rate ON player_statistics(win_rate DESC);
CREATE INDEX idx_player_statistics_matches_played ON player_statistics(total_matches_played DESC);
CREATE INDEX idx_player_statistics_last_updated ON player_statistics(last_updated DESC);

CREATE INDEX idx_player_statistics_leaderboard ON player_statistics(total_matches_won DESC, win_rate DESC);
