# URL Shortener

A production-ready, URL Shortener backend engineered for horizontal scalability. Built with Java 21, Spring Boot, PostgreSQL, Redis (cache-aside, rate limiting), and Observability via Prometheus and Grafana.

---

## Architecture Overview
<img width="1024" height="559" alt="image" src="https://github.com/user-attachments/assets/d901695b-0c57-4007-932c-98274716e7cf" />

---

## Technology Stack

- **Core Framework**: Java 21, Spring Boot, Spring WebMVC, Spring Data JPA, Spring Data Redis
- **Database & Migration**: PostgreSQL 16, Flyway Migration
- **Caching & Rate Limiting**: Redis 7 (Alpine), Lua Scripting
- **Observability**: Spring Boot Actuator, Micrometer, Prometheus, Grafana
- **Testing**: JUnit 5, Mockito, MockMvc, Testcontainers, SimpleMeterRegistry, Grafana k6
- **Documentation**: OpenAPI 3.0, Springdoc Swagger UI
- **DevOps & CI/CD**: Docker (Multi-Stage Build), Docker Compose, GitHub Actions

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
  "createdAt": "2026-08-25T02:00:00.000000",
  "expiresAt": "2026-08-25T03:00:00.000000"
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

### Unit and Slice Test Suite
Execute all unit, service, repository, and controller tests:
```bash
./mvnw clean test -Dtest="!UrlMappingJpaTest,!UrlShortenerApplicationTests"
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
└── test/                    # Comprehensive unit, mock and integration tests
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
