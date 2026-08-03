<h1 align="center">🏥 Smart Queue Management System</h1>
<p align="center">
  A production-grade distributed microservices platform for hospitals, banks, and government offices.
  Users book virtual tokens and receive real-time queue updates via web, SMS, and email.
</p>

---

## Architecture

```
                         ┌─────────────────────────────────┐
                         │          API Gateway :8080        │
                         │  JWT Validation · Rate Limiting   │
                         │  Circuit Breaker · CORS           │
                         └────┬────────┬────────┬───────────┘
                              │        │        │
               ┌──────────────┘        │        └──────────────┐
               ▼                       ▼                        ▼
      ┌─────────────┐        ┌──────────────┐        ┌──────────────────┐
      │ Auth Service│        │Queue Service │        │Analytics/Admin   │
      │   :8081     │        │   :8082      │        │  :8084 / :8085   │
      └──────┬──────┘        └──────┬───────┘        └────────┬─────────┘
             │                      │                          │
             └──────────────────────┼──────────────────────────┘
                                    │
                    ┌───────────────▼───────────────┐
                    │           Apache Kafka          │
                    │  token.booked · token.called   │
                    │  token.completed · retry        │
                    └───────────────┬───────────────┘
                                    │
                    ┌───────────────▼───────────────┐
                    │      Notification Service       │
                    │           :8083                 │
                    │   SMS (Twilio) · Email (SES)    │
                    └───────────────────────────────-┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Framework** | Spring Boot 3.2.5, Spring Cloud 2023.0.1 |
| **Language** | Java 17 |
| **Database** | PostgreSQL 16 (per service) |
| **Cache** | Redis 7.2 |
| **Messaging** | Apache Kafka 7.6.0 (Confluent) |
| **Auth** | JWT (JJWT 0.12.5) + Google OAuth2 |
| **Gateway** | Spring Cloud Gateway (reactive/WebFlux) |
| **Resilience** | Resilience4j (Circuit Breaker + Time Limiter) |
| **Migration** | Flyway |
| **Monitoring** | Prometheus + Grafana |
| **Container** | Docker + Docker Compose |
| **Build** | Maven (multi-module) |

---

## Services

| Service | Port | Description |
|---|---|---|
| **API Gateway** | 8080 | Single entry point — JWT validation, rate limiting, routing |
| **Auth Service** | 8081 | Registration, login, JWT issuance, Google OAuth2 |
| **Queue Service** | 8082 | Token booking, live queue tracking, ETA calculation |
| **Notification Service** | 8083 | Kafka-driven SMS/email dispatcher |
| **Analytics Service** | 8084 | Materialized views, peak-hour reports, wait-time analytics |
| **Admin Service** | 8085 | Admin dashboard API, service configuration |
| **Prometheus** | 9090 | Metrics collection |
| **Grafana** | 3000 | Metrics dashboards |

---

## Quick Start

### Prerequisites
- Docker Desktop (or Docker + Docker Compose)
- Java 17 + Maven 3.9+ (for local dev)

### 1. Clone & Configure

```bash
git clone <repo-url>
cd smart-queue-management

# Copy and fill in environment variables
cp .env.example .env
# Edit .env with your values (DB passwords, JWT secret, Twilio, etc.)
```

### 2. Start with Docker Compose

```bash
docker-compose up --build
```

All services, databases, Kafka, Redis, Prometheus and Grafana start automatically with health-check ordering.

### 3. Local Development (single service)

```bash
# Start infrastructure only
docker-compose up postgres redis kafka zookeeper -d

# Run a service locally
cd auth-service
mvn spring-boot:run
```

### 4. Build all services

```bash
mvn clean package -DskipTests
```

---

## API Reference

All requests go through the Gateway at `http://localhost:8080`.

### Auth Endpoints (public)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Login → returns JWT pair |
| `POST` | `/api/auth/refresh` | Rotate refresh token |
| `POST` | `/api/auth/logout` | Revoke refresh token |
| `GET` | `/api/auth/me` | Get current user profile |
| `GET` | `/api/auth/oauth2/google` | Google OAuth2 login |

### Queue Endpoints (require JWT)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/queues/{serviceId}/book` | Book a token |
| `GET` | `/api/queues/{serviceId}/status` | Live position + ETA |
| `GET` | `/api/queues/{serviceId}/current` | Currently serving (public) |
| `GET` | `/api/queues/{serviceId}/stream` | SSE real-time stream |
| `PUT` | `/api/tokens/{tokenId}/cancel` | Cancel a token |

### Admin Endpoints (require ADMIN role)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/admin/queues/{serviceId}/call-next` | Call next token |
| `POST` | `/api/admin/queues/{serviceId}/pause` | Pause queue |
| `GET` | `/api/admin/dashboard` | Live system metrics |
| `GET` | `/api/admin/users` | Paginated user list |

---

## Example Requests

### Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "username": "johndoe",
    "password": "SecurePass123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"SecurePass123"}'
```

### Book a Token
```bash
curl -X POST http://localhost:8080/api/queues/1/book \
  -H "Authorization: Bearer <access_token>"
```

---

## SQL Optimization Strategy

| Technique | Implementation |
|---|---|
| **B-Tree Index** | `tokens(service_id, status, created_at)` |
| **Partial Index** | `tokens(service_id) WHERE status = 'WAITING'` |
| **Materialized Views** | `mv_hourly_queue_stats`, `mv_peak_hours` — refreshed every 15 min |
| **Keyset Pagination** | Token listing uses cursor-based pagination |
| **Redis Cache** | Analytics (TTL 5 min), current serving token (TTL 10s) |
| **Connection Pool** | HikariCP, max-pool-size=10 per service |
| **Optimistic Locking** | `@Version` on `tokens` table for concurrent updates |

---

## Project Structure

```
smart-queue-management/
├── pom.xml                     # Parent Maven POM (multi-module)
├── docker-compose.yml
├── .env.example
├── docker/
│   ├── postgres/
│   │   └── init-databases.sh   # Creates all 5 databases
│   └── prometheus/
│       └── prometheus.yml
├── common/                     # Shared library (DTOs, enums, exceptions)
│   └── src/main/java/com/smartqueue/common/
│       ├── dto/                # ApiResponse, PageResponse
│       ├── enums/              # TokenStatus, UserRole, ErrorCode
│       ├── event/              # QueueEvent (Kafka DTO)
│       ├── exception/          # AppException, GlobalExceptionHandler
│       └── security/           # JwtUtil
├── auth-service/               # JWT + OAuth2 authentication
├── queue-service/              # Token booking + live queue (Phase 2)
├── notification-service/       # Kafka → SMS/Email (Phase 2)
├── analytics-service/          # Reports + materialized views (Phase 3)
├── admin-service/              # Admin dashboard API (Phase 3)
├── gateway/                    # Spring Cloud Gateway (API Gateway)
└── frontend/                   # HTML/CSS/JS SPA dashboard (Phase 3)
```

---

## Monitoring

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (default: admin/admin)

All services expose `/actuator/prometheus` for metric scraping.

---

## Development Phases

- ✅ **Phase 1** — Scaffolding, Common Library, Auth Service, API Gateway
- ⏳ **Phase 2** — Queue Service (token booking, SSE, Redis queue, ETA), Notification Service
- ⏳ **Phase 3** — Analytics Service, Admin Service, Frontend Dashboard

---

## Security Notes

- JWT access tokens expire in **15 minutes**; refresh tokens in **7 days**
- Refresh token **rotation** on every use (old token revoked)
- Account **locked for 15 min** after 5 failed login attempts
- All passwords hashed with **BCrypt strength 12**
- Rate limiting: **20 req/s** on auth endpoints, **30 req/s** on queue endpoints
- Circuit breakers prevent cascade failures across services
