# Real-time Fraud Alert System

A high-performance, non-blocking microservices ecosystem built with **Spring Boot 3.5**, **Java 21**, and **Project Reactor**. This system is designed to evaluate banking transactions in real-time, manage daily limits, and ensure eventual consistency using event-driven architecture.

## 🏗️ Architecture Overview

The system follows a reactive, event-driven approach to ensure low latency and high scalability, critical for banking environments.

* **Fraud Detection Service**: Core engine that validates transactions against customer limits. It implements the **Transactional Outbox Pattern** to guarantee that database updates and Kafka events are handled atomically.
* **Notification Service**: A reactive consumer that processes fraud events and simulates customer alerts.
* **Infrastructure**: Uses **PostgreSQL (R2DBC)** for reactive persistence, **Redis** for idempotency caching, and **Kafka** for asynchronous communication.



---

## 🚀 Tech Stack

* **Runtime**: Java 21 (LTS)
* **Framework**: Spring Boot 3.5.x
* **Reactive Stack**: Project Reactor (Mono/Flux), Spring WebFlux
* **Persistence**: Spring Data R2DBC (PostgreSQL)
* **Caching/Idempotency**: Spring Data Redis Reactive
* **Messaging**: Spring Kafka (Reactive Consumers/Producers)
* **Security**: Spring Security OAuth2 Resource Server (JWT)
* **Observability**: Micrometer Tracing (Brave), Prometheus, Actuator
* **Deployment**: Docker Compose, Kubernetes (Helm)

---

## ✨ Key Features & Patterns

### 1. Transactional Outbox Pattern
To avoid distributed transaction issues (2PC) and ensure data consistency between the DB and Kafka:
- Transactions and events are saved in the same local database transaction.
- A **Reactive Scheduler** polls the outbox table and publishes to Kafka, ensuring **at-least-once delivery**.

### 2. Distributed Idempotency
Prevents duplicate processing of the same transaction ID using **Redis**:
- Before processing, the service checks Redis for a cached response.
- Successful responses are cached with a configurable TTL (default 24h).

### 3. Reactive Resilience
- **Non-blocking I/O**: Every layer from the Controller to the Database Driver (R2DBC) is non-blocking.
- **Kafka Retries & DLQ**: The Notification service implements a `DefaultErrorHandler` with a `DeadLetterPublishingRecoverer` for robust error handling.

### 4. Observability
Integrated **Trace IDs** across microservices:
- The `X-Trace-ID` is propagated through headers.
- Logs include trace context via MDC (bridged with Reactor Context).

---

## 🛠️ Getting Started

### Prerequisites
- Docker & Docker Compose
- Java 21 (JDK)
- Maven 3.9+

### Running with Docker Compose
1. Build the applications:
    ```bash
    mvn clean package -DskipTests
    ```
2. Start the infrastructure and services:
    ```bash
    docker-compose up -d
    ```

### Running on Kubernetes (Helm)
1. Deployment manifests are located in the `/banking-system-chart` directory.
    ```bash
    helm install banking-app ./banking-system-chart -n banking-ns
    ```

---

## 📂 API Reference

### Process Transaction
`POST /api/v1/fraud/process`

**Request Body:**
```json
{
  "transactionId": "TXN-2026-001",
  "customerId": "CUST-001",
  "accountId": "ACC-101",
  "amount": 550.75,
  "currency": "USD",
  "operationType": "DEBIT",
  "merchantId": "MERC-99",
  "merchantName": "Amazon",
  "mcc": "5411",
  "channel": "WEB",
  "ipAddress": "192.168.1.1"
}

```

### Responses:

* **200 OK**: Transaction evaluated successfully (Status: APPROVED or REJECTED).
* **400 Bad Request**: Validation errors, daily limit exceeded, or invalid input data.
* **401 Unauthorized**: Missing, expired, or invalid JWT Bearer Token.
* **500 Internal Server Error**: Technical failure (Database timeout or Kafka connection issue).

---

## 👤 Author

**Julio Chacon** - *Senior Backend Java Developer*
Expertise in Banking Systems, Reactive Architecture, and Cloud-Native Solutions.