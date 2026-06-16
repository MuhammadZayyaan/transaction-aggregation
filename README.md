# Transaction Aggregation API

A production-grade Spring Boot 4 POC simulating a financial services platform with authentication, role-based access, session management, rate limiting, and transaction aggregation from multiple downstream services.

## Tech Stack

- Java 21
- Spring Boot 4.1.0
- PostgreSQL 18
- Liquibase (schema management + seed data)
- JWT (HMAC for session tokens, RSA/PKCS12 for API tokens)
- Guava (session cache + rate limit cache)
- OpenAPI 3.0 + openapi-generator (contract-first API design)
- Springdoc (Swagger UI)
- Maven

## Features

### Authentication & Security

- **Login with BCrypt password hashing** — credentials validated against `sso` table
- **Account lock-out** — 3 failed attempts locks the account (configurable)
- **Dual-token architecture:**
  - **Session token** (HMAC-signed) — 60 min hard TTL + 15 min inactivity timeout via Guava cache
  - **API token** (RSA-signed from PKCS12 keystore) — for machine-to-machine calls, no session required
- **JWT auth filter** — centralized token validation on all `/api/*` endpoints
- **Logout** — immediately invalidates session (token becomes useless even if not expired)
- **Consistent error messages** — prevents user enumeration attacks
- **No username in JWT** — only userId stored in token payload to prevent PII exposure

### Session Management

- Guava cache with sliding 15-minute inactivity window
- Each API call refreshes the session TTL
- **User profile cached in session** — first `/api/user/me` call hits DB, subsequent calls read from cache (no repeated DB lookups)
- Session survives page navigation within the app
- Idle users are timed out regardless of JWT expiry
- Logout removes session from cache instantly

### Rate Limiting

- Custom Guava-based per-user rate limiter (filter-based, transparent to controllers)
- Default limits:
  - Login: 5 requests/minute per IP
  - API calls: 60 requests/minute per user
  - Token generation: 3 requests/minute per user
- Response headers on every request: `X-RateLimit-Limit`, `X-RateLimit-Remaining`
- 429 Too Many Requests with `Retry-After` header when exceeded
- **Admin-adjustable at runtime** via `PUT /api/admin/rate-limit`
- **Note:** Mock APIs (`/mock/api/*`) are intentionally not rate-limited — they simulate external services

### User & Role Management

- User profiles linked to SSO accounts (name, country, ID type, ID reference)
- Role-based access control (ADMIN / USER)
- Admin-only features gated on both client and server side

### Transaction Aggregation

- Aggregates data from two simulated external APIs (payments + transfers)
- `TransactionService` calls mock APIs via Spring `RestClient`
- Mock API URLs are configurable — swap for real services in production
- Endpoints: all transactions, payments only, transfers only, aggregation summary (totals + category breakdown)

### Mock External APIs

- `MockPaymentTransactionsAPI` — simulates a payment service backed by PostgreSQL
- `MockTransferTransactionsAPI` — simulates a transfer service backed by PostgreSQL
- Both serve data per user ID, loaded from Liquibase-seeded tables
- **Not secured or rate-limited** — simulates internal/external downstream services

### OpenAPI Contract

- Full OpenAPI 3.0 spec at `src/main/resources/openapi/api-spec.yaml`
- openapi-generator produces interfaces + model DTOs at compile time
- Swagger UI available at `/swagger-ui.html`
- Spec served statically at `/openapi/api-spec.yaml`

### Frontend (Static HTML)

- **Login page** — MZR Bank branding, username/password with show/hide toggle, security basics panel, error messaging
- **Dashboard** — donut chart (pastel category breakdown), scrollable transaction bubbles, payments table, transfers table, user profile, last login timestamp, admin tools button (role-gated)
- **API Testing Tool** (admin only):
  - **Tabbed interface** — Single Request and Load Test as separate tabs
  - Dropdown of all APIs with editable URL, method, and request body
  - **Single Request tab** — send one request, view formatted response
  - **Load Test tab** — configurable concurrent requests with results dashboard (total, success, failed, avg/min/max response time, requests/sec)
  - Full name displayed in header
  - Non-admins see "Not Authorized" screen

## Project Structure

```
src/main/java/com/api/transaction_aggregation/
├── auth/
│   ├── controller/    LoginController, ApiTokenController, AdminUserController
│   ├── dto/           LoginRequest, LoginResponse
│   ├── entity/        SsoUser
│   ├── filter/        JwtAuthFilter
│   ├── repository/    SsoUserRepository
│   ├── service/       JwtService, LoginService, ApiTokenService
│   └── session/       SessionCache, SessionInfo
├── config/            SecurityConfig, FilterConfig
├── controller/        TransactionController
├── exception/         GlobalExceptionHandler
├── mock/
│   ├── controller/    MockPaymentTransactionsAPI, MockTransferTransactionsAPI
│   ├── entity/        Payment, Transfer
│   └── repository/    PaymentRepository, TransferRepository
├── model/             Transaction, AggregationResult
├── ratelimit/         RateLimitCache, RateLimitFilter, RateLimitController
├── role/
│   ├── entity/        UserRole
│   ├── repository/    UserRoleRepository
│   └── service/       RoleService
├── service/           TransactionService
└── user/
    ├── controller/    UserController
    ├── dto/           UserProfile
    ├── entity/        AppUser
    ├── repository/    AppUserRepository
    └── service/       UserService
```

## API Endpoints

### Authentication
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/login` | Public | Authenticate and get session token |
| POST | `/api/auth/logout` | Session | Invalidate session |
| POST | `/api/token/generate` | Session (Admin) | Generate RSA-signed API token |
| GET | `/api/token/validate` | Session | Validate an API token (via X-API-Token header) |

### User
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/user/me` | Session/API | Get authenticated user profile + role |

### Transactions
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/transactions` | Session/API | All transactions (payments + transfers) |
| GET | `/api/transactions/payments` | Session/API | Payments only |
| GET | `/api/transactions/transfers` | Session/API | Transfers only (with account details) |
| GET | `/api/transactions/aggregate` | Session/API | Totals and category breakdown |

**Query Parameters** (supported on `/api/transactions`, `/api/transactions/payments`, `/api/transactions/aggregate`):

| Parameter | Type | Example | Description |
|-----------|------|---------|-------------|
| `category` | string | `?category=groceries` | Filter by transaction category |
| `status` | string | `?status=successful` | Filter by status (successful/failed) |
| `from` | string | `?from=2026-06-10T00:00:00` | Filter from date (inclusive) |
| `to` | string | `?to=2026-06-14T23:59:59` | Filter to date (inclusive) |
| `minAmount` | number | `?minAmount=100` | Minimum transaction amount |
| `maxAmount` | number | `?maxAmount=5000` | Maximum transaction amount |
| `sort` | string | `?sort=amount` | Sort field (amount/category/status/reference/timestamp) |
| `order` | string | `?order=desc` | Sort direction (asc/desc, default: desc) |

**Examples:**
```bash
# Groceries only, sorted by amount
/api/transactions?category=groceries&sort=amount&order=asc

# Successful payments over R1000
/api/transactions/payments?status=successful&minAmount=1000

# Aggregate for a date range
/api/transactions/aggregate?from=2026-06-10T00:00:00&to=2026-06-14T23:59:59

# All failed transactions
/api/transactions?status=failed
```

### Admin
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/admin/rate-limit` | Session (Admin) | View current rate limits |
| PUT | `/api/admin/rate-limit` | Session (Admin) | Update rate limits at runtime |
| POST | `/api/admin/users/{username}/unlock` | Session (Admin) | Unlock a locked user account |

### Mock APIs (No auth, no rate limiting)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/mock/api/payments/{userId}` | Mock payment service |
| GET | `/mock/api/transfers/{userId}` | Mock transfer service |

## Database Schema

| Table | Purpose |
|-------|---------|
| `sso` | Login credentials, status, failed attempts, login timestamps |
| `app_user` | User profile (name, country, ID) |
| `role` | User roles (ADMIN/USER) |
| `payments` | Mock payment transactions |
| `transfers` | Mock transfer transactions |

## Solution Architecture

See the full solution diagram: [docs/solution-diagram.png](docs/solution-diagram.png)

The system follows a layered architecture with:
- **Login flow** — credentials validated against SSO table, JWT issued with session cached in Guava
- **Dashboard flow** — JWT validated via filter, user profile loaded from cache, role checked for feature access
- **Transaction flow** — TransactionService aggregates data from MockPaymentTransactionsAPI and MockTransferTransactionsAPI
- **API Testing flow** — admin role verified, requests sent with JWT, responses rendered

## Prerequisites

### Option A: Docker (Recommended)
- Docker Desktop installed and running

### Option B: Local Development
- Java 21: `/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home`
- PostgreSQL running on `localhost:5432`
- Database `transaction_aggregation_db` created

## Setup & Run

### Docker (one command)

```bash
docker compose up --build -d
```

This spins up:
- **Postgres** container (port 5433 externally, auto-creates DB + seed data via Liquibase)
- **App** container (port 8080, fully configured)

Stop:
```bash
docker compose down
```

Reset (wipe DB and start fresh):
```bash
docker compose down -v
docker compose up --build -d
```

### Local Development

```bash
# Set Java 21
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home

# Create database (first time only)
psql -U postgres -c "CREATE DATABASE transaction_aggregation_db;"

# Run the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The app starts on `http://localhost:8080/transaction-aggregation/`

### Available Pages

| URL | Description |
|-----|-------------|
| `/transaction-aggregation/login.html` | Login page |
| `/transaction-aggregation/dashboard.html` | User dashboard |
| `/transaction-aggregation/api-testing.html` | API Testing Tool (admin only) |
| `/transaction-aggregation/swagger-ui.html` | Swagger UI (API documentation) |
| `/transaction-aggregation/openapi/api-spec.yaml` | Raw OpenAPI spec |

## Test Credentials

| Username | Password | Role | Status |
|----------|----------|------|--------|
| admin | admin123 | ADMIN | Active |
| user1 | user123 | USER | Active |
| user2 | user234 | USER | Locked |

## Testing

### Option A: GUI (Browser)

1. Open `http://localhost:8080/transaction-aggregation/login.html`
2. Login with `admin` / `admin123`
3. Dashboard loads with transaction data, charts, and tables
4. Click "API Testing Tool" (admin only) to test any endpoint interactively

### Option B: Pure API (curl / Postman)

Complete walkthrough — no browser needed:

```bash
# Step 1: Login and get a session token
TOKEN=$(curl -s -X POST http://localhost:8080/transaction-aggregation/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

echo "Token: $TOKEN"

# Step 2: Get user profile
curl -s http://localhost:8080/transaction-aggregation/api/user/me \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 3: Get all transactions
curl -s http://localhost:8080/transaction-aggregation/api/transactions \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 4: Filter by category
curl -s "http://localhost:8080/transaction-aggregation/api/transactions?category=groceries" \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 5: Filter by status
curl -s "http://localhost:8080/transaction-aggregation/api/transactions?status=failed" \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 6: Filter by amount range
curl -s "http://localhost:8080/transaction-aggregation/api/transactions?minAmount=1000&maxAmount=10000" \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 7: Filter by date range
curl -s "http://localhost:8080/transaction-aggregation/api/transactions?from=2026-06-10T00:00:00&to=2026-06-12T23:59:59" \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 8: Sort by amount descending
curl -s "http://localhost:8080/transaction-aggregation/api/transactions?sort=amount&order=desc" \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 9: Combined filters
curl -s "http://localhost:8080/transaction-aggregation/api/transactions?category=groceries&status=successful&sort=amount&order=asc" \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 10: Payments only
curl -s http://localhost:8080/transaction-aggregation/api/transactions/payments \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 11: Transfers only (with account details)
curl -s http://localhost:8080/transaction-aggregation/api/transactions/transfers \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 12: Aggregation summary
curl -s http://localhost:8080/transaction-aggregation/api/transactions/aggregate \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 13: Aggregation filtered (successful only)
curl -s "http://localhost:8080/transaction-aggregation/api/transactions/aggregate?status=successful" \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 14: Generate RSA-signed API token (admin only)
curl -s -X POST http://localhost:8080/transaction-aggregation/api/token/generate \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 15: View rate limits (admin)
curl -s http://localhost:8080/transaction-aggregation/api/admin/rate-limit \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 16: Unlock a user (admin)
curl -s -X POST http://localhost:8080/transaction-aggregation/api/admin/users/user1/unlock \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 17: Logout
curl -s -X POST http://localhost:8080/transaction-aggregation/api/auth/logout \
  -H "Authorization: Bearer $TOKEN" | jq

# Step 18: Verify token no longer works after logout
curl -s http://localhost:8080/transaction-aggregation/api/user/me \
  -H "Authorization: Bearer $TOKEN" | jq
# Expected: {"error":"Session expired. Please log in again.","status":401}
```

### Option C: Swagger UI

Open `http://localhost:8080/transaction-aggregation/swagger-ui.html` for interactive API documentation.

## Quick Test

```bash
# Login
curl -X POST http://localhost:8080/transaction-aggregation/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Use the returned token for API calls
curl http://localhost:8080/transaction-aggregation/api/transactions/aggregate \
  -H "Authorization: Bearer <token>"

# Generate RSA-signed API token (admin only)
curl -X POST http://localhost:8080/transaction-aggregation/api/token/generate \
  -H "Authorization: Bearer <session-token>"

# Use API token (works even after logout)
curl http://localhost:8080/transaction-aggregation/api/transactions \
  -H "Authorization: Bearer <api-token>"

# View rate limits (admin)
curl http://localhost:8080/transaction-aggregation/api/admin/rate-limit \
  -H "Authorization: Bearer <token>"

# Update rate limits at runtime (admin)
curl -X PUT http://localhost:8080/transaction-aggregation/api/admin/rate-limit \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"loginLimit":10,"apiLimit":120,"tokenGenerateLimit":5}'

# Unlock a locked user (admin)
curl -X POST http://localhost:8080/transaction-aggregation/api/admin/users/user1/unlock \
  -H "Authorization: Bearer <token>"
```

## Configuration

All values are configurable via environment variables:

| Property | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | 8080 | Application port |
| `DB_CONNECTION_URL` | `jdbc:postgresql://localhost:5432/transaction_aggregation_db` | Database URL |
| `DB_CONNECTION_USER` | postgres | Database username |
| `DB_CONNECTION_PASS` | postgres | Database password |
| `JWT_SECRET` | (dev default) | HMAC secret for session tokens |
| `JWT_EXPIRATION` | 3600000 (60 min) | Session token TTL in ms |
| `SESSION_TIMEOUT` | 15 | Inactivity timeout in minutes |
| `MAX_FAILED_ATTEMPTS` | 3 | Login attempts before lock-out |
| `API_KEYSTORE_PATH` | keystore/api-keystore.p12 | PKCS12 keystore path |
| `API_KEYSTORE_PASSWORD` | (dev default) | Keystore password |
| `API_TOKEN_EXPIRATION` | 3600000 (60 min) | API token TTL in ms |
| `PAYMENTS_API_URL` | http://localhost:8080/.../mock/api/payments | Payments service URL |
| `TRANSFERS_API_URL` | http://localhost:8080/.../mock/api/transfers | Transfers service URL |
| `RATE_LIMIT_WINDOW` | 60 | Rate limit window in seconds |
| `RATE_LIMIT_LOGIN` | 5 | Max login attempts per minute per IP |
| `RATE_LIMIT_API` | 60 | Max API calls per minute per user |
| `RATE_LIMIT_TOKEN_GEN` | 3 | Max token generations per minute per user |

## Profiles

| Profile | Use | Seed data | SQL logging |
|---------|-----|-----------|-------------|
| `local` | Development | Yes | Yes |
| `int` | Integration testing | Yes | No |
| `prod` | Production | No | No |

## Security Architecture

```
Browser → Login → Session Token (HMAC, 60min TTL)
                       ↓
              Guava Session Cache (15min sliding)
                       ↓
              JwtAuthFilter validates token + session
                       ↓
              RateLimitFilter checks per-user limits
                       ↓
              Controller handles request

External Service → API Token (RSA/PKCS12, 60min TTL)
                       ↓
              JwtAuthFilter validates RSA signature (no session needed)
                       ↓
              RateLimitFilter checks per-user limits
                       ↓
              Controller handles request
```
