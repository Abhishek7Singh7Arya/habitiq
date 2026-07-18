CREATE TABLE goals (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    goal_type VARCHAR(255) NOT NULL,
    target_value DOUBLE PRECISION NOT NULL,
    current_value DOUBLE PRECISION,
    unit VARCHAR(50) NOT NULL,
    target_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE progress_logs (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    goal_id VARCHAR(36) NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    logged_value DOUBLE PRECISION NOT NULL,
    unit VARCHAR(50) NOT NULL,
    notes TEXT,
    tasks_completed_today INTEGER,
    tasks_total_today INTEGER,
    logged_at TIMESTAMP NOT NULL
);
