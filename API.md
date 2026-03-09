# API Reference

Base URL: `http://localhost:8081`

Swagger UI: `http://localhost:8081/swagger-ui/index.html`  
OpenAPI JSON: `http://localhost:8081/v3/api-docs`

---

## Response Format

All endpoints return a standardized response:

```json
{
  "code": 0,
  "message": "Success",
  "data": { ... }
}
```

Error response:

```json
{
  "code": 400,
  "message": "Error description",
  "data": null
}
```

### Error Codes

| Code | Meaning               |
|------|-----------------------|
| 0    | Success               |
| 400  | Bad Request           |
| 401  | Unauthorized          |
| 403  | Forbidden             |
| 404  | Not Found             |
| 409  | Conflict              |
| 429  | Too Many Requests     |
| 500  | Internal Server Error |

---

## Authentication Endpoints

### POST /api/auth/login

Authenticate with username/password and client credentials.

**Request Body:**
```json
{
  "username": "user",
  "password": "password",
  "clientId": "spring-app",
  "clientSecret": "your-client-secret"
}
```

**Success Response (200):**
```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "access_token": "eyJhbG...",
    "refresh_token": "eyJhbG...",
    "token_type": "Bearer",
    "expires_in": 300
  }
}
```

**Error Responses:**
- `400` — Validation error (missing fields)
- `401` — Invalid credentials
- `429` — Rate limit exceeded (5 requests/minute)

---

### POST /api/auth/refresh

Refresh an expired access token.

**Request Body:**
```json
{
  "refreshToken": "eyJhbG...",
  "clientId": "spring-app",
  "clientSecret": "your-client-secret"
}
```

**Success Response (200):**
```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "access_token": "eyJhbG...",
    "refresh_token": "eyJhbG...",
    "token_type": "Bearer",
    "expires_in": 300
  }
}
```

**Error Responses:**
- `400` — Validation error
- `401` — Invalid or expired refresh token

---

### POST /api/auth/logout

Revoke a refresh token.

**Request Body:**
```json
{
  "refreshToken": "eyJhbG...",
  "clientId": "spring-app",
  "clientSecret": "your-client-secret"
}
```

**Success Response (200):**
```json
{
  "code": 0,
  "message": "Logout successful",
  "data": null
}
```

> ℹ️ Keycloak 26 always returns 200 for logout, even with invalid tokens.

---

## Client Endpoints (Protected)

All client endpoints require a valid Bearer token in the `Authorization` header:

```
Authorization: Bearer <access_token>
```

### POST /api/clients

Create a new client.

**Required Role:** `CLIENT_CREATE`

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+37061234567"
}
```

**Validation Rules:**

| Field      | Rules                                               |
|------------|-----------------------------------------------------|
| firstName  | Required, 1–50 characters                           |
| lastName   | Required, 1–50 characters                           |
| phone      | Required, must match pattern `+[0-9]{7,15}`         |

**Success Response (200):**
```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+37061234567"
  }
}
```

**Error Responses:**
- `400` — Validation error
- `401` — Missing or invalid token
- `403` — Insufficient role
- `409` — Phone number already exists
- `429` — Rate limit exceeded (20 requests/minute)

---

### GET /api/clients/{id}

Get a client by ID.

**Required Role:** `CLIENT_GET`

**Path Parameters:**

| Parameter | Type | Description       |
|-----------|------|-------------------|
| id        | Long | Client identifier |

**Success Response (200):**
```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+37061234567"
  }
}
```

**Error Responses:**
- `400` — Invalid ID format
- `401` — Missing or invalid token
- `403` — Insufficient role
- `404` — Client not found

---

## Internationalization

The API supports multiple languages via the `Accept-Language` header.

**Supported Languages:**

| Code | Language |
|------|----------|
| `en` | English  |
| `ru` | Russian  |

**Example:**
```http
GET /api/clients/999999 HTTP/1.1
Authorization: Bearer <token>
Accept-Language: ru
```

Response:
```json
{
  "code": 404,
  "message": "Клиент с id=999999 не найден",
  "data": null
}
```

---

## Actuator Endpoints (Monitoring)

| Endpoint                       | Description              |
|--------------------------------|--------------------------|
| `GET /actuator/health`         | Application health check |
| `GET /actuator/prometheus`     | Prometheus metrics       |

---

## Rate Limits

| Endpoint          | Limit              |
|-------------------|--------------------|
| `/api/auth/login` | 5 requests/minute  |
| `/api/clients.*`  | 20 requests/minute |

When rate limit is exceeded, the API returns HTTP `429 Too Many Requests`.

