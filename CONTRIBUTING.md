# Contributing to jwt-demo

Thank you for your interest in contributing! This document provides guidelines and instructions for contributing to the project.

## Table of Contents

- [Development Environment Setup](#development-environment-setup)
- [Project Structure](#project-structure)
- [Coding Standards](#coding-standards)
- [Branch Naming](#branch-naming)
- [Commit Messages](#commit-messages)
- [Pull Request Process](#pull-request-process)
- [Testing](#testing)
- [Code Review Checklist](#code-review-checklist)

---

## Development Environment Setup

### Prerequisites

| Tool               | Version   |
|--------------------|-----------|
| Java (JDK)         | 21+       |
| Maven              | 3.9+      |
| Docker Desktop     | 4.28.x    |
| Git                | 2.40+     |

> ⚠️ **Windows users**: Use Docker Desktop version **4.28.x** for stable Testcontainers support. Newer versions (4.29+) may cause compatibility issues.

### First-time setup

1. Clone the repository:
   ```bash
   git clone https://github.com/<owner>/jwt-demo.git
   cd jwt-demo
   ```

2. Copy the environment template:
   ```bash
   copy .env.example .env
   ```

3. Fill in all required variables in `.env` (see `.env.example` for the list).

4. Start infrastructure services:
   ```bash
   docker compose up -d postgres postgres-app keycloak
   ```

5. Build the project:
   ```bash
   mvn clean compile
   ```

6. Run tests to verify the setup:
   ```bash
   mvn test
   ```

---

## Project Structure

```
src/main/java/lt/satsyuk/
├── auth/           — Keycloak authentication handlers
├── config/         — Spring configuration classes
├── controller/     — REST controllers (AuthController, ClientController)
├── dto/            — Data Transfer Objects (records)
├── exception/      — Custom exceptions and GlobalExceptionHandler
├── mapper/         — MapStruct mappers
├── model/          — JPA entities
├── repository/     — Spring Data JPA repositories
├── security/       — Security components (introspector, role converter)
└── service/        — Business logic services

src/test/java/lt/satsyuk/
├── api/integrationtest/ — Integration tests (Testcontainers, WireMock)
├── api/util/            — Test base classes and utilities
├── api/validation/      — Validation tests
├── dto/                 — DTO unit tests
├── mapper/              — Mapper unit tests
└── service/             — Service unit tests
```

---

## Coding Standards

### General

- **Java version**: 21 (use records, sealed classes, text blocks where appropriate)
- **Formatting**: Follow standard Java conventions; use IDE auto-format
- **No warnings**: Code should compile without warnings

### Conventions

| Area            | Convention                                                    |
|-----------------|---------------------------------------------------------------|
| DTOs            | Java records (`*Request`, `*Response`)                        |
| Entities        | Lombok `@Data` / `@Builder` on JPA `@Entity` classes         |
| Mapping         | MapStruct interfaces (`*Mapper`)                              |
| Exceptions      | Custom exceptions extending `RuntimeException`                |
| Validation      | Bean Validation annotations (`@NotBlank`, `@Pattern`, `@Size`) |
| Messages (i18n) | Keys in `messages.properties` using dot notation              |
| Packages        | `lt.satsyuk.<subpackage>`                                     |

### Security

- Never commit secrets, passwords, or tokens
- Use environment variables for all credentials
- All new endpoints must have appropriate `@PreAuthorize` annotations

---

## Branch Naming

Use the following prefixes:

| Prefix      | Purpose                 | Example                        |
|-------------|-------------------------|--------------------------------|
| `feature/`  | New feature             | `feature/add-user-endpoint`    |
| `bugfix/`   | Bug fix                 | `bugfix/fix-token-refresh`     |
| `hotfix/`   | Urgent production fix   | `hotfix/fix-login-crash`       |
| `chore/`    | Maintenance / refactor  | `chore/update-dependencies`    |
| `docs/`     | Documentation changes   | `docs/update-readme`           |
| `test/`     | Test improvements       | `test/add-negative-tests`      |

---

## Commit Messages

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <short description>

[optional body]
```

**Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `ci`, `perf`

Examples:
```
feat(auth): add token expiration validation
fix(clients): handle duplicate phone numbers
test(integration): add WireMock timeout tests
docs: update API documentation
```

---

## Pull Request Process

1. Create a branch from `master` using the naming convention above
2. Make your changes with appropriate tests
3. Ensure all checks pass:
   ```bash
   mvn clean verify
   ```
4. Push your branch and create a Pull Request to `master`
5. Fill in the PR template with a description of changes
6. Request review from at least one team member
7. Address review comments
8. Squash-merge after approval

---

## Testing

### Test categories

| Type              | Pattern       | Plugin    | Command           |
|-------------------|---------------|-----------|-------------------|
| Unit tests        | `*Test.java`  | Surefire  | `mvn test`        |
| Integration tests | `*IT.java`    | Failsafe  | `mvn verify`      |

### Requirements for new code

- All new features must include unit tests
- All new endpoints must include integration tests
- Integration tests must use Testcontainers (Keycloak, PostgreSQL)
- Negative scenarios should use WireMock when testing external service failures
- Coverage reports: `target/site/jacoco/index.html` (unit), `target/site/jacoco-it/index.html` (integration)

### Running specific tests

```bash
# Run a single test class
mvn test -Dtest=KeycloakAuthServiceTest

# Run a single integration test
mvn -DskipTests=false "-Dit.test=ClientIntegrationIT" verify

# Run tests with full stack traces
mvn clean verify -DskipTests=false -DtrimStackTrace=false
```

---

## Code Review Checklist

- [ ] Code compiles without warnings
- [ ] All existing tests pass (`mvn verify`)
- [ ] New code is covered by tests
- [ ] No hardcoded secrets or credentials
- [ ] API changes are documented in Swagger annotations
- [ ] Exception handling follows the `GlobalExceptionHandler` pattern
- [ ] i18n messages are added to all `messages*.properties` files
- [ ] Commit messages follow Conventional Commits

