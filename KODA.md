# KODA.md — Project Context

## Project Overview

**Spring Boot + Keycloak OAuth2 Proxy** — REST API proxy server for Keycloak authentication.

### Purpose
The backend does not store client credentials for login/refresh/logout — the client sends them in each request. This makes the system flexible, multi-tenant, and secure. For opaque token introspection, the resource server uses service credentials from environment variables.

### Key Features
- 🔑 Username/password authentication
- 🔄 Token refresh
- 🚪 Logout with refresh token revocation
- 🛡 Opaque token validation via Spring Security introspection
- 🎭 Role-based authorization (ADMIN, CLIENT_CREATE, CLIENT_GET)
- 🚦 Rate limiting (Bucket4j)
- 📊 OpenTelemetry tracing + JSON logging
- 📈 Prometheus metrics
- 📚 Swagger/OpenAPI documentation
- 🌐 Internationalization (i18n) — English and Russian languages

### Technology Stack
| Category | Technology |
|----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.3 |
| Security | Spring Security (Resource Server) |
| Authentication | Keycloak 26+ |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Rate Limiting | Bucket4j Spring Boot Starter |
| Cache | Caffeine |
| Mapping | MapStruct |
| Testing | JUnit 5, Testcontainers, WireMock |
| Documentation | SpringDoc OpenAPI |
| Monitoring | Prometheus, Grafana, Tempo, Loki |
| Tracing | OpenTelemetry |

---

## Project Structure

```
├── .github/workflows/ci.yml    — CI/CD pipeline (Maven, SonarCloud)
├── docker-compose.yaml         — Docker Compose (Keycloak, Postgres, Grafana, Tempo, Loki, Prometheus)
├── Dockerfile                  — Application Docker image
├── pom.xml                     — Maven configuration
├── grafana/                    — Grafana provisioning
├── keycloak/                   — Exported realm for Keycloak
├── postman/                    — Postman collections
├── prometheus.yml              — Prometheus configuration
├── tempo.yaml                  — Tempo configuration
├── loki-config.yaml            — Loki configuration
├── promtail-config.yaml        — Promtail configuration
├── otel.yaml                   — OpenTelemetry Collector configuration
├── src/main/java/lt/satsyuk/  — Source code
│   ├── api/                    — Controllers, DTOs
│   ├── auth/                   — Keycloak integration
│   ├── config/                 — Configuration classes
│   ├── controller/             — REST controllers
│   ├── dto/                    — Data Transfer Objects
│   ├── exception/              — Exceptions and GlobalExceptionHandler
│   ├── mapper/                 — MapStruct mappers
│   ├── model/                  — JPA entities
│   ├── repository/             — Spring Data repositories
│   ├── security/               — Security components
│   └── service/                — Business logic
├── src/main/resources/
│   ├── application.properties  — Application configuration
│   ├── logback-spring.xml      — Logging configuration
│   ├── messages.properties     — Messages (default/en)
│   ├── messages_en.properties  — English messages
│   ├── messages_ru.properties  — Russian messages
│   └── db/migration/           — Flyway migrations
└── src/test/                   — Tests
    ├── java/lt/satsyuk/
    │   ├── api/integrationtest/ — Integration tests
    │   ├── api/util/            — Test utilities
    │   ├── dto/                 — DTO tests
    │   ├── mapper/              — Mapper tests
    │   └── service/             — Service tests
    └── resources/
        └── application-test.properties — Test configuration
```

---

## Build and Run

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker Desktop 4.28.x (for Testcontainers)

### Environment Setup
```bash
# Copy template and fill in values
copy .env.example .env
```

Required variables:
- `KEYCLOAK_DB_PASSWORD`
- `APP_DB_PASSWORD`
- `KEYCLOAK_ADMIN_PASSWORD`
- `GRAFANA_ADMIN_PASSWORD`
- `KEYCLOAK_RESOURCE_CLIENT_ID`
- `KEYCLOAK_RESOURCE_CLIENT_SECRET`

### Commands

| Action | Command |
|--------|---------|
| Build | `mvn compile` |
| Run tests | `mvn test` |
| Integration tests | `mvn verify` |
| Full build with tests | `mvn clean verify` |
| Run application | `mvn spring-boot:run` |
| Docker Compose (all services) | `docker compose up -d` |

### Endpoints After Startup
| Service | URL |
|--------|-----|
| Application | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8081/v3/api-docs |
| Prometheus metrics | http://localhost:8081/actuator/prometheus |
| Health check | http://localhost:8081/actuator/health |
| Keycloak UI | http://localhost:8080 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |

---

## Security Architecture

### Multi-layer Architecture
| Layer | Responsibility |
|-------|----------------|
| **Keycloak** | Authentication, token issuance, user/role/mapper storage |
| **Spring Security** | Token introspection, authority extraction, access rule enforcement |
| **Controllers** | Authorization rules declaration via `@PreAuthorize` |

### Role Model
| Role | Description |
|------|-------------|
| `ADMIN` | Administrative user |
| `CLIENT_CREATE` | Create clients |
| `CLIENT_GET` | Read client data |

### Role Mapping
```
Keycloak → Spring Security:
ADMIN → ROLE_ADMIN
CLIENT_CREATE → checked via @PreAuthorize("hasRole('CLIENT_CREATE')")
CLIENT_GET → checked via @PreAuthorize("hasRole('CLIENT_GET')")
```

### Access Matrix
| User | `POST /api/clients` | `GET /api/clients/{id}` |
|------|---------------------|-------------------------|
| user | ✅ (CLIENT_CREATE) | ✅ (CLIENT_GET) |
| admin | ❌ Forbidden | ❌ Forbidden |

---

## Internationalization (i18n)

The project supports multiple languages via the `Accept-Language` header.

### Message Files
- `messages.properties` — default messages (English)
- `messages_en.properties` — English messages
- `messages_ru.properties` — Russian messages

### Usage
```http
GET /api/clients/999999 HTTP/1.1
Accept-Language: ru
```

Response:
```json
{"code": 404, "message": "Клиент с id=999999 не найден"}
```

### Key Components
- `I18nConfig.java` — MessageSource and LocaleResolver configuration
- `MessageService.java` — Service for retrieving internationalized messages
- `GlobalExceptionHandler.java` — Exception handling with i18n support

---

## Development Guidelines

### Coding Style
- Java 21 with records for DTOs
- Lombok to reduce boilerplate
- MapStruct for DTO-entity mapping
- Validation annotations in DTOs (`@NotBlank`, `@Pattern`, `@Size`)
- Message keys in curly braces: `{validation.firstName.required}`

### Testing Practices
- Unit tests: `*Test.java` (Surefire)
- Integration tests: `*IT.java` (Failsafe)
- Testcontainers for Keycloak and PostgreSQL
- WireMock for negative tests (network errors, timeouts)

### Naming Conventions
- Packages: `lt.satsyuk.<subpackage>`
- DTOs: `*Request`, `*Response`
- Exceptions: `*Exception`
- Tests: `<ClassName>Test` or `<ClassName>IT`

### Contribution
1. Create a branch from `master`
2. Make changes with tests
3. Ensure `mvn verify` passes
4. Create a PR to `master`

---

## API Endpoints

### Authentication
| Method | URL | Description |
|--------|-----|-------------|
| POST | `/api/auth/login` | Login (username, password, clientId, clientSecret) |
| POST | `/api/auth/refresh` | Refresh token (refreshToken, clientId, clientSecret) |
| POST | `/api/auth/logout` | Logout (refreshToken, clientId, clientSecret) |

### Clients (protected)
| Method | URL | Role | Description |
|--------|-----|------|-------------|
| POST | `/api/clients` | CLIENT_CREATE | Create client |
| GET | `/api/clients/{id}` | CLIENT_GET | Get client by ID |

---

## CI/CD

### GitHub Actions (`.github/workflows/ci.yml`)
1. Checkout code
2. Install JDK 21
3. Cache Maven and SonarQube packages
4. Build and test: `mvn -B -ntp verify`
5. Test reports (JUnit)
6. SonarCloud analysis

### CI Requirements
- Secrets: `SONAR_TOKEN`
- Variables: `SONAR_PROJECT_KEY`, `SONAR_ORGANIZATION`

---

## Troubleshooting

### ❌ 403 on `/api/user`
Check that the token contains:
```json
"realm_access": { "roles": ["USER"] }
```

### ❌ Logout always returns 200
Keycloak 26 always returns 200 for `/logout`. The API wraps this in a structured response.

### ❌ Tests fail with "Docker not available"
- Install Docker Desktop
- **Windows**: use version 4.28.x (newer versions may have Testcontainers issues)
- Ensure Docker is running

---

## Key Files for Understanding

| File | Purpose |
|------|---------|
| `SecurityConfig.java` | Spring Security configuration, filter chain |
| `KeycloakOpaqueTokenIntrospector.java` | Custom introspector with role converter |
| `KeycloakAuthService.java` | Keycloak integration (login, refresh, logout) |
| `GlobalExceptionHandler.java` | Exception handling with i18n |
| `MessageService.java` | Message internationalization |
| `I18nConfig.java` | MessageSource, LocaleResolver configuration |
| `AbstractIntegrationTest.java` | Base class for integration tests |
