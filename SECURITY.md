# Security Policy

## Supported Versions

| Version        | Supported          |
|----------------|--------------------|
| 0.x (current)  | ✅ Yes             |

## Reporting a Vulnerability

If you discover a security vulnerability in this project, please report it responsibly.

### How to report

1. **Do NOT** open a public GitHub issue for security vulnerabilities
2. Send an email to: **[security contact email]**
3. Include the following information:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

### Response timeline

- **Acknowledgment**: within 48 hours
- **Initial assessment**: within 5 business days
- **Fix timeline**: depends on severity (critical: ASAP, high: 7 days, medium: 30 days)

## Security Best Practices

### Credentials Management

- All secrets are managed via environment variables (`.env` file)
- The `.env` file is excluded from version control via `.gitignore`
- Never commit real credentials to the repository
- Use `.env.example` as a template with placeholder values

### Required Environment Variables

| Variable                           | Description                              |
|------------------------------------|------------------------------------------|
| `KEYCLOAK_DB_PASSWORD`             | PostgreSQL password for Keycloak         |
| `APP_DB_PASSWORD`                  | PostgreSQL password for the application  |
| `KEYCLOAK_ADMIN_PASSWORD`          | Keycloak admin console password          |
| `GRAFANA_ADMIN_PASSWORD`           | Grafana admin password                   |
| `KEYCLOAK_RESOURCE_CLIENT_ID`      | OAuth2 client ID for token introspection |
| `KEYCLOAK_RESOURCE_CLIENT_SECRET`  | OAuth2 client secret for introspection   |

### Authentication & Authorization

- Authentication is handled by **Keycloak** (OAuth2 / OpenID Connect)
- Token validation uses **opaque token introspection** (not JWT decoding)
- Authorization is enforced via Spring Security `@PreAuthorize` annotations
- Role-based access control (RBAC) with Keycloak realm roles

### Rate Limiting

- Rate limiting is enabled via **Bucket4j** with a custom servlet filter to prevent brute-force attacks
- `/api/auth/login`: 5 requests per minute
- `/api/clients`: 20 requests per minute

### Docker Security

- Application container runs as a non-root user (`app`)
- Multi-stage Docker build to minimize image size and attack surface
- Base image: `eclipse-temurin:25-jre-alpine`

### Dependencies

- Dependencies are regularly updated
- SonarCloud analysis is integrated into the CI pipeline

