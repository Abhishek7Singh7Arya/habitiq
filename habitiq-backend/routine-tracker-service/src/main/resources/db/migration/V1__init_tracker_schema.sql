CREATE TABLE tracking_sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    routine_id VARCHAR(36) NOT NULL,
    start_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE scheduled_tasks (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL REFERENCES tracking_sessions(id) ON DELETE CASCADE,
    routine_task_id VARCHAR(36) NOT NULL,
    task_description VARCHAR(1000) NOT NULL,
    task_type VARCHAR(100) NOT NULL,
    user_phone VARCHAR(100) NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    completed_at TIMESTAMP,
    notified_at TIMESTAMP,
    reminded_at TIMESTAMP,
    call_placed_at TIMESTAMP
);
