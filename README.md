# 🏥 Smart Queue Management System

A production-grade, distributed **Virtual Queue Management System** built with Spring Boot microservices. Designed for hospitals, banks, government offices, and any high-footfall service environment.

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.2-DC382D?style=flat-square&logo=redis)](https://redis.io/)
[![Apache Kafka](https://img.shields.io/badge/Kafka-7.6-231F20?style=flat-square&logo=apachekafka)](https://kafka.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker)](https://www.docker.com/)

---

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Services](#-services)
- [API Reference](#-api-reference)
- [Database Design](#-database-design)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration)
- [Security](#-security)
- [Distributed Systems Design](#-distributed-systems-design)
- [Development Phases](#-development-phases)

---

## ✨ Features

| Feature | Description |
|---|---|
| 🎫 **Virtual Token Booking** | Book queue tokens remotely — no physical queuing |
| 📡 **Real-time Queue Updates** | Server-Sent Events (SSE) push live position and ETA |
| ⏱️ **Smart ETA** | EWMA rolling average service time for accurate wait estimates |
| 🔐 **JWT + OAuth2** | Stateless auth with Google OAuth2, refresh token rotation, account locking |
| 🚦 **API Gateway** | Rate limiting (Redis), circuit breakers (Resilience4j), JWT validation |
| 📧 **Notifications** | Email (HTML) + SMS on booking, called, completed events |
| 🔄 **Kafka Event Bus** | Async inter-service communication, guaranteed delivery with DLT |
| 📊 **Analytics** | Materialized views, hourly stats, peak hour heatmaps *(Phase 3)* |
| 🛡️ **Admin Dashboard** | Service management, counter control, queue admin *(Phase 3)* |
| 📈 **Observability** | Prometheus metrics + Grafana dashboards |

---

## 🏗️ Architecture

```
                           ┌─────────────────────────────────────────────┐
                           │               CLIENT APPS                   │
                           │   (Web Browser / Mobile / Kiosk)            │
                           └──────────────────┬──────────────────────────┘
                                              │ HTTPS
                           ┌──────────────────▼──────────────────────────┐
                           │           API GATEWAY  :8080                │
                           │  • JWT validation & header injection         │
                           │  • Redis rate limiting (per IP / per user)   │
                           │  • Resilience4j circuit breakers             │
                           │  • Route → downstream services               │
                           └──┬──────┬──────┬──────┬──────────────────────┘
                              │      │      │      │
              ┌───────────────▼─┐  ┌─▼──────▼─┐  ┌▼──────────────┐
              │  AUTH SERVICE   │  │  QUEUE   │  │   ANALYTICS   │
              │     :8081       │  │ SERVICE  │  │   SERVICE     │
              │  JWT + OAuth2   │  │  :8082   │  │    :8084      │
              │  Refresh tokens │  │  Tokens  │  │  Reports &    │
              │  Account lock   │  │  SSE     │  │  Dashboards   │
              └────────┬────────┘  │  Redis Q │  └───────────────┘
                       │           └────┬─────┘
                       │                │  Kafka Events
                       │           ┌────▼──────────────┐
                       │           │ NOTIFICATION SVC   │
                       │           │     :8083          │
                       │           │ Email + SMS        │
                       │           │ Retry + DLT        │
                       │           └───────────────────-┘
                       │
              ┌─────────┴──────────────────────────────────────────────┐
              │                  INFRASTRUCTURE                         │
              │  PostgreSQL :5432 │ Redis :6379 │ Kafka :9092           │
              │  Prometheus :9090 │ Grafana :3000                       │
              └────────────────────────────────────────────────────────┘
```

### Key Design Decisions

| Decision | Rationale |
|---|---|
| **Gateway validates JWT** | Downstream services trust `X-User-Id` / `X-User-Roles` headers — no re-parsing JWT |
| **Redis ZSET for queue ordering** | O(log N) insert/pop, atomic `ZPOPMIN` prevents double-calling, score = sequence number |
| **Optimistic locking (`@Version`)** | Prevents lost updates when multiple staff call tokens simultaneously |
| **Kafka for notifications** | Decouples queue operations from slow email/SMS sends; DLT handles failures |
| **Materialized views** | Analytics queries run on pre-aggregated data, not live transaction tables |
| **EWMA rolling average** | Service time estimate adapts dynamically to actual throughput (`α=0.1`) |

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.2.5 |
| **Service Mesh** | Spring Cloud 2023.0.1 |
| **API Gateway** | Spring Cloud Gateway (WebFlux) |
| **Auth** | Spring Security, JWT (JJWT 0.12.5), OAuth2 |
| **Database** | PostgreSQL 16 |
| **Migrations** | Flyway 10.10 |
| **Cache / Queue** | Redis 7.2 (ZSET for live queue, String for current serving) |
| **Messaging** | Apache Kafka (Confluent 7.6) |
| **Resilience** | Resilience4j (Circuit Breaker, Time Limiter) |
| **Notifications** | JavaMail (HTML email), SMS (pluggable — mock by default) |
| **Observability** | Spring Actuator, Prometheus, Grafana |
| **Containers** | Docker, Docker Compose |
| **Build** | Maven 3.9 (multi-module) |

---

## 📁 Project Structure

```
Smart Queue Management System/
├── pom.xml                          # Parent POM — dependency management
├── docker-compose.yml               # Full stack: all services + infra
├── .env.example                     # Copy to .env and fill real values
├── .gitguardian.yml                 # GitGuardian policy — allowlists .env.example
├── .gitignore
├── README.md
│
├── common/                          # Shared library (jar, not a service)
│   └── src/main/java/com/smartqueue/common/
│       ├── dto/          ApiResponse.java, PageResponse.java
│       ├── enums/        TokenStatus.java, UserRole.java, ErrorCode.java
│       ├── event/        QueueEvent.java           ← Kafka contract
│       ├── exception/    AppException.java, GlobalExceptionHandler.java
│       └── security/     JwtUtil.java
│
├── gateway/                         # API Gateway :8080
│   └── src/main/java/com/smartqueue/gateway/
│       ├── filter/       JwtAuthenticationFilter.java
│       ├── config/       GatewayConfig.java, RateLimiterConfig.java
│       └── fallback/     FallbackController.java
│
├── auth-service/                    # Authentication :8081
│   └── src/main/java/com/smartqueue/auth/
│       ├── controller/   AuthController.java
│       ├── service/      AuthService.java
│       ├── entity/       UserEntity.java, RoleEntity.java, RefreshTokenEntity.java
│       ├── repository/   UserRepository.java, ...
│       ├── dto/          LoginRequest, RegisterRequest, AuthResponse, ...
│       ├── security/     SecurityConfig.java, JwtAuthFilter.java
│       └── util/         JwtUtil.java
│
├── queue-service/                   # Core Queue Logic :8082
│   └── src/main/java/com/smartqueue/queue/
│       ├── controller/   TokenController.java, AdminQueueController.java, ServiceController.java
│       ├── service/      QueueService.java, EtaCalculationService.java,
│       │                 SseEmitterService.java, KafkaProducerService.java
│       ├── entity/       TokenEntity.java, ServiceEntity.java, CounterEntity.java
│       ├── repository/   TokenRepository.java, ServiceRepository.java, CounterRepository.java
│       ├── dto/          BookTokenRequest, TokenResponse, QueueStatusResponse, ...
│       ├── config/       KafkaConfig.java, RedisConfig.java
│       └── scheduler/    MaterializedViewRefreshScheduler.java
│
├── notification-service/            # Notifications :8083
│   └── src/main/java/com/smartqueue/notification/
│       ├── consumer/     QueueEventConsumer.java
│       ├── service/      NotificationService.java
│       ├── provider/     NotificationProvider.java, EmailNotificationProvider.java,
│       │                 SmsNotificationProvider.java
│       ├── entity/       NotificationEntity.java
│       ├── repository/   NotificationRepository.java
│       ├── config/       KafkaConsumerConfig.java
│       └── scheduler/    RetryScheduler.java
│
├── analytics-service/               # Analytics :8084  [Phase 3]
├── admin-service/                   # Admin :8085       [Phase 3]
│
├── docker/
│   └── postgres/
│       └── init-databases.sh        # Creates 5 databases on first run
└── prometheus.yml                   # Prometheus scrape config
```

---

## 🔧 Services

### 1. API Gateway `:8080`
Central entry point. Validates JWTs and injects identity headers. Never lets unauthenticated requests reach protected routes.

**Responsibilities:**
- `JwtAuthenticationFilter` — validates Bearer token, injects `X-User-Id`, `X-User-Email`, `X-User-Roles`
- Rate limiting — 20 req/s per IP (auth), 30 req/s per user (queue) via Redis token bucket
- Circuit breakers — Resilience4j with 50% failure threshold; auto-recovery after 5–10s
- Routes all 5 downstream services with individual fallback endpoints

### 2. Auth Service `:8081`
Handles all identity concerns. Stateless after login — JWTs are self-contained.

**Responsibilities:**
- `POST /api/auth/register` — register + auto-issue tokens
- `POST /api/auth/login` — credential login, account lock after 5 failures (15 min)
- `POST /api/auth/refresh` — rotate refresh token (old token revoked)
- `POST /api/auth/logout` — revoke refresh token
- `GET /api/auth/me` — get current user profile
- Google OAuth2 login via `/oauth2/authorization/google`
- Flyway migrations for `users`, `roles`, `refresh_tokens`

### 3. Queue Service `:8082`
The core business service. Manages the entire token lifecycle.

**Token lifecycle:** `WAITING → CALLED → SERVING → COMPLETED`  
**Exit states:** `CANCELLED`, `NO_SHOW`, `EXPIRED`

**Key endpoints:**

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/queues/{id}/book` | User | Book a token |
| `GET` | `/api/queues/{id}/status` | Optional | Queue status + your token |
| `GET` | `/api/queues/{id}/current` | Public | Currently serving number |
| `GET` | `/api/queues/{id}/stream` | None | SSE real-time stream |
| `GET` | `/api/tokens/{id}` | User | Token details + position |
| `PUT` | `/api/tokens/{id}/cancel` | User | Cancel your token |
| `POST` | `/api/admin/queues/{id}/call-next` | Staff | Call next in queue |
| `POST` | `/api/admin/tokens/{id}/complete` | Staff | Mark service complete |
| `POST` | `/api/admin/tokens/{id}/no-show` | Staff | Mark no-show |
| `GET` | `/api/services` | Public | List active services |

**Redis ZSET queue structure:**
```
Key:    queue:{serviceId}
Member: tokenId (as String)
Score:  sequenceNumber     ← ZPOPMIN always gets earliest token
TTL:    24 hours
```

### 4. Notification Service `:8083`
Purely event-driven. Listens to Kafka topics and sends notifications.

**Kafka topics consumed:**

| Topic | Trigger | Email | SMS |
|---|---|---|---|
| `token.booked` | User books a token | ✅ Confirmation with ETA | ✅ |
| `token.called` | Token called to counter | ✅ Proceed now | ✅ Priority |
| `token.completed` | Service done | ✅ Receipt | — |
| `token.cancelled` | Token cancelled/no-show | ✅ | ✅ |

**Reliability:** Failed notifications retry every 5 minutes (max 3 attempts). After exhaustion, status = `DEAD`. Real Twilio integration is a 10-line swap in `SmsNotificationProvider`.

---

## 🗄️ Database Design

### Queue Service — Key Indexes

```sql
-- Most critical: finding WAITING tokens for a service in order
CREATE INDEX idx_tokens_service_waiting ON tokens(service_id, sequence_number)
    WHERE status = 'WAITING';  -- partial index, ~90% smaller than full index

-- Composite for service + date range queries
CREATE INDEX idx_tokens_service_date ON tokens(service_id, booked_at DESC);

-- User's active tokens
CREATE INDEX idx_tokens_user_id ON tokens(user_id);
```

### Materialized Views (refreshed every 15 min)

```sql
-- Hourly throughput per service
mv_hourly_queue_stats: (service_id, hour, completed, no_show, cancelled, avg_wait)

-- Peak hours heatmap (30-day rolling window)
mv_peak_hours: (service_id, day_of_week, hour_of_day, avg_tokens, avg_wait)
```

---

## 🚀 Getting Started

### Prerequisites

- **Docker Desktop** 4.x+
- **Java 17** (for local development without Docker)
- **Maven 3.9+** (for local development)

### 1. Clone

```bash
git clone https://github.com/radhika143789/Smart-Queue-Management-System.git
cd Smart-Queue-Management-System
```

### 2. Configure secrets

```bash
cp .env.example .env
```

Edit `.env` with real values:
```env
POSTGRES_PASSWORD=your_secure_password
REDIS_PASSWORD=your_redis_password
JWT_SECRET=your-minimum-64-character-secret-key-here-replace-this-value
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your-gmail-app-password
```

> ⚠️ **Never commit `.env`** — it is gitignored. The app **will not start** without these values.

### 3. Start the full stack

```bash
docker-compose up -d
```

Services start in dependency order. Wait ~60 seconds for all health checks to pass.

```bash
# Watch startup progress
docker-compose ps
docker-compose logs -f gateway
```

### 4. Verify

```bash
# Gateway health
curl http://localhost:8080/actuator/health

# Auth service
curl http://localhost:8081/api/auth/health

# Queue service
curl http://localhost:8082/api/services
```

### 5. Register and book your first token

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@test.com","username":"testuser","password":"Password123!","firstName":"Test","lastName":"User"}'

# Copy the accessToken from the response, then book a token
curl -X POST http://localhost:8080/api/queues/1/book \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"userPhone":"+919876543210"}'
```

### 6. Monitor dashboards

| Service | URL |
|---|---|
| Grafana | http://localhost:3000 (admin/admin) |
| Prometheus | http://localhost:9090 |
| Queue SSE stream | http://localhost:8080/api/queues/1/stream |

---

## ⚙️ Configuration

All configuration is environment-variable driven. No secrets in code or YAML.

| Variable | Description | Required |
|---|---|---|
| `JWT_SECRET` | HS512 signing key — min 64 chars | ✅ |
| `POSTGRES_PASSWORD` | PostgreSQL password | ✅ |
| `REDIS_PASSWORD` | Redis password | ✅ |
| `POSTGRES_USER` | PostgreSQL user | Default: `smartqueue` |
| `AUTH_DB` | Auth service database name | Default: `auth_db` |
| `QUEUE_DB` | Queue service database | Default: `queue_db` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker URL | Default: `kafka:9092` |
| `MAIL_HOST` | SMTP host | Default: `smtp.gmail.com` |
| `MAIL_USERNAME` | Sender email | Optional |
| `MAIL_PASSWORD` | Email app password | Optional |
| `SMS_MOCK_MODE` | `true` = log SMS, `false` = real | Default: `true` |
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID | Optional |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 secret | Optional |

---

## 🔐 Security

### Authentication Flow

```
1. Client → POST /api/auth/login
2. Auth Service validates credentials → issues JWT (15 min) + refresh token (7 days)
3. Client → any protected endpoint with Authorization: Bearer <jwt>
4. API Gateway validates JWT → injects X-User-Id, X-User-Roles headers
5. Downstream services trust these headers — no JWT re-parsing needed
6. On JWT expiry → POST /api/auth/refresh with refresh token → new JWT + rotated refresh token
```

### Security Features

- **Account locking** — 5 failed logins → 15-minute lock
- **Refresh token rotation** — each refresh invalidates the old token (prevents replay)
- **No password fallbacks** — credentials must be set via environment; app fails fast on startup
- **JWT claims** — contain `userId` and `roles` for downstream authorization without DB lookup
- **Rate limiting** — per-IP for auth endpoints, per-user for queue endpoints

---

## 🌐 Distributed Systems Design

### Redis Queue (ZSET)

```
ZADD queue:1 42 "1001"      # Book token: member=tokenId, score=sequence
ZPOPMIN queue:1 1           # Call next: atomically pops lowest score
ZRANK queue:1 "1001"        # Queue position: 0-indexed rank
ZCARD queue:1               # Total waiting
SET current:1 "A-042" EX 3600  # Currently serving (1hr TTL)
```

### Kafka Event Flow

```
Queue Service
    └─ publishes QueueEvent to token.booked / token.called / token.completed / token.cancelled
            │
            ├─ Notification Service (consumer group: notification-service)
            │       └─ sends email + SMS
            │       └─ on failure: retries 2x with 5s backoff → DLT
            │
            └─ Analytics Service [Phase 3] (consumer group: analytics-service)
                    └─ aggregates stats
```

### Circuit Breaker States

```
CLOSED (normal) → [50% failure rate in 10 calls] → OPEN (fail fast)
    → [wait 5-10s] → HALF_OPEN (3 probe calls) → CLOSED or OPEN
```

---

## 📈 Development Phases

| Phase | Status | Contents |
|---|---|---|
| **Phase 1** | ✅ Complete | Infrastructure, Common Library, Auth Service, API Gateway |
| **Phase 2** | ✅ Complete | Queue Service (token lifecycle, Redis, SSE, ETA), Notification Service (Kafka, email, SMS) |
| **Phase 3** | 📋 Planned | Analytics Service (reports, charts), Admin Service (service management) |
| **Phase 4** | 📋 Planned | Integration tests, load tests, production hardening |

### Phase 1 — Infrastructure & Auth ✅
- Maven multi-module parent POM with Spring Cloud BOM
- Docker Compose with PostgreSQL, Redis, Kafka, Zookeeper, Prometheus, Grafana
- Common library: `ApiResponse`, `AppException`, `ErrorCode`, `QueueEvent`, `JwtUtil`
- Auth Service: JWT login/register/refresh/logout, Google OAuth2, account locking, Flyway migrations
- API Gateway: JWT filter, Redis rate limiter, Resilience4j circuit breakers, fallback controllers

### Phase 2 — Queue & Notifications ✅
- Queue Service: full token lifecycle (WAITING → CALLED → SERVING → COMPLETED), Redis ZSET queue, SSE streaming, EWMA ETA, materialized views refresher
- Notification Service: Kafka consumer for 4 event types, HTML email, mock SMS (Twilio-ready), retry scheduler, dead-letter topic

### Phase 3 — Analytics & Admin 📋
- Analytics Service: REST API over materialized views, peak hours, daily throughput, export CSV
- Admin Service: service CRUD, counter management, staff assignment, system health dashboard

### Phase 4 — Testing & Hardening 📋
- JUnit 5 + Testcontainers integration tests
- k6 load testing scripts
- Kubernetes manifests (Helm charts)
- CI/CD GitHub Actions pipeline

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/your-feature`
3. Commit with semantic messages: `feat:`, `fix:`, `security:`, `docs:`
4. Push and open a Pull Request

---

## 📄 License

This project is for educational and portfolio purposes.

---

*Built with ❤️ using Spring Boot microservices, Redis, Kafka, and PostgreSQL*
