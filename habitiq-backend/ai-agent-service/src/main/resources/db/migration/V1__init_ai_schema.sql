CREATE TABLE conversations (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE messages (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE routines (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE routine_days (
    id VARCHAR(36) PRIMARY KEY,
    routine_id VARCHAR(36) NOT NULL REFERENCES routines(id) ON DELETE CASCADE,
    day_order INTEGER NOT NULL,
    day_label VARCHAR(50) NOT NULL,
    notes TEXT
);

CREATE TABLE routine_tasks (
    id VARCHAR(36) PRIMARY KEY,
    routine_day_id VARCHAR(36) NOT NULL REFERENCES routine_days(id) ON DELETE CASCADE,
    task_type VARCHAR(50) NOT NULL,
    scheduled_time TIME NOT NULL,
    description TEXT NOT NULL,
    duration_minutes INTEGER,
    notes TEXT
);
