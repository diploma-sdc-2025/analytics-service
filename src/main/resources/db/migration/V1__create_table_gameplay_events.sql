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
