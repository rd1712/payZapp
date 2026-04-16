# PayZapp — Digital Wallet & Payments Platform

A production-grade microservices-based digital wallet and payments platform built with Java Spring Boot. Implements real-world fintech patterns including double-entry ledger, idempotency, Saga orchestration, event-driven architecture, and CQRS.

---

## Architecture

```
                        ┌─────────────────┐
                        │   API Gateway    │
                        │  (Port 8080)     │
                        │  Spring Cloud    │
                        └───────┬─────────┘
                    ┌───────────┼───────────┐
                    ▼           ▼           ▼
            ┌──────────┐ ┌──────────┐ ┌──────────┐
            │  User    │ │  Wallet  │ │ Payment  │
            │ Service  │ │ Service  │ │ Service  │
            │ :8081    │ │ :8082    │ │ :8083    │
            └──────────┘ └────┬─────┘ └────┬─────┘
                              │            │
                    ┌─────────▼────────────▼──────┐
                    │      Kafka Event Bus         │
                    └──┬──────────┬───────────┬────┘
                       ▼          ▼           ▼
               ┌────────────┐ ┌────────┐ ┌──────────┐
               │Notification│ │ Fraud  │ │Reporting │
               │  Service   │ │Detect. │ │ Service  │
               │   :8084    │ │ :8085  │ │  :8086   │
               └────────────┘ └────────┘ └──────────┘

    Infrastructure: Eureka :8761 | Zipkin :9411 | PostgreSQL :5432 | Kafka :9092
```

---

## Services

| Service | Port | Description |
|---|---|---|
| API Gateway | 8080 | Single entry point, routing via Eureka |
| User Service | 8081 | Registration, login, JWT authentication |
| Wallet Service | 8082 | Double-entry ledger, debit/credit, idempotency |
| Payment Service | 8083 | Saga orchestration, state machine |
| Notification Service | 8084 | Kafka consumer, payment confirmations |
| Fraud Detection | 8085 | Rules engine, wallet freezing |
| Reporting Service | 8086 | CQRS, transaction history |
| Eureka Server | 8761 | Service discovery |

---

## Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.2.1
- **Security:** Spring Security + JWT (JJWT)
- **Database:** PostgreSQL (database-per-service)
- **Migrations:** Flyway
- **Messaging:** Apache Kafka
- **Service Discovery:** Netflix Eureka
- **API Gateway:** Spring Cloud Gateway (reactive)
- **Inter-service Communication:** OpenFeign + Spring Cloud LoadBalancer
- **Tracing:** Zipkin + Micrometer
- **Build:** Maven (multi-module)
- **Infrastructure:** Docker Compose

---

## Key Design Patterns

### Double-Entry Ledger
Every wallet transaction creates two ledger entries — a debit and a corresponding credit. Balance is always provable from the ledger history. Prevents data loss on partial failures.

### Idempotency Keys
Every payment and wallet operation accepts an idempotency key. Duplicate requests return the cached response without reprocessing. Prevents double-charging on network retries.

### Optimistic Locking
Wallet entities use `@Version` for optimistic locking. Concurrent debit attempts on the same wallet detect conflicts via version mismatch and retry with fresh data. No database locks held — high throughput maintained.

### Saga Orchestration
Payment Service orchestrates distributed transactions across services:
1. Debit sender wallet → AUTHORIZED
2. Credit receiver wallet → CAPTURED
3. Settle → SETTLED

On failure — compensating transactions run in reverse order to restore consistency.

### Payment State Machine
Payments follow strict state transitions:
```
INITIATED → AUTHORIZED → CAPTURED → SETTLED
Any state → FAILED
SETTLED → REFUNDED
```
Invalid transitions throw exceptions — prevents invalid payment states.

### CQRS
Payment Service owns the write model. Reporting Service maintains a separate read-optimized `transaction_records` table built from Kafka events. Analytical queries never impact payment processing.

### Rules Engine
Fraud Detection implements the Strategy pattern — each fraud rule implements `FraudRule` interface. New rules are added by creating new classes. Zero changes to existing code (Open/Closed Principle).

---

## Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop

---

## Running Locally

### 1. Start Infrastructure

```bash
docker-compose up -d
```

Starts PostgreSQL, Kafka, ZooKeeper, and Zipkin.

### 2. Create Databases

```bash
docker exec -it payzapp-postgres psql -U postgres -c "CREATE DATABASE userdb;"
docker exec -it payzapp-postgres psql -U postgres -c "CREATE DATABASE walletdb;"
docker exec -it payzapp-postgres psql -U postgres -c "CREATE DATABASE paymentdb;"
docker exec -it payzapp-postgres psql -U postgres -c "CREATE DATABASE reportingdb;"
```

### 3. Build All Modules

```bash
mvn install -DskipTests
```

### 4. Start Services (in order)

```bash
# Terminal 1 - Eureka Server
cd eureka-server && mvn spring-boot:run

# Terminal 2 - User Service
cd user-service && mvn spring-boot:run

# Terminal 3 - Wallet Service
cd wallet-service && mvn spring-boot:run

# Terminal 4 - Payment Service
cd payment-service && mvn spring-boot:run

# Terminal 5 - Notification Service
cd notification-service && mvn spring-boot:run

# Terminal 6 - Fraud Detection
cd fraud-detection && mvn spring-boot:run

# Terminal 7 - Reporting Service
cd reporting-service && mvn spring-boot:run

# Terminal 8 - API Gateway
cd api-gateway && mvn spring-boot:run
```

---

## API Reference

All requests go through the API Gateway at `http://localhost:8080`.

### Authentication

**Register:**
```bash
POST /api/auth/register
{
  "email": "user@example.com",
  "userName": "username",
  "firstName": "First",
  "lastName": "Last",
  "password": "password123",
  "phoneNumber": "9999999999"
}
```

**Login:**
```bash
POST /api/auth/login
{
  "email": "user@example.com",
  "password": "password123"
}
```
Returns JWT token. Include in all subsequent requests:
```
Authorization: Bearer <token>
```

### Wallet

**Create Wallet:**
```bash
POST /api/wallet/create
Authorization: Bearer <token>
```

**Check Balance:**
```bash
GET /api/wallet/balance
Authorization: Bearer <token>
```

**Credit Wallet:**
```bash
POST /api/wallet/credit
Authorization: Bearer <token>
{
  "walletId": "uuid",
  "amount": 1000,
  "idempotencyKey": "unique-key"
}
```

**Debit Wallet:**
```bash
POST /api/wallet/debit
Authorization: Bearer <token>
{
  "walletId": "uuid",
  "amount": 500,
  "idempotencyKey": "unique-key"
}
```

### Payments

**Initiate Payment:**
```bash
POST /api/payment/initiate
Authorization: Bearer <token>
{
  "fromWalletId": "uuid",
  "toWalletId": "uuid",
  "amount": 100,
  "idempotencyKey": "unique-key"
}
```

### Reporting

**All Transactions:**
```bash
GET /api/reports/transactions
Authorization: Bearer <token>
```

**Transactions by Wallet:**
```bash
GET /api/reports/transactions/{walletId}
Authorization: Bearer <token>
```

---

## Monitoring

| Tool | URL | Purpose |
|---|---|---|
| Eureka Dashboard | http://localhost:8761 | Service registry |
| Zipkin UI | http://localhost:9411 | Distributed tracing |

---

## Design Decisions

### Why Microservices?
Each service scales independently. Payment Service can scale to 10x without scaling Reporting. Independent deployments reduce risk. Kafka decouples services — Notification Service outage doesn't affect payments.

### Why Kafka over REST for Notifications?
Notifications are non-critical and async. REST would add latency to the payment response. Kafka events survive service failures — notifications are eventually delivered even if the service was down.

### Why Optimistic over Pessimistic Locking?
Pessimistic locking holds database connections while waiting for row locks. At scale, connection pool exhausts. Optimistic locking never blocks — detects conflicts only when they occur (rare in practice) and retries.

### Why Saga over 2PC?
2PC requires all participants to hold locks simultaneously. In distributed systems, this creates deadlock risks and availability issues. Saga uses local transactions with compensating actions — no cross-service locks, better fault tolerance.

### Why CQRS for Reporting?
Analytics queries running on the transactional database compete with payment processing. Separate read models allow query optimization without impacting write performance. Eventual consistency is acceptable for dashboards.

---

## Author

Rohit Deshmukh  

