# HabitIQ - AI-Powered Personal Fitness Coach and Habit Escalation Platform

HabitIQ is a production-grade, distributed fitness habit tracking and generative coaching platform. Unlike ordinary tracking apps, HabitIQ actively keeps users consistent by parsing their current diet/workout plans, updating schedules, and performing interactive voice and WhatsApp follow-ups using a multi-LLM AI Agent and Twilio IVR escalation.

---

## 🚀 Platform Overview

The system is designed on a microservices architecture using Java (Spring Boot) and React Native (Expo) built to handle up to 10 lakh (1 million) active tracking state requests per minute through distributed caches, event-driven streaming, and decoupled databases.

```
                    ┌────────────────────────┐
                    │  React Native App      │
                    └───────────┬────────────┘
                                │ JWT Auth
                                ▼
                    ┌────────────────────────┐
                    │   Spring API Gateway   │
                    └───────────┬────────────┘
                                │ HTTP Routing
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│ User Service │        │ AI Agent     │        │ File Parser  │
└──────────────┘        │ Service      │        └──────────────┘
                        └───────┬──────┘
                                │ Kafka: ROUTINE_CONFIRMED
                                ▼
                        ┌──────────────┐
                        │ Tracker      │
                        │ Service      │
                        └───────┬──────┘
                                │ Kafka: TASK_DUE
                                ▼
┌──────────────┐        ┌──────────────┐
│ Progress     │◄───────┤ Notification │
│ Service      │        │ Service      │
└──────────────┘        └──────────────┘
```

---

## 🛠 Tech Stack

### Backend
- **Core Framework**: Java 21 / Spring Boot 3.3
- **Service Discovery**: Netflix Eureka Discovery Server
- **Routing & Edge Security**: Spring Cloud API Gateway (JWT claim forwarding)
- **Database**: PostgreSQL (decoupled per-service databases)
- **Caching**: Redis (distributed session, rate-limiting, and locks)
- **Event Bus**: Apache Kafka (decoupled event-driven workflows)
- **GenAI Orchestration**: LangChain4j (with switchable Gemini / OpenAI / DeepSeek provider layer)
- **Document Extractors**: Apache PDFBox / Apache POI
- **Escalation Integrations**: Twilio REST / TwiML Interactive Voice Response (IVR)

### Mobile Client
- **Framework**: React Native with Expo (Single codebase for Android, iOS, and Web)
- **Design Language**: Premium Glassmorphism Dark Theme
- **State Management**: React Context & Hooks

---

## 🔌 Port Registry

| Service Name | Port | Description |
| :--- | :--- | :--- |
| `api-gateway` | `8080` | Edge gateway (Routing, Rate Limiting, JWT Validation) |
| `config-server` | `8888` | Central configurations management |
| `discovery-server` | `8761` | Eureka discovery panel |
| `user-service` | `8081` | Authentication & User profiles |
| `file-processor-service` | `8082` | PDF/Excel plan parsing & Routine exports |
| `ai-agent-service` | `8083` | LLM Chat & weekly routine planner |
| `routine-tracker-service` | `8084` | Scheduling, daily crons, & task escalation |
| `notification-service` | `8085` | Twilio SMS/WhatsApp dispatcher & IVR Webhook |
| `progress-service` | `8086` | Progress logging, step metrics, weight milestones |

---

## 🏃 Run Platform Locally

### Prerequisites
- Java 21 installed
- Maven 3.8+ installed
- Docker & Docker Compose installed
- Node.js 18+ (for mobile client)

### Step 1: Clone and Build Jars
Compile and package all Spring Boot microservices:
```bash
cd habitiq-backend
mvn clean package -DskipTests
```

### Step 2: Set Environment Variables
Copy `.env.example` to `.env` and fill in your api keys:
```bash
cp .env.example .env
```
Ensure you set:
- `GEMINI_API_KEY` (Gemini is the default provider)
- `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, and phone numbers if testing live voice calls.

### Step 3: Run Containers
Launch all databases, message brokers, caching nodes, and microservices inside Docker:
```bash
docker-compose --env-file .env up --build -d
```
All databases are automatically initialized using Flyway migrations upon startup.

---

## 📱 Run Mobile App

### Step 1: Install Dependencies
```bash
cd habitiq-mobile
npm install
```

### Step 2: Start Expo Server
```bash
npm run start
```
Use the Expo Go app on your physical iOS/Android phone, or run an emulator to inspect the glassmorphism dashboard.

---

## 📖 Developer Solutions Guide
For detailed system flow charts, scale strategy, database schemas, and AI extension guides, see the [SOLUTION_ARCH.md](SOLUTION_ARCH.md) file.
