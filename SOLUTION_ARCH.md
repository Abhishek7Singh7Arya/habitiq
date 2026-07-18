# HabitIQ - Solution Architecture & Developer Guide

This document explains the structural design, data schemas, escalation engine, and scaling strategy of the HabitIQ platform to guide developers in future updates.

---

## 1. Multi-LLM AI Provider Layer

The AI Coach service (`ai-agent-service`) uses a custom-built provider abstraction layer using **LangChain4j**. This enables model-switching simply by modifying properties or environment variables (`AI_PROVIDER=gemini|openai|deepseek`) without recompiling code.

### Switching Models in Docker / K8s
To switch the active model on the fly, update the environment variables in `docker-compose.yml` or your Kubernetes ConfigMap:

```yaml
environment:
  - AI_PROVIDER=deepseek
  - DEEPSEEK_API_KEY=your_key_here
  - DEEPSEEK_MODEL=deepseek-chat
```

### Extending with a New Provider (e.g. Anthropic Claude)
To add a new AI model provider:
1. Open [AiConfig.java](habitiq-backend/ai-agent-service/src/main/java/com/habitiq/ai/config/AiConfig.java).
2. Add fields for the new API key and model properties.
3. Import the corresponding langchain4j dependency in `pom.xml` if needed (e.g. `langchain4j-anthropic`).
4. Update the switch-case statement inside `chatLanguageModel()` bean definition to initialize the new chat model.

---

## 2. Twilio IVR Voice & WhatsApp Escalation Flow

HabitIQ guarantees user consistency through active escalation. When a task is due, the platform initiates a multi-stage checkoff flow:

```
[Task Due] ──► Send 1st WhatsApp ──► Wait 10 Min ──► Send 2nd WhatsApp ──► Wait 30 Sec ──► Place Voice Call (IVR)
                    │                                   │                                    │
                    ▼ (User replies YES)                ▼ (User replies YES)                 ▼ (User presses 1)
               [Mark Completed]                    [Mark Completed]                     [Mark Completed]
```

### Telephony Callback Workflow
1. **Cron Polling**: The `routine-tracker-service` polls tasks due every 1 minute.
2. **First / Second Reminders**: Dispatched as WhatsApp messages via `notification-service`.
3. **Voice Escalation**:
   - `TrackerService` triggers a `TASK_VOICE_CALL` event.
   - `TwilioService` initiates the phone call and passes a dynamic TwiML script.
   - Twilio speaks the message using Polly text-to-speech and waits for key presses (`numDigits="1"`).
   - Once a key is pressed, Twilio posts the result to the `/api/notifications/webhook/voice/gather` endpoint.
   - `NotificationOrchestrationService` parses the digit and broadcasts a `VOICE_RESPONSE` event on Kafka.
   - `TrackerService` consumes this event and marks the task status (`COMPLETED` or `SKIPPED`).

---

## 3. High-Scale Strategy (10 Lakh Requests/Minute)

To handle 10 lakh (1 million) events per minute, the architecture employs the following patterns:

1. **Edge JWT Authentication**:
   The API Gateway validates JWT tokens *before* forwarding requests down the service mesh. Claims (`userId`, `role`, `email`) are mutated into custom request headers (`X-User-Id`, etc.). This eliminates downstream database calls for authentication, reducing token-parsing latency to sub-millisecond ranges.
2. **Distributed Rate Limiting**:
   The API Gateway uses Redis Reactive Rate Limiter to rate-limit incoming IP and token requests, preventing DDOS attacks at the edge.
3. **Decoupled Databases**:
   Each service has its own dedicated PostgreSQL schema. For scaling to millions of users, databases can be independently scaled, read-replicas attached, or replaced with NoSQL databases (e.g., MongoDB for AI conversations) without affecting tracking routines.
4. **Asynchronous Communications (Kafka)**:
   Notifications and progress logs are processed completely out-of-band via Kafka event topics. The core routine tracker and gateway are never blocked by Twilio webhooks or file parsing latency.
5. **Redis Cache Aside**:
   Routines and dashboard timelines are cached in Redis. Daily timeline queries hit the Redis cache, avoiding heavy PostgreSQL table joins on schedules.

---

## 4. Platform Data Schema

Below are the primary database schemas initialized by Flyway.

### User Service (`habitiq_users`)
```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE user_profiles (
    id VARCHAR(36) PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    age INTEGER,
    gender VARCHAR(50),
    weight_kg DOUBLE PRECISION,
    height_cm DOUBLE PRECISION,
    fitness_goal VARCHAR(100),
    activity_level VARCHAR(100),
    health_conditions TEXT,
    dietary_preferences TEXT,
    updated_at TIMESTAMP NOT NULL
);
```

### AI Coach Service (`habitiq_ai`)
```sql
CREATE TABLE conversations (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE messages (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

### Tracker Service (`habitiq_tracker`)
```sql
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
    session_id VARCHAR(36) REFERENCES tracking_sessions(id) ON DELETE CASCADE,
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
```

---

## 5. Adding New Escalation States

To extend the task tracking lifecycle (e.g. adding a "Notification to Emergency Contact" if a critical user doesn't answer phone calls):
1. Add a status to the `TaskStatus` enum in [ScheduledTask.java](habitiq-backend/routine-tracker-service/src/main/java/com/habitiq/tracker/domain/ScheduledTask.java) (e.g., `EMERGENCY_CONTACT_NOTIFIED`).
2. Add a database column and entity field for time logs (e.g., `emergency_notified_at`).
3. Add a new cron method in [TrackerService.java](habitiq-backend/routine-tracker-service/src/main/java/com/habitiq/tracker/service/TrackerService.java) that searches for tasks in `CALLED` state with no completion after 15 minutes.
4. Trigger a Kafka event to `notification-service` to call or SMS the emergency contact.
