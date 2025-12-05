CREATE TABLE service_health_metrics (
    time TIMESTAMP NOT NULL,
    service VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value NUMERIC NOT NULL,
    unit VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_service_health_service CHECK (
        service IN ('GAME_SERVICE', 'MATCH_SERVICE', 'SHOP_SERVICE', 'AUTH_SERVICE', 'LEADERBOARD_SERVICE')
    ),
    CONSTRAINT chk_service_health_metric CHECK (
        metric_name IN (
            'CPU_USAGE', 'MEMORY_USAGE', 'RESPONSE_TIME_MS', 'ERROR_RATE',
            'REQUEST_COUNT', 'ACTIVE_CONNECTIONS', 'DB_QUERY_TIME_MS',
            'CACHE_HIT_RATE', 'QUEUE_SIZE', 'THROUGHPUT'
        )
    ),
    CONSTRAINT chk_service_health_unit CHECK (
        unit IN ('PERCENT', 'MILLISECONDS', 'COUNT', 'BYTES', 'RATIO', NULL)
    )
);

CREATE INDEX idx_service_health_time ON service_health_metrics(time DESC);
CREATE INDEX idx_service_health_service ON service_health_metrics(service, time DESC);
CREATE INDEX idx_service_health_metric ON service_health_metrics(metric_name, time DESC);
CREATE INDEX idx_service_health_service_metric ON service_health_metrics(service, metric_name, time DESC);
