# PayZapp — Learning Guide
> A comprehensive guide for someone learning Spring Boot Microservices from scratch.
> Updated after each session. Use this as your revision reference.

---

## Table of Contents
1. [Build Tools — Maven](#1-build-tools--maven)
2. [Multi-Module Maven Project](#2-multi-module-maven-project)
3. [Docker & Docker Compose](#3-docker--docker-compose)
4. [Spring Boot Fundamentals](#4-spring-boot-fundamentals)
5. [JPA & Hibernate](#5-jpa--hibernate)
6. [Flyway — Database Migrations](#6-flyway--database-migrations)
7. [Spring Security](#7-spring-security)
8. [JWT vs Sessions](#8-jwt-vs-sessions)
9. [BCrypt Password Hashing](#9-bcrypt-password-hashing)
10. [Repository Pattern](#10-repository-pattern)
11. [DTOs — Data Transfer Objects](#11-dtos--data-transfer-objects)
12. [Interview Arsenal](#12-interview-arsenal)

---

## 1. Build Tools — Maven

### What is Maven?
At ServiceNow, the platform handled everything for you — dependencies, builds, deployments. In the real world with Java, you manage this yourself. Maven is the tool that does it.

Maven does two things:
1. **Dependency management** — you declare what libraries you need, Maven downloads them automatically
2. **Build lifecycle** — compiles your code, runs tests, packages into a runnable JAR

### How it works
Every Maven project has one file: `pom.xml` (Project Object Model). It's XML that describes your project.

Instead of manually downloading Spring Boot's JAR and all its 50+ dependencies, you write:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.2.1</version>
</dependency>
```

Maven reads this, downloads Spring Boot and all its transitive dependencies automatically.

### Key Maven commands
```bash
mvn compile          # compile all .java files
mvn test             # run tests
mvn package          # compile + test + create JAR
mvn spring-boot:run  # compile + run the Spring Boot app
mvn validate         # validate pom.xml structure
```

### The JAR file
When you run `mvn package`, Maven produces a single `user-service.jar`. This JAR contains your compiled code + all dependencies + embedded Tomcat. You run it with:
```bash
java -jar user-service.jar
```
No need to install Tomcat separately — Spring Boot embeds it.

### Interview Answer
*"Maven is our build tool. It handles dependency management through `pom.xml` — we declare what libraries we need and Maven resolves and downloads them. It also manages the build lifecycle — `mvn package` compiles code, runs tests, and produces a runnable JAR with an embedded Tomcat server."*

---

## 2. Multi-Module Maven Project

### The Problem
PayZapp has 7 microservices. Without a multi-module setup, each service has its own `pom.xml` declaring its own dependency versions. When Spring Boot releases a security patch, you update 7 files. Miss one — security vulnerability in production.

### The Solution — Parent POM
One parent `pom.xml` at the root. All child services inherit from it.

```
payZapp/
├── pom.xml                  ← parent (coordinator)
├── common/
│   └── pom.xml              ← child
├── user-service/
│   └── pom.xml              ← child
├── wallet-service/
│   └── pom.xml              ← child
└── ... (5 more services)
```

### Parent pom.xml — 5 key sections

```xml
<!-- 1. Project identifiers -->
<groupId>com.payzapp</groupId>
<artifactId>payzapp-parent</artifactId>
<version>1.0-SNAPSHOT</version>

<!-- 2. Tells Maven this produces nothing, just a coordinator -->
<packaging>pom</packaging>

<!-- 3. Lists all child modules -->
<modules>
    <module>common</module>
    <module>user-service</module>
    <!-- ... -->
</modules>

<!-- 4. Version declarations ONLY — doesn't add dependencies -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>  <!-- imports Spring Boot's BOM -->
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 5. Actually added to ALL children automatically -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Critical Distinction — `dependencyManagement` vs `dependencies`

| | `dependencyManagement` | `dependencies` |
|---|---|---|
| What it does | Declares versions only | Actually adds to classpath |
| Children get it automatically? | ❌ No | ✅ Yes |
| Children need to declare it? | ✅ Yes (without version) | ❌ No |

**Memory hook:** `dependencyManagement` manages versions. `dependencies` manages what you actually use.

### What is a BOM?
BOM = Bill of Materials. A special POM file published by Spring Boot containing pre-tested compatible versions for 200+ libraries.

When you import the Spring Boot BOM with `scope=import`, you pull in all those version declarations. Children can then use any Spring Boot library without specifying a version — Spring Boot guarantees compatibility.

### Child pom.xml
```xml
<parent>
    <groupId>com.payzapp</groupId>
    <artifactId>payzapp-parent</artifactId>
    <version>1.0-SNAPSHOT</version>
</parent>

<artifactId>user-service</artifactId>
<packaging>jar</packaging>  <!-- produces a runnable JAR -->

<dependencies>
    <!-- No version needed — inherited from parent BOM -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

### Dependency Scopes
| Scope | Compile time | Runtime | In final JAR |
|---|---|---|---|
| (none) | ✅ | ✅ | ✅ |
| `runtime` | ❌ | ✅ | ✅ |
| `test` | ✅ | ✅ | ❌ |
| `provided` | ✅ | ❌ | ❌ |

- `runtime` — PostgreSQL driver (not needed to compile, needed when running)
- `test` — JUnit (only for testing, never in production JAR)
- `provided` — Lombok (generates code at compile time, not needed at runtime)

### Package Naming Convention
`com.payzapp.userservice` — reverse domain name convention.
- `com.payzapp` — your company/project (reverse of payzapp.com)
- `userservice` — the specific module

This guarantees global uniqueness. `com.google.gson` vs `com.payzapp.gson` — no conflict.

### Interview Answers

**"How do you manage dependency versions across multiple microservices?"**
*"We use a multi-module Maven project with a parent `pom.xml`. All dependency versions are declared once in the parent's `dependencyManagement` block using Spring Boot's BOM import. Child services inherit these versions automatically — they declare which dependencies they need but never specify versions. Upgrading Spring Boot means changing one line in the parent."*

**"What is a BOM and how does it work?"**
*"BOM stands for Bill of Materials. It's a special Maven POM file containing pre-tested compatible versions for hundreds of libraries. We import Spring Boot's BOM using `scope=import` in `dependencyManagement`. After that, any child service can declare Spring Boot dependencies without versions — they get the version Spring Boot guarantees to be compatible."*

**"What's the difference between `dependencyManagement` and `dependencies`?"**
*"`dependencyManagement` only declares versions — it never adds dependencies to any module's classpath. Child modules must still explicitly declare what they need. `dependencies` actually adds the library to the classpath. One-liner: `dependencyManagement` manages versions, `dependencies` manages what you actually use."*

---

## 3. Docker & Docker Compose

### What is Docker?
PayZapp needs PostgreSQL, Kafka, ZooKeeper, and Zipkin running locally. Without Docker, you'd install each manually — 20-step PostgreSQL install, complex Kafka + ZooKeeper setup. Every developer does this separately. "Works on my machine" is a constant problem.

Docker packages software into **containers** — lightweight, isolated boxes that have everything needed to run. Pre-built containers exist for PostgreSQL, Kafka, everything we need.

### Container vs Virtual Machine
- **VM** — emulates an entire computer. Slow, takes GBs of RAM.
- **Container** — shares your OS kernel but isolates the process. Starts in seconds, uses much less memory.

Analogy: VM is a separate apartment with its own electricity and plumbing. Container is a room in your house — shares infrastructure but self-contained.

### Docker Image vs Container
- **Image** — a blueprint (like a Java class). PostgreSQL image is a pre-built snapshot.
- **Container** — a running instance of that image (like an object from a class).

### Port Mapping — `host:container`
```yaml
ports:
  - "5432:5432"  # Mac port 5432 → container port 5432
  - "5433:5432"  # Mac port 5433 → container port 5432
```
The container is isolated. PostgreSQL inside it listens on 5432 inside. You can't reach inside directly. Port mapping creates a tunnel from your Mac into the container.

Use `5433:5432` if you already have PostgreSQL installed locally on 5432 — avoid port conflict.

### Volumes — Persisting Data
Containers are temporary. Stop and remove PostgreSQL container → all data gone.

Volumes map a folder inside the container to your Mac's filesystem:
```yaml
volumes:
  - postgres_data:/var/lib/postgresql/data
```
Data written inside the container is actually stored on your Mac. Container can be destroyed and recreated — data survives.

### Docker Compose
Instead of starting each container manually, `docker-compose.yml` defines all infrastructure. One command starts everything:

```bash
docker-compose up -d    # start all containers in background
docker-compose down     # stop all containers
docker-compose down -v  # stop + delete volumes (wipes database)
docker ps               # list running containers
```

### PayZapp's docker-compose.yml
```yaml
services:
  postgres:
    image: postgres:15
    container_name: payzapp-postgres
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: userdb
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    container_name: payzapp-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    container_name: payzapp-kafka
    depends_on:
      - zookeeper       # start zookeeper first
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181   # service name, not localhost
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  zipkin:
    image: openzipkin/zipkin:latest
    container_name: payzapp-zipkin
    ports:
      - "9411:9411"

volumes:
  postgres_data:
```

### Why `zookeeper:2181` not `localhost:2181`?
Inside Docker Compose, containers communicate using **service names** as internal DNS. `localhost` inside Kafka container means Kafka itself. `zookeeper` resolves to the ZooKeeper container's IP automatically.

### What is ZooKeeper?
Kafka is a distributed system that can run as multiple servers (brokers). ZooKeeper manages cluster metadata — which brokers are alive, which is the leader, where data lives. You never interact with ZooKeeper directly — it just needs to be running.

Analogy: Kafka is a team of workers, ZooKeeper is the manager coordinating them.

### What is Zipkin?
Distributed tracing tool. Every request gets a Trace ID that propagates across all services. Zipkin collects timing data and visualizes the complete request journey across services. Accessible at `http://localhost:9411`.

Used to answer: "A payment takes 5 seconds — which service is the bottleneck?"

### Interview Answer

**"How do you set up local development infrastructure for microservices?"**
*"We use Docker Compose to run all infrastructure locally — PostgreSQL, Kafka, ZooKeeper, and Zipkin. Each is defined as a service in `docker-compose.yml` with appropriate port mappings. A single `docker-compose up -d` command starts everything. This ensures every developer has an identical local environment — eliminating 'works on my machine' issues."*

---

## 4. Spring Boot Fundamentals

### What is Spring Boot?
Building a payments app from scratch would require thousands of lines just for setup — database connections, HTTP server configuration, security setup. Spring Boot handles all that automatically. You focus on business logic.

### The Three Layers (MVC Pattern)
Every Spring Boot service follows this structure:

```
HTTP Request
     ↓
Controller    →  receives HTTP, sends HTTP response
     ↓
Service       →  business logic
     ↓
Repository    →  database operations
     ↓
Entity        →  maps to database table
     ↓
Database
```

**Controller** — entry point. Handles HTTP. Nothing else.
```java
@RestController
public class UserController {
    // handles POST /register
    // handles POST /login
}
```

**Service** — the brain. Business logic only. Doesn't know about HTTP or database directly.
```java
@Service
public class UserService {
    // validate email uniqueness
    // hash password
    // create user
}
```

**Repository** — talks to database. Only saves, finds, updates, deletes. No business logic.
```java
public interface UserRepository extends JpaRepository<User, UUID> {
    // save user, find by email
}
```

**Entity** — Java class that maps to a database table. One object = one row.
```java
@Entity
@Table(name = "users")
public class User { }
```

### Why Three Layers?
- Want to switch from PostgreSQL to MongoDB? Only change Repository layer.
- Want to add a mobile API? Add another Controller, reuse same Service.
- Want to unit test business logic? Mock Repository, test Service in isolation.

### @SpringBootApplication — Three Annotations in One
```java
@SpringBootApplication  // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

- **`@SpringBootConfiguration`** — marks as configuration class
- **`@EnableAutoConfiguration`** — Spring Boot looks at your dependencies and auto-configures. See PostgreSQL driver + JPA? Automatically sets up database connection. See Spring Security? Automatically secures all endpoints.
- **`@ComponentScan`** — scans all sub-packages for `@Service`, `@Repository`, `@Controller` and registers them as beans.

### What is a Bean?
A bean is an object that Spring creates and manages. Instead of `new UserService()`, Spring creates it and injects it where needed. You declare beans with `@Bean`, `@Service`, `@Repository`, `@Controller`, etc.

### Convention over Configuration
Spring Boot's core philosophy — follow standard conventions and things just work. Standard folder structure (`src/main/java`), standard annotations, standard naming — no manual wiring needed.

### application.yml
Every service has `src/main/resources/application.yml` — configuration file:

```yaml
server:
  port: 8081                # which port this service listens on

spring:
  application:
    name: user-service      # service name for Eureka registration
  datasource:
    url: jdbc:postgresql://localhost:5432/userdb
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate    # validate schema, never modify

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/  # where to register
```

### PayZapp Port Assignments
| Service | Port |
|---|---|
| api-gateway | 8080 |
| user-service | 8081 |
| wallet-service | 8082 |
| payment-service | 8083 |
| notification-service | 8084 |
| fraud-detection | 8085 |
| reporting-service | 8086 |
| eureka | 8761 |

### Dependency Injection
Two ways to inject dependencies in Spring:

**❌ Field injection (discouraged):**
```java
@Autowired
private UserRepository userRepository;
```

**✅ Constructor injection (preferred):**
```java
@Service
@RequiredArgsConstructor  // Lombok generates constructor for final fields
public class UserService {
    private final UserRepository userRepository;  // final = immutable after injection
    private final PasswordEncoder passwordEncoder;
}
```

Constructor injection is preferred because:
1. Fields can be `final` — guaranteed never to change after injection
2. Makes dependencies explicit and visible
3. Easier to unit test — just call the constructor with mocks

---

## 5. JPA & Hibernate

### What is ORM?
ORM = Object Relational Mapping. Converts between Java objects and database rows automatically.

Without ORM:
```java
String sql = "INSERT INTO users (id, name, email) VALUES (?, ?, ?)";
PreparedStatement stmt = connection.prepareStatement(sql);
stmt.setString(1, user.getId().toString());
stmt.setString(2, user.getName());
stmt.setString(3, user.getEmail());
stmt.executeUpdate();
```

With JPA/Hibernate:
```java
userRepository.save(user);  // Hibernate generates and runs the SQL
```

### JPA vs Hibernate
- **JPA** — a specification (set of rules and interfaces). Defines annotations like `@Entity`, `@Table`, `@Column`.
- **Hibernate** — the most popular implementation of JPA. Spring Boot uses it by default.

Analogy: JPA is a job description, Hibernate is the employee doing the work. You code to JPA interfaces, Hibernate executes under the hood.

### Key JPA Annotations

```java
@Entity                          // this class maps to a database table
@Table(name = "users")           // explicit table name (default = class name)
@EntityListeners(AuditingEntityListener.class)  // enables auto timestamps
@Getter @Setter                  // Lombok — generates getters/setters
@NoArgsConstructor               // Lombok — required by JPA to instantiate entities
@AllArgsConstructor              // Lombok — constructor with all fields
@Builder                         // Lombok — builder pattern for creating objects
public class User {

    @Id                                              // primary key
    @GeneratedValue(strategy = GenerationType.UUID)  // auto-generate UUID
    private UUID userId;

    @Column(nullable = false)                        // NOT NULL in database
    private String firstName;

    @Column(nullable = false, unique = true)         // NOT NULL + UNIQUE
    private String email;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)                     // store enum as "ACTIVE" not 0
    private AccountStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)     // set once, never update
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

### Why `updatable = false` on `createdAt`?
`createdAt` should never change after a record is created. `updatable = false` tells Hibernate — never include this column in UPDATE statements.

### JPA Auditing
Instead of manually setting `createdAt = LocalDateTime.now()` everywhere, Spring Data JPA can set these automatically:

1. Add `@EntityListeners(AuditingEntityListener.class)` on entity
2. Add `@CreatedDate` and `@LastModifiedDate` on fields
3. Add `@EnableJpaAuditing` on your main application class

Spring automatically sets these timestamps on create and update.

### `ddl-auto` settings
| Setting | Behavior | Use when |
|---|---|---|
| `create` | Drop and recreate tables on startup | Never in production |
| `create-drop` | Create on startup, drop on shutdown | Testing |
| `update` | Add missing columns, never drop | Dangerous in production |
| `validate` | Verify schema matches entities, fail if mismatch | Production |
| `none` | Do nothing | When using Flyway |

**We use `validate`** — Flyway creates the schema, Hibernate only validates it matches our entities.

### Builder Pattern
```java
// Without builder (unreadable with 10 fields)
User user = new User(uuid, "Rohit", "Deshmukh", "rohit@gmail.com", hashedPwd, "9999999999", AccountStatus.INACTIVE, now, now);

// With builder (readable, named fields)
User user = User.builder()
    .firstName("Rohit")
    .lastName("Deshmukh")
    .email("rohit@gmail.com")
    .passwordHash(hashedPassword)
    .status(AccountStatus.INACTIVE)
    .build();
```

### Jakarta vs Javax
Spring Boot 3.x uses `jakarta.persistence.*` instead of old `javax.persistence.*`. Same APIs, renamed due to Oracle trademark issues when Java EE moved to Eclipse Foundation.

If you see old tutorials using `javax.persistence` — that's `jakarta.persistence` in Spring Boot 3.x.

---

## 6. Flyway — Database Migrations

### The Problem
Your application evolves — new features require schema changes. Without version control for your database:
- Week 1: create `users` table
- Week 3: add `phone_number` column
- New developer joins: their database is missing `phone_number`, app crashes

### How Flyway Works
Flyway creates a `flyway_schema_history` table that tracks every script that's been run:

| version | description | executed_on |
|---|---|---|
| 1 | create users table | 2024-01-01 |
| 2 | add phone number | 2024-01-15 |

On startup, Flyway:
1. Looks at all scripts in `src/main/resources/db/migration`
2. Checks which ones already ran in `flyway_schema_history`
3. Runs only new ones in order

New developer clones repo → runs app → Flyway runs V1, V2, V3 in order → identical schema.

### Naming Convention
```
V{version}__{description}.sql
```
- `V1__create_users_table.sql`
- `V2__add_phone_number_to_users.sql`
- `V3__create_wallets_table.sql`

Two underscores is required — Flyway's convention. `V1` runs before `V2`, always.

### The Golden Rule — Never Modify a Run Migration
Flyway checksums every script. If you modify `V1__create_users_table.sql` after it ran, Flyway detects the checksum mismatch and refuses to start.

**In production:** Create a new migration to fix mistakes.
```sql
-- V2__fix_email_column_name.sql
ALTER TABLE users RENAME COLUMN email_id TO email;
```

**In local development only:** Wipe database and re-run.
```bash
docker-compose down -v  # -v removes volumes (destroys all data)
docker-compose up -d
```

### Example Migration
```sql
-- V1__create_users_table.sql
CREATE TABLE users
(
    user_id       UUID         PRIMARY KEY,
    first_name    VARCHAR(255) NOT NULL,
    last_name     VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone_number  VARCHAR(20)  NOT NULL UNIQUE,
    status        VARCHAR(50)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL
);
```

### Flyway vs Hibernate Auto-DDL
| | Flyway | Hibernate `ddl-auto: create` |
|---|---|---|
| Version controlled | ✅ Yes | ❌ No |
| Auditable | ✅ Yes | ❌ No |
| Safe in production | ✅ Yes | ❌ Dangerous |
| Can drop tables accidentally | ❌ Never | ✅ Yes |

### Interview Answer

**"How do you manage database schema changes across environments?"**
*"We use Flyway for database migrations. Migration scripts are version-controlled SQL files in `src/main/resources/db/migration`, committed alongside application code. On every startup, Flyway checks `flyway_schema_history` and applies any pending migrations in order. This gives us auditable, repeatable schema changes — every environment runs the exact same scripts in the same order. We never modify existing migrations; instead we create new ones to fix mistakes, preserving the complete history."*

---

## 7. Spring Security

### Default Behavior
Adding `spring-boot-starter-security` to `pom.xml` automatically protects ALL endpoints. Every request is intercepted before reaching controllers. Unauthenticated requests get redirected to an auto-generated login page.

### Security Filter Chain
Every HTTP request passes through a chain of filters before reaching your Controller:

```
Request
   ↓
Filter: Read JWT from Authorization header
   ↓
Filter: Validate JWT signature
   ↓
Filter: Set user identity in SecurityContext
   ↓
Controller
```

We configure this chain with a `SecurityFilterChain` bean.

### SecurityConfig.java
```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### CSRF — Cross Site Request Forgery

**The attack:**
1. You log into PayZapp — browser stores session cookie
2. You visit `evil.com`
3. `evil.com` has hidden form that submits `POST /api/wallet/transfer?to=hacker&amount=10000` to PayZapp
4. Browser automatically sends your PayZapp cookie with the request
5. PayZapp thinks it's you — transfers money

**CSRF protection:** Server generates a secret token. Every request must include it. Malicious sites can't know this token (browser's Same Origin Policy blocks cross-domain reads).

**Why we disable it:** PayZapp uses JWT in the `Authorization` header, not cookies. Browsers do NOT automatically attach headers to requests — only your app code does explicitly. So `evil.com` can make a request to PayZapp but cannot set the Authorization header. No JWT = rejected. CSRF attack is impossible.

### `SessionCreationPolicy.STATELESS`

**Stateful (sessions):**
- User logs in → server creates session in memory → gives client a session ID (cookie)
- Every request → client sends session ID → server looks up session → knows who you are
- Problem: 3 server instances, session only on Instance 1 → requests to Instance 2 fail

**Stateless (JWT):**
- User logs in → server creates JWT containing user data → gives to client
- Every request → client sends JWT → any server validates signature → knows who you are
- No server memory needed — any instance handles any request

`SessionCreationPolicy.STATELESS` tells Spring Security: don't create sessions, don't use cookies, validate JWT on every request.

### URL Patterns
- `/api/auth/**` — `**` matches anything: `/api/auth/register`, `/api/auth/login`, `/api/auth/anything/else`
- `/api/auth/*` — `*` matches one level only: `/api/auth/login` ✓ but NOT `/api/auth/reset/password` ✗

### Interview Answers

**"How does Spring Security work in your system?"**
*"Spring Security intercepts every HTTP request through a filter chain before it reaches our controllers. We configure a `SecurityFilterChain` bean that disables CSRF (we use JWT, not cookies), sets session management to stateless, and defines authorization rules — `/api/auth/**` endpoints are public, everything else requires a valid JWT token."*

**"Why did you disable CSRF?"**
*"We use JWT tokens in the Authorization header rather than cookies. CSRF attacks exploit the browser's automatic cookie attachment behavior. Since JWT must be explicitly set in request headers by client code, malicious sites cannot make requests on a user's behalf — they have no access to the JWT. CSRF protection is therefore unnecessary overhead."*

---

## 8. JWT vs Sessions

### Sessions (Stateful)
```
Login → server stores { session_abc: { userId: 5, email: "rohit@gmail.com" } }
      → gives client cookie: session_abc
Request → client sends cookie → server looks up session → knows who you are
```

**Problem at scale:** 10 server instances. User logs into Instance 3. Session only exists on Instance 3. Next request hits Instance 7 → "who are you?" → logged out mid-transaction.

**Use sessions when:** Single server, browser-only clients, simple low-traffic application.

### JWT (Stateless)
```
Login → server creates JWT: { userId: 5, email: "rohit@gmail.com", role: "USER", expires: 1hr }
      → signs with secret key → gives to client
Request → client sends JWT in header → any server validates signature → knows who you are
```

Server stores nothing. Any instance handles any request.

### JWT Structure
```
eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiI1In0.signature
|___________________|.|___________________|.|_________|
      Header               Payload           Signature
      (algorithm)          (user data)       (tamper-proof)
```

All three parts are Base64 encoded. The signature is generated using your secret key — if anyone tampers with the payload, the signature won't match and the token is rejected.

### JWT Lifecycle
1. User logs in with email + password
2. Server validates credentials
3. Server creates JWT with userId, roles, expiry
4. Server signs JWT with secret key
5. Client stores JWT (memory or localStorage)
6. Client sends JWT in every request: `Authorization: Bearer <token>`
7. Server validates signature on every request
8. If valid → process request. If expired/invalid → 401 Unauthorized.

### Why JWT for PayZapp
- Multiple service instances (stateless required)
- Mobile + web + third-party clients (cookies are browser-specific)
- Microservices (any service can validate JWT independently)

### Interview Answer

**"Why JWT over sessions in a microservices system?"**
*"Sessions are stateful — server stores user data in memory, creating affinity problems with load balancing across multiple instances. JWT is stateless — the token contains all user information and any service instance can validate it by checking the signature, without any shared state. This is essential in our microservices architecture where user-service might have 10 instances running simultaneously."*

---

## 9. BCrypt Password Hashing

### Encryption vs Hashing
- **Encryption** — reversible. You can decrypt. Used for: sensitive data you need to read back (credit card numbers).
- **Hashing** — one-way. Cannot reverse. Used for: passwords. You never need to "decrypt" a password — just compare hashes.

### How BCrypt Works
```
"mypassword123" + random_salt → BCrypt → "$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZA..."
```

The hash contains everything needed for verification:
```
$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
|__|__|______________________|_________________________________|
ver wf       salt (22 chars)              hash
```
- `$2a$` — BCrypt version
- `12` — work factor
- Next 22 chars — random salt (stored inside hash)
- Rest — the actual hash

### What is Salt?
Without salt, identical passwords produce identical hashes. Hacker sees two identical hashes → knows two users have same password → cracks one, cracks both.

BCrypt adds a unique random salt to each password before hashing. Same password + different salt = completely different hash. Hacker can't reuse computation across users.

### Work Factor — Being Slow is a Feature
```java
new BCryptPasswordEncoder(12)  // work factor 12 → ~250ms per hash
```

For a normal user logging in: 250ms is fine, barely noticeable.
For a hacker trying 1 billion passwords: 250ms × 1,000,000,000 = 8 years of computation.

BCrypt is intentionally slow. Increasing the work factor doubles the time per hash. As computers get faster, you increase the work factor.

### Password Verification
```java
// Login: user enters "mypassword123"
passwordEncoder.matches("mypassword123", storedHash)
// BCrypt reads salt from stored hash → hashes input with same salt → compares
```

You never store or compare plain text. Ever.

### Interview Answer

**"How do you store passwords securely?"**
*"We hash passwords with BCrypt before storing. BCrypt is a one-way cryptographic hash — it's irreversible, so even if our database is compromised, attackers can't recover plain text passwords. BCrypt incorporates a random salt unique to each user, preventing rainbow table attacks. The work factor makes brute force computationally expensive — at work factor 12, cracking 1 billion passwords would take years on modern hardware."*

---

## 10. Repository Pattern

### Spring Data JPA
You define an interface extending `JpaRepository<Entity, IDType>`. Spring generates the implementation at runtime. No SQL, no boilerplate.

```java
public interface UserRepository extends JpaRepository<User, UUID> {
    // JpaRepository gives you for free:
    // save(user), findById(uuid), deleteById(uuid), findAll(), count(), etc.
}
```

### Method Name Derivation
Spring Data JPA generates SQL from method names automatically:

```java
Optional<User> findByEmail(String email);
// → SELECT * FROM users WHERE email = ?

Optional<User> findByUsername(String username);
// → SELECT * FROM users WHERE username = ?

boolean existsByEmail(String email);
// → SELECT COUNT(*) > 0 FROM users WHERE email = ?

boolean existsByUsername(String username);
// → SELECT COUNT(*) > 0 FROM users WHERE username = ?
```

### Why `Optional<User>` not `User`?
If no user found, returning `null` is dangerous — callers can forget to check and get `NullPointerException`.

`Optional<User>` forces the caller to handle the "not found" case explicitly:
```java
Optional<User> user = userRepository.findByEmail(email);
user.orElseThrow(() -> new UserNotFoundException("User not found"));
```

### Full UserRepository
```java
package com.payzapp.userservice.repository;

import com.payzapp.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
```

Note: No `public` keyword on interface methods — interface methods are public by default.

### Interview Answer

**"How do you implement the repository layer?"**
*"We use Spring Data JPA repositories. By extending `JpaRepository`, we get standard CRUD operations for free. For custom queries, Spring Data's method name derivation generates SQL automatically — `findByEmail` becomes `SELECT * FROM users WHERE email = ?` without any manual implementation. We return `Optional<T>` from finder methods to force callers to handle the 'not found' case explicitly, preventing NullPointerExceptions."*

---

## 11. DTOs — Data Transfer Objects

### What is a DTO and Why?
DTOs are plain Java classes used to transfer data between layers or across service boundaries. You never expose your JPA entities directly in API responses.

**Why not return the entity directly?**
1. **Security** — `User` entity has `passwordHash`. Returning it exposes sensitive data.
2. **Encapsulation** — internal database structure is an implementation detail. Clients shouldn't depend on it.
3. **Flexibility** — you can change your entity structure without breaking API contracts.
4. **Control** — you decide exactly what fields the client sees.

### RegisterRequest DTO
What the client sends when registering:
```java
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class RegisterRequest {
    @Email @NotBlank
    private String email;
    @NotBlank private String username;
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @NotBlank private String password;
}
```

`@NotBlank` — field cannot be null or empty string.
`@Email` — must be a valid email format.

These annotations work with `spring-boot-starter-validation`. Controller must use `@Valid` to trigger validation.

### RegisterResponse DTO
What we send back after successful registration:
```java
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class RegisterResponse {
    private UUID userId;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private AccountStatus status;
    // NO passwordHash — never expose this
}
```

### Interview Answer

**"Why do you use DTOs instead of returning JPA entities directly?"**
*"Returning entities directly would expose internal implementation details and sensitive data. Our `User` entity contains `passwordHash` — we never want that in an API response. DTOs give us explicit control over what data crosses service boundaries. They also decouple our API contract from our database schema — we can change entity structure without breaking clients. Additionally, DTOs allow us to add request validation annotations without polluting the entity model."*

---

## 12. Interview Arsenal

### Concepts You Can Now Confidently Explain

| Concept | Can explain? | Can code? |
|---|---|---|
| Multi-module Maven project | ✅ | ✅ |
| `dependencyManagement` vs `dependencies` | ✅ | ✅ |
| BOM import | ✅ | ✅ |
| Docker containers vs VMs | ✅ | ✅ |
| Docker Compose for local dev | ✅ | ✅ |
| Spring Boot 3-layer architecture | ✅ | ✅ |
| JPA entities and annotations | ✅ | ✅ |
| Hibernate ORM | ✅ | ✅ |
| Flyway migrations | ✅ | ✅ |
| Spring Security filter chain | ✅ | ✅ |
| CSRF and why we disable it | ✅ | ✅ |
| Sessions vs JWT | ✅ | ✅ |
| Stateless authentication | ✅ | ✅ |
| BCrypt password hashing | ✅ | ✅ |
| Repository pattern with Spring Data | ✅ | ✅ |
| DTOs and why they matter | ✅ | ✅ |
| Constructor injection over field injection | ✅ | ✅ |
| Optional<T> for null safety | ✅ | ✅ |

### Top Interview Questions for Target Companies

**Razorpay / PhonePe / Stripe:**
- "How do you handle password security?" → BCrypt with salt and work factor
- "Why JWT over sessions in microservices?" → Stateless, any instance validates
- "How do you prevent SQL injection?" → JPA parameterized queries automatically
- "Walk me through a registration request end to end" → Controller → Service → Repository → DB

**Google / Amazon / Uber:**
- "How do you manage configuration across microservices?" → Spring Cloud Config (Phase 6)
- "How do you handle distributed transactions?" → Saga pattern (Phase 3)
- "What's your database migration strategy?" → Flyway with versioned scripts

---

*Last updated: Session 1 — Phase 0 complete, Phase 1 (User Service) 50% complete*

---

## 12. JWT — JSON Web Token

### What is JWT?
JWT is a Base64 encoded token with three parts separated by dots:

```
eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiI5OGI4MzA0NyJ9.xK9V3zL2mN8pQ1rT
|___________________|.|___________________|.|_________|
      Header               Payload           Signature
```

**Part 1 — Header:**
```json
{ "alg": "HS256", "typ": "JWT" }
```
Just metadata — which algorithm signs this token. Base64 encoded.

**Part 2 — Payload (Claims):**
```json
{
  "sub": "98b83047-f571-44d6-a47f-20e6bc24d518",
  "email": "rohit@gmail.com",
  "role": "USER",
  "iat": 1712068221,
  "exp": 1712154621
}
```
The actual data. `iat` = issued at, `exp` = expiry. Anyone can decode and read this — it's NOT encrypted.

**Part 3 — Signature:**
```
HMACSHA256(base64(header) + "." + base64(payload), secretKey)
```
The server signs the header + payload with a secret key. Nobody else can forge this without the key.

---

### What is HMAC?
HMAC = Hash-based Message Authentication Code.

**Problem with plain hashing:** Anyone can hash anything. If the server just hashed the payload:
1. Hacker modifies payload
2. Hacker hashes the modified payload themselves
3. Hacker replaces the signature
4. Server hashes what it received → matches → attack succeeds

**HMAC solution — add a secret key:**
```
HMAC(payload, secretKey) → signature
```
Now the hacker modifies the payload but doesn't have the secret key. They can't generate the correct signature. Server validates → doesn't match → rejected.

**One line:** Plain hash = anyone can forge. HMAC = only secret key holder can forge.

---

### What is a Claim?
A claim is just a key-value pair in the JWT payload:
```json
{
  "userId": "98b83047",    ← custom claim
  "role": "USER",          ← custom claim
  "exp": 1712154621        ← standard claim (expiry)
}
```

Two types:
- **Standard claims** — predefined: `sub` (subject), `iat` (issued at), `exp` (expiry)
- **Custom claims** — anything you add: `email`, `role`

What's safe to put in JWT:
- `userId` — just an identifier, not sensitive
- `role` — needed for authorization
- `email` — some companies include it

What NEVER goes in JWT:
- `passwordHash`
- Credit card numbers
- Any truly sensitive personal data

**Why?** JWT payload is Base64 encoded, NOT encrypted. Anyone with the token can decode and read it.

---

### JWT Lifecycle
```
1. User logs in with email + password
2. Server validates credentials
3. Server creates payload: { userId, email, role, exp }
4. Server signs it with secret key → generates signature
5. Server combines: header.payload.signature = JWT
6. Server sends JWT to client

7. Client stores JWT
8. Every request: client sends JWT in Authorization header:
   Authorization: Bearer eyJhbGci...

9. Server receives JWT
10. Server validates signature using secret key
11. If valid → reads userId from payload → knows who you are
12. If expired/invalid → 401 Unauthorized
```

---

### JwtUtil — Implementation

```java
@Component
public class JwtUtil {

    @Value("${jwt.secret}")        // reads from application.yml
    private String secret;

    @Value("${jwt.expiration}")    // 86400000 = 24 hours in milliseconds
    private long expiration;

    // Convert Base64 secret string to cryptographic Key
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Generate JWT token from User object
    public String generateToken(User user) {
        return Jwts.builder()
            .setSubject(user.getUserId().toString())   // who this token is for
            .claim("email", user.getEmail())           // custom claim
            .claim("role", "USER")                     // custom claim
            .setIssuedAt(new Date())                   // when issued
            .setExpiration(new Date(System.currentTimeMillis() + expiration))  // when expires
            .signWith(getSigningKey())                 // sign with secret
            .compact();                                // build the token string
    }

    // Validate JWT and return claims (payload)
    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)   // parseClaimsJws = signed token (JWS)
            .getBody();              // returns the payload as Claims map
    }
}
```

**Why Base64 encode the secret in application.yml?**
HMAC-SHA256 needs raw bytes, not a plain string. We store the secret as Base64 in config (safe text format), then decode it back to raw bytes before using it cryptographically.

**`parseClaimsJws` vs `parseClaimsJwt`:**
- `parseClaimsJwt` — unsigned tokens (no signature)
- `parseClaimsJws` — signed tokens (JWT with signature = JWS)
  Since we sign our tokens, we always use `parseClaimsJws`.

---

### application.yml JWT config
```yaml
jwt:
  secret: cGF5emFwcC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLWhzMjU2
  expiration: 86400000   # 24 hours in milliseconds (86400 seconds × 1000)
```

Generate Base64 secret:
```bash
echo -n "your-plain-secret-here" | base64
```

---

### Interview Answer

**"How do JWT tokens work in your system?"**
*"JWT tokens have three parts — header, payload, and signature. The header specifies the algorithm, the payload carries user claims like userId and role, and the signature is an HMAC of the header and payload using our server-side secret. When a user logs in, we generate a JWT and send it to the client. On every subsequent request, the client sends the JWT in the Authorization header. Our JWT filter validates the signature — if valid, we extract the userId from the payload and set the authenticated user in Spring Security's context. The token is stateless — we never store it server-side."*

---

## 13. JWT Filter — JwtAuthFilter

### Why do we need a JWT Filter?
Spring Security doesn't natively understand JWT. We need a filter that:
1. Intercepts every request
2. Reads JWT from the `Authorization` header
3. Validates it
4. Tells Spring Security "this user is authenticated"

Without this filter, Spring Security has no way to understand JWT tokens and rejects all requests to protected endpoints.

---

### What is OncePerRequestFilter?
`OncePerRequestFilter` is a Spring base class for filters that guarantees your filter runs **exactly once per request**.

**Why "exactly once" matters:**
Spring can forward requests internally — for example, when an error occurs, Spring forwards to `/error` internally. A plain `Filter` implementation would run twice — once for the original request, once for the internal forward.

For a security filter, running twice is problematic:
- Validates JWT twice (unnecessary overhead)
- Sets SecurityContext twice (potential issues)
- Unpredictable behavior on errors

`OncePerRequestFilter` tracks if it already ran using a request attribute. Second time it sees "already ran" → skips automatically.

---

### What is SecurityContext?
`SecurityContext` is Spring Security's thread-local storage for the current authenticated user. It lives for exactly one request's lifetime.

```
Request arrives
     ↓
JwtAuthFilter: validates token → puts "Rohit, role USER" in SecurityContext
     ↓
Controller: can read who is logged in from SecurityContext
     ↓
Service: can also read from SecurityContext if needed
     ↓
Request ends → SecurityContext cleared automatically
```

**`SecurityContextHolder` vs `SecurityContext`:**
- `SecurityContextHolder` — static utility class, the access point (like a locker room)
- `SecurityContext` — holds the Authentication object for current thread (like a locker)
- `Authentication` — the actual user details inside (like the items in the locker)

```
SecurityContextHolder → getContext() → SecurityContext → getAuthentication() → Authentication
```

---

### Complete JwtAuthFilter

```java
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,    // incoming HTTP request
            HttpServletResponse response,  // outgoing HTTP response
            FilterChain filterChain        // rest of the filter chain
    ) throws ServletException, IOException {

        // Step 1: Read Authorization header
        String authHeader = request.getHeader("Authorization");
        String token = null;

        // Step 2: Check if header exists and has Bearer token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);  // skip "Bearer " (7 chars)

            try {
                // Step 3: Validate token and get claims
                Claims claim = jwtUtil.validateToken(token);

                // Step 4: Create Spring Security authentication object
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        claim.getSubject(),   // userId as principal
                        null,                 // password (not needed after auth)
                        List.of(new SimpleGrantedAuthority("ROLE_" + claim.get("role")))
                    );

                // Step 5: Attach request details (IP, session info) for audit
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Step 6: Put authenticated user in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                // Invalid/expired token — do nothing
                // SecurityContext stays empty → Spring Security returns 401
            }
        }

        // Step 7: Always pass request to next filter
        filterChain.doFilter(request, response);
    }
}
```

**Line by line explained:**

`request.getHeader("Authorization")` — reads the Authorization header. Format: `Bearer eyJhbGci...`

`authHeader.substring(7)` — "Bearer " is exactly 7 characters. This extracts everything after it — the actual token.

`jwtUtil.validateToken(token)` — validates signature and expiry. Throws exception if invalid — caught by catch block.

`UsernamePasswordAuthenticationToken` — Spring Security's object representing an authenticated user. Three params: who (userId), credentials (null), roles.

`authentication.setDetails(...)` — attaches IP address and other request metadata. Used for audit logging.

`SecurityContextHolder.getContext().setAuthentication(authentication)` — puts the verified identity in Spring's whiteboard for this request.

`filterChain.doFilter(request, response)` — **critical**. Passes request to next filter. If you forget this, request stops here and never reaches controller.

Empty catch block — if token is invalid, we don't crash. We just don't set SecurityContext. Spring Security then sees no authenticated user and returns 401.

---

### How Spring knows to run this filter
Two things make it automatic:

1. `@Component` — Spring creates the bean on startup
2. `extends OncePerRequestFilter` — Spring Security registers it with Tomcat automatically

Plus we explicitly add it in SecurityConfig:
```java
.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
```

This tells Spring — run our filter BEFORE the default username/password filter. Our filter sets SecurityContext first; Spring's default filter sees it's done and skips.

---

## 14. Global Exception Handling

### The Problem
When `UserService` throws `RuntimeException("Email already exists")`, Spring doesn't know what HTTP status to return. The client gets an empty response or 500 Internal Server Error.

We need a way to map exceptions to proper HTTP responses globally — once, not in every controller.

### @ControllerAdvice
`@ControllerAdvice` marks a class as a global exception handler. Spring intercepts any exception thrown anywhere in the application and routes it to the matching `@ExceptionHandler` method.

```
UserService throws UserAlreadyExistException
           ↓
Spring catches it globally
           ↓
Finds @ExceptionHandler(UserAlreadyExistException.class)
           ↓
Calls handleUserAlreadyExists(ex)
           ↓
Returns 409 response to client
```

### Custom Exceptions

**UserAlreadyExistException:**
```java
public class UserAlreadyExistException extends RuntimeException {
    public UserAlreadyExistException(String message) {
        super(message);  // passes message to RuntimeException
    }
}
```

**InvalidCredentialException:**
```java
public class InvalidCredentialException extends RuntimeException {
    public InvalidCredentialException(String message) {
        super(message);
    }
}
```

Why extend `RuntimeException`? Unchecked exceptions — callers don't need to declare or catch them. Clean code.

Why `public` constructor? Other classes need to instantiate it — `new UserAlreadyExistException(...)`.

### GlobalExceptionHandler

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistException.class)
    public ResponseEntity<?> handleUserAlreadyExists(UserAlreadyExistException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        // 409 Conflict — resource already exists
    }

    @ExceptionHandler(InvalidCredentialException.class)
    public ResponseEntity<?> handleInvalidCredentials(InvalidCredentialException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        // 401 Unauthorized — wrong credentials
    }
}
```

**`ResponseEntity<?>`** — Spring's wrapper for HTTP responses. The `?` means any type of body. Builder pattern:
- `.status(HttpStatus.CONFLICT)` — set HTTP status 409
- `.body(ex.getMessage())` — set response body

### HTTP Status Codes Reference
| Code | Name | When to use |
|---|---|---|
| 200 | OK | Success |
| 201 | Created | Resource created successfully |
| 400 | Bad Request | Client sent invalid data |
| 401 | Unauthorized | Not authenticated (no/invalid JWT) |
| 403 | Forbidden | Authenticated but not allowed |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Resource already exists |
| 500 | Internal Server Error | Server crashed |

**401 vs 403:**
- `401` — "Who are you?" — no/invalid token, wrong password
- `403` — "I know who you are, but no." — valid token but no permission

**Security note:** Return the same `401` for both "email not found" and "wrong password". Never tell the attacker which one failed — that would help them enumerate valid emails.

---

## 15. Complete User Service Flow

### Registration Flow
```
POST /api/auth/register
{
  "email": "rohit@gmail.com",
  "userName": "rohit",
  "firstName": "Rohit",
  "lastName": "Deshmukh",
  "password": "password123",
  "phoneNumber": "9999999999"
}
```

1. `UserController.registerUser()` receives request
2. `@Valid` triggers validation — `@NotBlank`, `@Email` annotations checked
3. `UserService.register()` called
4. Check `existsByEmail()` → if exists → throw `UserAlreadyExistException` → 409
5. Check `existsByUserName()` → if exists → throw `UserAlreadyExistException` → 409
6. `passwordEncoder.encode(password)` → BCrypt hash
7. `User.builder()...build()` → create User object with status `INACTIVE`
8. `userRepository.save(user)` → Hibernate runs INSERT, returns saved user with UUID
9. Build `RegisterResponse` from saved user (no password hash)
10. Return 200 with response

---

### Login Flow
```
POST /api/auth/login
{ "email": "rohit@gmail.com", "password": "password123" }
```

1. `UserController.login()` receives request
2. `UserService.login()` called
3. `findByEmail()` → if not found → throw `InvalidCredentialException` → 401
4. `passwordEncoder.matches(rawPassword, storedHash)` → if false → throw `InvalidCredentialException` → 401
5. `jwtUtil.generateToken(user)` → creates signed JWT
6. Return `LoginResponse` with token, userId, email

---

### Protected Endpoint Flow
```
GET /api/user/profile
Authorization: Bearer eyJhbGci...
```

1. Request arrives at Tomcat
2. `JwtAuthFilter.doFilterInternal()` runs
3. Reads `Authorization` header
4. Extracts token (after "Bearer ")
5. `jwtUtil.validateToken(token)` → validates signature + expiry
6. Creates `UsernamePasswordAuthenticationToken` with userId and role
7. Sets in `SecurityContextHolder`
8. Passes to next filter via `filterChain.doFilter()`
9. Spring Security checks SecurityContext → sees authenticated user → allows request
10. Controller runs → returns response

**Without token:**
Step 5 fails or header is null → SecurityContext stays empty → Spring Security returns 401

---

### UserController — Complete Code
```java
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public RegisterResponse registerUser(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }
}
```

**`@RestController`** = `@Controller` + `@ResponseBody`. Returns JSON automatically — no HTML templates.

**`@RequestMapping("/api/auth")`** — base URL for all endpoints in this controller.

**`@PostMapping("/register")`** — handles `POST /api/auth/register`.

**`@Valid`** — triggers Jakarta validation annotations (`@NotBlank`, `@Email`) on the request DTO.

**`@RequestBody`** — tells Spring to read the HTTP request body as JSON and deserialize it into the DTO. Without this, the DTO fields are all null.

---

### SecurityConfig — Complete Code
```java
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            // JWT uses headers not cookies → CSRF attacks impossible → disable

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Don't create sessions → JWT carries everything → server stores nothing

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()  // register + login = public
                .anyRequest().authenticated()                 // everything else needs JWT
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                )
            )
            // Return 401 (not Spring's default 403) for unauthenticated requests

            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
            // Run our JWT filter before Spring's default auth filter

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**`SecurityFilterChain`** is a Spring Security interface. When Spring finds a `@Bean` of this type, it automatically picks it up and uses it as the security configuration.

**`addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)`** — registers our JWT filter in the chain BEFORE Spring's default username/password filter. Our filter sets SecurityContext first; default filter sees it's done and skips.

---

## 16. Interview Arsenal

### Concepts You Can Now Confidently Explain

| Concept | Can explain? | Can code? |
|---|---|---|
| Multi-module Maven project | ✅ | ✅ |
| `dependencyManagement` vs `dependencies` | ✅ | ✅ |
| BOM import | ✅ | ✅ |
| Docker containers vs VMs | ✅ | ✅ |
| Docker Compose for local dev | ✅ | ✅ |
| Spring Boot 3-layer architecture | ✅ | ✅ |
| JPA entities and annotations | ✅ | ✅ |
| Hibernate ORM | ✅ | ✅ |
| Flyway migrations | ✅ | ✅ |
| Spring Security filter chain | ✅ | ✅ |
| CSRF and why we disable it | ✅ | ✅ |
| Sessions vs JWT | ✅ | ✅ |
| Stateless authentication | ✅ | ✅ |
| BCrypt password hashing | ✅ | ✅ |
| Repository pattern with Spring Data | ✅ | ✅ |
| DTOs and why they matter | ✅ | ✅ |
| Constructor injection over field injection | ✅ | ✅ |
| Optional<T> for null safety | ✅ | ✅ |
| JWT generation and validation | ✅ | ✅ |
| HMAC and why it prevents tampering | ✅ | ✅ |
| JWT filter (OncePerRequestFilter) | ✅ | ✅ |
| SecurityContext and SecurityContextHolder | ✅ | ✅ |
| Global exception handling (@ControllerAdvice) | ✅ | ✅ |
| HTTP status codes (401 vs 403 vs 409) | ✅ | ✅ |

### Top Interview Questions for Target Companies

**Razorpay / PhonePe / Stripe:**
- "Walk me through login end to end" → validate credentials → BCrypt match → generate JWT → return token
- "How do you secure protected endpoints?" → JWT filter → validate signature → set SecurityContext
- "How do you handle password security?" → BCrypt with salt and work factor — never store plain text
- "Why JWT over sessions?" → Stateless, any instance validates, works for mobile + web
- "What's 401 vs 403?" → 401 = not authenticated, 403 = authenticated but not authorized
- "How do you prevent returning sensitive data in API responses?" → DTOs — never expose entities

**Google / Amazon / Uber:**
- "How do you manage configuration across microservices?" → Spring Cloud Config (Phase 6)
- "How do you handle distributed transactions?" → Saga pattern (Phase 3)
- "What's your database migration strategy?" → Flyway with versioned scripts, never modify run migrations
- "How does your JWT filter work?" → OncePerRequestFilter, reads Bearer token, validates HMAC signature, sets SecurityContext

---

*Last updated: Session 2 — Phase 1 (User Service) 100% complete*
*Next session: Wallet Service — double-entry ledger, idempotency keys, optimistic locking*