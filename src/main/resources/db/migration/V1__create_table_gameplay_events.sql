CREATE TABLE gameplay_events (
                                 id BIGSERIAL PRIMARY KEY,
                                 time TIMESTAMP NOT NULL,
                                 event_type VARCHAR(50) NOT NULL,
                                 user_id BIGINT,
                                 match_id BIGINT,
                                 service VARCHAR(50) NOT NULL,
                                 metadata TEXT,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_gameplay_events_time ON gameplay_events(time DESC);
CREATE INDEX idx_gameplay_events_user_id ON gameplay_events(user_id, time DESC);
CREATE INDEX idx_gameplay_events_match_id ON gameplay_events(match_id);
CREATE INDEX idx_gameplay_events_event_type ON gameplay_events(event_type, time DESC);
CREATE INDEX idx_gameplay_events_service ON gameplay_events(service, time DESC);
CREATE INDEX idx_gameplay_events_user_event ON gameplay_events(user_id, event_type, time DESC);
