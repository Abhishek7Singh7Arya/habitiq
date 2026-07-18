CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    scheduled_task_id VARCHAR(36) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    attempt INTEGER NOT NULL,
    external_id VARCHAR(255),
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE notification_logs (
    id BIGSERIAL PRIMARY KEY,
    channel VARCHAR(50) NOT NULL,
    direction VARCHAR(50) NOT NULL,
    from_phone VARCHAR(100) NOT NULL,
    raw_payload TEXT NOT NULL,
    scheduled_task_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL
);
