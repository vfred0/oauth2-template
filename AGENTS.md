# AGENTS.md

## Project snapshot
- `jwt-demo` is a Spring Boot 3.5 / Java 17 OAuth2 proxy in front of Keycloak, not an identity store. The backend deliberately does **not** persist login/refresh/logout client credentials; callers send `clientId`/`clientSecret` on each auth request (`README.md`, `src/main/java/lt/satsyuk/service/KeycloakAuthService.java`).
- There are two distinct auth paths: `/api/auth/**` forwards form-encoded requests to Keycloak via `KeycloakAuthService`, while protected endpoints validate bearer tokens through **opaque introspection** using separate resource-server credentials from env (`src/main/java/lt/satsyuk/config/SecurityConfig.java`, `src/main/java/lt/satsyuk/security/KeycloakOpaqueTokenIntrospector.java`, `src/main/resources/application.properties`).
- Business data is local PostgreSQL only for the `client` table; auth state stays in Keycloak. Persistence changes usually touch Flyway migration + JPA entity + repository + mapper + service + integration test together (`src/main/resources/db/migration/V1__create_client_table.sql`, `src/main/java/lt/satsyuk/model/Client.java`).

## Code patterns to follow
- Controllers expose standardized `ApiResponse<T>` envelopes. Regular CRUD endpoints return `ApiResponse.ok(...)`; auth endpoints use `ResponseEntity<ApiResponse<...>>` so they can translate `KeycloakAuthException` status codes inline (`src/main/java/lt/satsyuk/controller/AuthController.java`, `src/main/java/lt/satsyuk/dto/ApiResponse.java`).
- Protected endpoints must declare method security with `@PreAuthorize("hasRole('...')")`; roles are derived by prefixing Keycloak realm/client roles with `ROLE_` in `KeycloakOpaqueRoleConverter`, so checks use `hasRole('CLIENT_CREATE')`, not raw claim names.
- Keep Swagger annotations in sync with security: protected controllers use `@SecurityRequirement(name = "bearerAuth")`, and OpenAPI bearer scheme is declared centrally in `src/main/java/lt/satsyuk/config/OpenApiConfig.java`.
- DTOs are Java records with Bean Validation message keys, e.g. `CreateClientRequest`. If you add validation or new user-facing messages, update **all** bundles: `messages.properties`, `messages_en.properties`, `messages_ru.properties`.
- Domain exceptions carry message codes/parameters and are localized in `GlobalExceptionHandler` through `MessageService`; follow `ClientNotFoundException` / `PhoneAlreadyExistsException` instead of hardcoding translated text in controllers.
- Security failures return JSON from `JsonAuthEntryPoint` and `JsonAccessDeniedHandler`; do not introduce HTML/error-page defaults.
- `KeycloakAuthService` also emits Micrometer counters `auth.login`, `auth.refresh`, `auth.logout` tagged with `result=success|failure`; preserve these counters when changing auth flows.
- Outbound Keycloak calls should reuse the observation-enabled `RestTemplate` bean from `RestTemplateConfig` rather than constructing new clients ad hoc.
- Bucket4j rules for `/api/clients.*` depend on `@securityService.clientId() == 'spring-app'`; if you change token claims, client IDs, or auth principal shape, re-check rate limiting behavior (`src/main/java/lt/satsyuk/service/SecurityService.java`, `src/main/resources/application.properties`).

## Testing and developer workflows
- `mvn test` runs Surefire unit tests matching `*Test.java` and writes coverage to `target/site/jacoco/index.html`.
- `mvn verify` runs Failsafe integration tests matching `*IT.java` and writes coverage to `target/site/jacoco-it/index.html`.
- Run one integration test with: `mvn -DskipTests=false "-Dit.test=ClientIntegrationIT" verify` (`CONTRIBUTING.md`).
- For local runtime, copy `.env.example` to `.env`, fill Keycloak/Postgres/Grafana secrets plus `KEYCLOAK_RESOURCE_CLIENT_ID` / `KEYCLOAK_RESOURCE_CLIENT_SECRET`, then start infra with `docker compose up -d postgres postgres-app keycloak` and run the app with `mvn spring-boot:run`.
- On Windows, docs explicitly recommend Docker Desktop **4.28.x**; newer versions may break Testcontainers (`CONTRIBUTING.md`).

## Test infrastructure map
- `AbstractIntegrationTest` provides shared Postgres Testcontainer, HTTP helpers, typed `ApiResponse` assertions, and cache cleanup between tests (it preserves `filterConfigCache` but clears other caches).
- `KeycloakIntegrationTest` adds a real Keycloak Testcontainer loaded from `keycloak/realm-export.json`; use it for happy-path auth/authorization scenarios.
- `WireMockIntegrationTest` stubs Keycloak token/introspection/logout endpoints and is the correct base for negative external-failure cases and rate-limit tests.
- Test rate limits are intentionally shortened to **20 seconds** in `src/test/resources/application-test.properties`; do not “fix” waits in `RateLimitingIT` without checking that profile.
- Existing integration tests are the best executable spec for behavior: `ClientIntegrationIT` covers persistence + i18n + authz, `KeycloakIntegrationIT` covers real Keycloak flows, `KeycloakNegativeIT` covers malformed/unavailable upstream responses.

## External systems and seeded data
- `docker-compose.yaml` runs Keycloak 26, two Postgres containers (Keycloak DB + app DB), and an observability stack: OTel Collector, Tempo, Loki, Prometheus, Grafana.
- `keycloak/realm-export.json` seeds realm `my-realm`, users `user` / `admin`, clients `spring-app` and `resource-server`, plus role mappers that populate `realm_access.roles` and `resource_access.spring-app.roles`.
- Localization is request-driven via `Accept-Language`; Russian messages are asserted in `ClientIntegrationIT`, so changing error text requires synchronized bundle/test updates.

