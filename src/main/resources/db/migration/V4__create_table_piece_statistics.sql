CREATE TABLE piece_statistics (
                                  id SERIAL PRIMARY KEY,
                                  piece_id INT NOT NULL UNIQUE,
                                  times_purchased INT NOT NULL DEFAULT 0,
                                  times_in_winning_team INT NOT NULL DEFAULT 0,
                                  total_damage_dealt BIGINT NOT NULL DEFAULT 0,
                                  total_kills INT NOT NULL DEFAULT 0,
                                  avg_survival_time_seconds NUMERIC(10,2) NOT NULL DEFAULT 0,
                                  win_rate_when_used NUMERIC(5,2) NOT NULL DEFAULT 0,
                                  last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT chk_piece_stats_purchased CHECK (times_purchased >= 0),
                                  CONSTRAINT chk_piece_stats_winning CHECK (times_in_winning_team >= 0),
                                  CONSTRAINT chk_piece_stats_winning_vs_purchased CHECK (times_in_winning_team <= times_purchased),
                                  CONSTRAINT chk_piece_stats_damage CHECK (total_damage_dealt >= 0),
                                  CONSTRAINT chk_piece_stats_kills CHECK (total_kills >= 0),
                                  CONSTRAINT chk_piece_stats_survival CHECK (avg_survival_time_seconds >= 0),
                                  CONSTRAINT chk_piece_stats_win_rate CHECK (win_rate_when_used >= 0 AND win_rate_when_used <= 100)
);

CREATE INDEX idx_piece_statistics_piece_id ON piece_statistics(piece_id);
CREATE INDEX idx_piece_statistics_times_purchased ON piece_statistics(times_purchased DESC);
CREATE INDEX idx_piece_statistics_win_rate ON piece_statistics(win_rate_when_used DESC);
CREATE INDEX idx_piece_statistics_damage ON piece_statistics(total_damage_dealt DESC);
CREATE INDEX idx_piece_statistics_last_updated ON piece_statistics(last_updated DESC);

CREATE INDEX idx_piece_statistics_balance ON piece_statistics(win_rate_when_used DESC, times_purchased DESC);
