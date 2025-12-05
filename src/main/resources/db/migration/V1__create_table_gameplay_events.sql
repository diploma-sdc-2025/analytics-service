CREATE TABLE gameplay_events (
                                 time TIMESTAMP NOT NULL,
                                 event_type VARCHAR(50) NOT NULL,
                                 user_id BIGINT,
                                 match_id BIGINT,
                                 service VARCHAR(50) NOT NULL,
                                 metadata JSONB,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT chk_gameplay_events_event_type CHECK (
                                     event_type IN (
                                                    'MATCH_STARTED', 'MATCH_ENDED', 'ROUND_STARTED', 'ROUND_ENDED',
                                                    'PIECE_PURCHASED', 'PIECE_PLACED', 'PIECE_UPGRADED', 'PIECE_SOLD',
                                                    'SHOP_REFRESHED', 'LEVEL_UP', 'DAMAGE_DEALT', 'DAMAGE_TAKEN',
                                                    'PLAYER_ELIMINATED', 'VICTORY', 'DEFEAT'
                                         )
                                     ),
                                 CONSTRAINT chk_gameplay_events_service CHECK (
                                     service IN ('GAME_SERVICE', 'MATCH_SERVICE', 'SHOP_SERVICE', 'AUTH_SERVICE', 'LEADERBOARD_SERVICE')
                                     )
);

CREATE INDEX idx_gameplay_events_time ON gameplay_events(time DESC);
CREATE INDEX idx_gameplay_events_user_id ON gameplay_events(user_id, time DESC);
CREATE INDEX idx_gameplay_events_match_id ON gameplay_events(match_id);
CREATE INDEX idx_gameplay_events_event_type ON gameplay_events(event_type, time DESC);
CREATE INDEX idx_gameplay_events_service ON gameplay_events(service, time DESC);

CREATE INDEX idx_gameplay_events_metadata ON gameplay_events USING GIN (metadata);

CREATE INDEX idx_gameplay_events_user_event ON gameplay_events(user_id, event_type, time DESC);
