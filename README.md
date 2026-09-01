# URL Shortener

[![CI/CD Pipeline](https://github.com/murilodcosta/spring-boot-redis-url-shortener/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/murilodcosta/spring-boot-redis-url-shortener/actions/workflows/ci-cd.yml)

A production-ready URL Shortener backend engineered for horizontal scalability and high throughput. Built with Java 21, Spring Boot, PostgreSQL, Redis (cache-aside, atomic token bucket rate limiting, asynchronous click tracking), and full observability via Prometheus and Grafana. Fully automated via a multi-stage GitHub Actions CI/CD pipeline deploying directly to an Azure Linux Virtual Machine with post-deploy smoke tests.

---

## Architecture Overview
<img width="1024" height="559" alt="image" src="https://github.com/user-attachments/assets/d901695b-0c57-4007-932c-98274716e7cf" />

---

## Technology Stack

- **Core Framework**: Java 21 (Virtual Threads enabled), Spring Boot, Spring WebMVC, Spring Data JPA, Spring Data Redis
- **Database & Migration**: PostgreSQL 16, Flyway Migration (strict versioning)
- **Caching & Rate Limiting**: Redis 7 (Alpine), Atomic Lua Scripts (`token_bucket.lua`)
- **Observability**: Spring Boot Actuator, Micrometer, Prometheus, Grafana
- **Testing Suite**: JUnit 5, Mockito, MockMvc, Testcontainers (PostgreSQL), Grafana k6
- **Documentation**: OpenAPI 3.0, Springdoc Swagger UI
- **DevOps & Cloud Deployment**: Multi-Stage Dockerfile, Docker Compose, GitHub Actions, GitHub Container Registry (`ghcr.io`), Azure Virtual Machine (Ubuntu 24.04 LTS)

---

## CI/CD & Automated Cloud Deployment

The repository is configured with an end-to-end automated pipeline in GitHub Actions ([`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml)):

```text
+-----------------------+     +-------------------------------+     +---------------------------+     +------------------------------+
| 1. Run Automated      | --> | 2. Build & Push Docker Image  | --> | 3. Deploy to Azure VM     | --> | 4. Post-Deploy Smoke Test    |
|    Tests (Maven +     |     |    to GHCR                    |     |    via SSH (Zero Downtime |     |    (Polling /actuator/health |
|    Testcontainers)    |     |    (Multi-Stage Container)    |     |    Container Restart)     |     |    for HTTP 200 UP)          |
+-----------------------+     +-------------------------------+     +---------------------------+     +------------------------------+
```

1. **Automated Testing (`test`)**: Spins up real PostgreSQL containers via Testcontainers on Ubuntu runners and executes all unit and integration test suites.
2. **Package Publishing (`build-and-push`)**: Builds an optimized, non-root Linux container image and pushes immutable tags (`latest`, `sha-<commit>`) to GitHub Container Registry (`ghcr.io`).
3. **Continuous Deployment (`deploy`)**: Connects securely via SSH to the Azure Virtual Machine, updates the compose definition, pulls the new image, and restarts the application container with zero database interruption.
4. **Post-Deployment Verification (`smoke-test`)**: Performs resilient health polling against the live public IP (`/actuator/health`) until HTTP 200 `UP` is verified.

---

### Core Endpoints

| Method | Endpoint | Description | Status Codes |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/urls` | Creates a shortened URL with optional expiration | `201 Created`, `400 Bad Request`, `429 Too Many Requests` |
| `GET` | `/{shortCode}` | Redirects to original long URL via Location header | `302 Found`, `404 Not Found`, `410 Gone`, `429 Too Many Requests` |
| `GET` | `/actuator/prometheus` | Exposes OpenMetrics for Prometheus scraping | `200 OK` |
| `GET` | `/actuator/health` | Application and component health status | `200 OK` |

#### Create Short URL Request Example
```http
POST /api/urls HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "url": "https://spring.io/projects/spring-boot",
  "expiresInMinutes": 60
}
```

#### Response Example (`201 Created`)
```json
{
  "shortCode": "1",
  "shortUrl": "http://localhost:8080/1",
  "longUrl": "https://spring.io/projects/spring-boot",
  "createdAt": "2026-09-01T12:00:00.000000",
  "expiresAt": "2026-09-01T13:00:00.000000"
}
```

---

## Getting Started

### Prerequisites
- Java 21 JDK
- Docker and Docker Compose
- Maven Wrapper (included)

### 1. Start Infrastructure Services
Start PostgreSQL, Redis, Prometheus, and Grafana containers:
```bash
docker compose up -d
```

### 2. Run the Application
```bash
# On Linux/macOS
./mvnw spring-boot:run

# On Windows (PowerShell)
.\mvnw spring-boot:run
```

### 3. Service Dashboard Access

- **Spring Boot Application**: `http://localhost:8080`
- **Swagger UI Documentation**: `http://localhost:8080/swagger-ui/index.html`
- **Prometheus Metrics Engine**: `http://localhost:9090`
- **Grafana Dashboards**: `http://localhost:3000` (User: `admin`, Password: `admin`)
- **PostgreSQL**: `localhost:5435` (Database: `urlshortener`, User: `app`, Password: `app`)
- **Redis**: `localhost:6379`

---

## Automated Testing & Load Testing

### Unit and Integration Test Suite
Execute all unit, slice, repository, and Testcontainers integration tests:
```bash
./mvnw clean test
```

### Load Testing with Grafana k6

#### 1. Cache Hit & Throughput Test (`k6-test.js`)
Simulates 50 concurrent virtual users hitting the redirect endpoint:
```bash
docker run --rm -i -v "${PWD}:/scripts" grafana/k6 run /scripts/k6-test.js
```

#### 2. Rate Limiter Enforcement Test (`k6-ratelimit-test.js`)
Validates exact token bucket limits (10 allowed `201`, 10 rejected `429`):
```bash
docker run --rm -i -v "${PWD}:/scripts" grafana/k6 run /scripts/k6-ratelimit-test.js
```

---

## Project Structure

```text
src/
├── main/
│   ├── java/dev/murilodcosta/url_shortener/
│   │   ├── config/          # Redis, WebMvc, Interceptors, Async, OpenAPI configs
│   │   ├── controller/      # REST API Controllers (UrlController, RedirectController)
│   │   ├── dto/             # Immutable Records (ShortenRequest, ShortenResponse)
│   │   ├── exception/       # Custom exceptions & GlobalExceptionHandler
│   │   ├── job/             # Scheduled tasks (ClickCountSyncJob)
│   │   ├── model/           # JPA Entities (UrlMapping)
│   │   ├── repository/      # Spring Data Repositories
│   │   ├── service/         # Business logic (UrlShortenerService, RateLimiterService, ClickTrackingService)
│   │   └── util/            # Base62Encoder utility
│   └── resources/
│       ├── db/migration/    # Flyway SQL schema migrations
│       ├── scripts/         # Redis Lua scripts (token_bucket.lua)
│       └── application.yml  # Application properties & metrics exposure
└── test/                    # Comprehensive unit, mock and Testcontainers integration tests
```

---

## Key Architectural Decisions

### 1. Collision-Free Base62 Encoding
- Utilizes PostgreSQL sequences to generate 64-bit numerical identifiers.
- Converts numerical identifiers into compact alphanumeric Base62 strings (`[0-9a-zA-Z]`).
- Guarantees $O(1)$ computation with mathematical collision prevention, eliminating the need for iterative database collision checks.

### 2. Cache-Aside with Redis
- **Cache Warm-Up**: Shortened URLs are populated into Redis memory at the instant of creation.
- **Microsecond Redirects**: Subsequent redirect requests are served entirely from Redis RAM with zero database disk reads.
- **Dynamic TTL Management**: Time-To-Live expiration in Redis matches the exact user-defined link lifetime.
- **Graceful Degradation**: If Redis experiences network degradation or downtime, the application falls back to PostgreSQL without interrupting service.

### 3. Token Bucket Rate Limiting
- Enforces granular rate limits per client IP address via an atomic Lua script executed inside Redis:
  - `POST /api/urls`: 10 requests per minute per IP.
  - `GET /{shortCode:[a-zA-Z0-9]+}`: 100 requests per second per IP.
- Operates under a **Fail-Open** strategy to preserve system availability during cache failures.

### 4. Asynchronous Write-Behind Click Tracking
- Resolves PostgreSQL row-lock contention under high concurrency.
- Redirect requests increment an in-memory counter in Redis (`INCR clicks:{shortCode}`) asynchronously via a dedicated `ThreadPoolTaskExecutor`.
- A scheduled background job executes atomically (`GETDEL`) every 60 seconds to synchronize accumulated counts into PostgreSQL in batch transactions.
