# AI-Pass LiveSync Engine

A production-ready event-driven integration layer that receives external events, processes them asynchronously through configurable workflows, and exposes structured results with full monitoring and observability.

## Live Deployment

| Service | URL |
|---------|-----|
| **Backend API** | `https://YOUR_RENDER_URL` |
| **Swagger UI** | `https://YOUR_RENDER_URL/swagger-ui.html` |
| **Health Check** | `https://YOUR_RENDER_URL/api/health` |
| **Prometheus Metrics** | `https://YOUR_RENDER_URL/actuator/prometheus` |
| **Dashboard** | `https://YOUR_FRONTEND_URL` |

---

## Architecture Overview

```
                    ┌──────────────────────────────────────────────────────┐
                    │                  External Systems                    │
                    │  Invoice System · Supplier Portal · HR · Monitoring  │
                    └──────────────┬───────────────────────────────────────┘
                                   │ POST /api/events/webhook
                                   │ (+ optional HMAC-SHA256 auth)
                                   ▼
                    ┌──────────────────────────────────────────────────────┐
                    │                   API Layer                          │
                    │  WebhookController → validates → persists → queues   │
                    │  EventController   → query events (paginated)        │
                    │  HealthController  → status, queue depth, metrics    │
                    │  LogController     → processing audit logs           │
                    └──────────────┬───────────────────────────────────────┘
                                   │
                    ┌──────────────▼───────────────────────────────────────┐
                    │              Redis Queue (LPUSH/RPOP)                │
                    │         Falls back to in-memory if unavailable       │
                    └──────────────┬───────────────────────────────────────┘
                                   │
                    ┌──────────────▼───────────────────────────────────────┐
                    │            Async Event Worker (polled @1s)           │
                    │                                                      │
                    │  ┌─────────────────────────────────────────────────┐ │
                    │  │         Workflow Engine (Strategy Pattern)       │ │
                    │  │                                                 │ │
                    │  │  invoice.uploaded  → InvoiceReviewHandler       │ │
                    │  │  supplier.updated  → SupplierEvaluationHandler  │ │
                    │  │  hr.onboarding     → HROnboardingHandler       │ │
                    │  │  anomaly.alert     → AnomalyAlertHandler       │ │
                    │  └─────────────────────────────────────────────────┘ │
                    │                                                      │
                    │  RetryScheduler → exponential backoff (30s poll)     │
                    └──────────────┬───────────────────────────────────────┘
                                   │
                    ┌──────────────▼───────────────────────────────────────┐
                    │            PostgreSQL + Redis                        │
                    │  Events · EventLogs · Queue · Metrics               │
                    └─────────────────────────────────────────────────────┘
```

### Event Lifecycle

```
PENDING → PROCESSING → PROCESSED (with workflow result)
                     ↘ FAILED → retry (x3 with backoff) → DEAD_LETTER
```

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Backend | Java 17 + Spring Boot 4 |
| Database | PostgreSQL (Neon) |
| Queue | Redis (with in-memory fallback) |
| Metrics | Spring Actuator + Micrometer + Prometheus |
| API Docs | springdoc-openapi (Swagger UI) |
| Dashboard | React 19 + Vite |
| Container | Docker + docker-compose |
| Orchestration | Kubernetes manifests |
| Deployment | Render.com |

---

## API Endpoints

### Core

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/events/webhook` | Ingest an event (returns 202 Accepted) |
| `GET` | `/api/events` | List events (paginated) |
| `GET` | `/api/events/{id}` | Get event by ID with workflow result |
| `GET` | `/api/health` | Service health, queue, uptime, metrics |
| `GET` | `/api/logs` | Processing logs (filterable by `?eventId=`) |

### Bonus

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/events/{id}/replay` | Replay a processed/failed event |
| `GET` | `/actuator/prometheus` | Prometheus metrics scrape endpoint |
| `GET` | `/swagger-ui.html` | Interactive API documentation |

---

## Quick Start

### Using curl

**Ingest an invoice event:**

```bash
curl -X POST http://localhost:8080/api/events/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "invoice.uploaded",
    "source": "invoice-system",
    "payload": {
      "invoice_id": "INV-1001",
      "amount": 1200,
      "vendor": "Acme Corp"
    }
  }'
```

**Ingest a supplier update:**

```bash
curl -X POST http://localhost:8080/api/events/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "supplier.updated",
    "source": "supplier-portal",
    "payload": {
      "supplier_id": "SUP-501",
      "status": "active",
      "rating": 4.5
    }
  }'
```

**Ingest an HR onboarding event:**

```bash
curl -X POST http://localhost:8080/api/events/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "hr.onboarding",
    "source": "hr-system",
    "payload": {
      "employee_name": "Alice Johnson",
      "department": "Engineering",
      "start_date": "2026-06-01"
    }
  }'
```

**Ingest an anomaly alert:**

```bash
curl -X POST http://localhost:8080/api/events/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "anomaly.alert",
    "source": "monitoring-system",
    "payload": {
      "anomaly_type": "transaction_spike",
      "severity": "critical",
      "confidence": 0.95
    }
  }'
```

**Check health:**

```bash
curl http://localhost:8080/api/health
```

**List events:**

```bash
curl http://localhost:8080/api/events?page=0&size=10
```

**Get specific event:**

```bash
curl http://localhost:8080/api/events/{id}
```

**Replay an event:**

```bash
curl -X POST http://localhost:8080/api/events/{id}/replay
```

**View logs for an event:**

```bash
curl http://localhost:8080/api/logs?eventId={id}
```

---

## Local Development Setup

### Prerequisites

- Java 17+
- Gradle 9+ (included via wrapper)
- Node.js 20+ (for dashboard)
- Docker + Docker Compose (optional)

### Option 1: Docker Compose (recommended)

```bash
docker compose up --build
```

Services:
- Backend: http://localhost:8080
- Dashboard: http://localhost:3000
- PostgreSQL: localhost:5432
- Redis: localhost:6379

### Option 2: Manual

```bash
# Backend (uses H2 in-memory by default)
cd backend
./gradlew bootRun

# Dashboard
cd frontend
npm install
npm run dev
```

### Option 3: With external database

```bash
# Copy and configure environment
cp .env.example .env
# Edit .env with your database credentials

# Run with environment
cd backend
source ../.env  # or set-env on Windows
./gradlew bootRun
```

---

## Queue Approach

The system uses **Redis Lists** (`LPUSH`/`RPOP`) as the primary event queue:

1. When a webhook arrives, the event is persisted to PostgreSQL with `PENDING` status
2. The event ID is pushed to a Redis list (`aipass:event-queue`)
3. A scheduled worker polls every 1 second, pops event IDs, and processes them
4. If Redis is unavailable at startup, the system falls back to a `ConcurrentLinkedQueue` in-memory

This approach ensures:
- **Non-blocking ingestion**: webhook returns 202 immediately
- **Persistence**: events survive restarts (stored in PostgreSQL)
- **Graceful degradation**: works without Redis
- **Scalability**: multiple workers can consume from the same Redis queue

---

## Workflow Logic

Each event type maps to a dedicated handler via the **Strategy Pattern**:

| Event Type | Workflow | Logic |
|-----------|---------|-------|
| `invoice.uploaded` | Invoice Review | Amount <= 5000 → PASS, else REVIEW |
| `supplier.updated` | Supplier Evaluation | Active + rating >= 3.0 → PASS, rating < 2.0 → REJECT, else REVIEW |
| `hr.onboarding` | HR Onboarding | All required fields present → PASS, else REVIEW |
| `anomaly.alert` | Anomaly Alert | Critical/confidence >= 0.9 → FLAG, high → REVIEW, else PASS |

Adding a new workflow requires only implementing the `WorkflowHandler` interface and annotating with `@Component`. No changes to existing code.

---

## Deployment Approach

**Platform:** Render.com

**Services:**
- **Web Service** (backend) — Docker-based, auto-deployed from GitHub
- **Static Site** (dashboard) — React SPA, built from `frontend/`
- **PostgreSQL** — Managed database (or Neon external)
- **Redis** — Managed Redis instance

**Configuration:** All secrets and configuration via environment variables (12-factor app).

---

## What Is Mocked

- **External systems** — No actual invoice/supplier/HR systems. Events are simulated via the webhook endpoint or seed data.
- **Workflow decisions** — Based on simple threshold logic rather than ML models.
- **Seed data** — 8 sample events auto-loaded on startup to demonstrate the full lifecycle.

---

## Bonus Features Implemented

- **Kubernetes Deployment** — Full K8s manifests in `k8s/` (deployment, service, configmap, secrets, ingress, Redis, PostgreSQL with PVC)
- **Retry Logic** — Exponential backoff (10s, 20s, 40s) with max 3 retries before dead-lettering
- **Dashboard UI** — React SPA with real-time event list, health metrics, log viewer, and event replay
- **Metrics Visualization** — Prometheus-compatible metrics at `/actuator/prometheus` with custom counters and timers
- **Webhook Authentication** — HMAC-SHA256 signature verification via `X-Webhook-Signature` header (configurable)
- **Event Replay** — `POST /api/events/{id}/replay` to re-process any event

---

## Improvements for Next Version

1. **Message broker** — Replace Redis Lists with RabbitMQ or Kafka for guaranteed delivery, consumer groups, and dead-letter queues
2. **Event schema registry** — Validate event payloads against schemas (Avro/JSON Schema) for contract enforcement
3. **Distributed tracing** — Add OpenTelemetry for end-to-end request tracing across services
4. **Rate limiting** — Protect the webhook endpoint from DDoS/abuse with token bucket rate limiting
5. **Circuit breaker** — Wrap external integrations with Resilience4j circuit breakers
6. **Event versioning** — Support payload schema evolution without breaking existing workflows
7. **Multi-tenancy** — Namespace events by tenant/organization for SaaS deployment
8. **CQRS** — Separate read/write models for better query performance at scale
9. **WebSocket** — Push real-time event updates to the dashboard instead of polling
10. **Horizontal scaling** — Consumer group support so multiple backend instances process events in parallel without duplication

---

## Project Structure

```
aipass-integ-livesync-layer/
├── backend/
│   ├── src/main/java/aipasslivesync/backend/
│   │   ├── config/          # Async, Redis, CORS, Swagger, Seed Data
│   │   ├── controller/      # REST endpoints
│   │   ├── dto/             # Request/Response records
│   │   ├── entity/          # JPA entities
│   │   ├── enums/           # EventType, EventStatus, etc.
│   │   ├── exception/       # Global error handling
│   │   ├── repository/      # Spring Data JPA
│   │   ├── scheduler/       # Retry logic
│   │   ├── security/        # HMAC webhook auth
│   │   └── service/         # Business logic + workflow handlers
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── src/test/java/       # Integration tests
│   ├── Dockerfile
│   └── build.gradle
├── frontend/
│   ├── src/                 # React dashboard
│   ├── Dockerfile
│   └── package.json
├── k8s/                     # Kubernetes manifests
├── docker-compose.yml
├── .env.example
└── README.md
```
