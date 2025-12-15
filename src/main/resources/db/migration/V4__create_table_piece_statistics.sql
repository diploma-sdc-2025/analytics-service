CREATE TABLE piece_statistics (
                                  id SERIAL PRIMARY KEY,
                                  piece_id INT NOT NULL UNIQUE,
                                  times_purchased INT NOT NULL DEFAULT 0,
                                  times_in_winning_team INT NOT NULL DEFAULT 0,
                                  total_damage_dealt BIGINT NOT NULL DEFAULT 0,
                                  total_kills INT NOT NULL DEFAULT 0,
                                  avg_survival_time_seconds NUMERIC(10,2) NOT NULL DEFAULT 0,
                                  win_rate_when_used NUMERIC(5,2) NOT NULL DEFAULT 0,
                                  last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_piece_statistics_piece_id ON piece_statistics(piece_id);
CREATE INDEX idx_piece_statistics_times_purchased ON piece_statistics(times_purchased DESC);
CREATE INDEX idx_piece_statistics_win_rate ON piece_statistics(win_rate_when_used DESC);
