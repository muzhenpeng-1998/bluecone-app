# Bluecone Test Strategy

## Pyramid Overview
- **Unit tests (`*Test`)** stay in module-level `src/test/java` and avoid Spring. Focus on deterministic logic in `app-core` (domain, config, id, notification) and small infrastructure helpers (key builders, DTO mappers). Use AssertJ + Mockito.
- **Service/integration tests (`*IT`)** boot Spring + Testcontainers. `app-infra` exercises database mappers, Redis stacks, outbox, scheduler, and config center. `app-application` boots the full API with security filters, gateway middleware, and multi-tenant pipeline to verify HTTP contracts.
- **API/Security regression** sits on top of the integration layer and validates auth flows, gateway routing/signature checks, and admin endpoints. These tests extend the web base class and work through HTTP just like clients.

## Naming & Conventions
- `*Test` — unit or lightweight component tests executed by Surefire during `mvn test`. Never touch containers or the network.
- `*IT` — integration tests triggered by Failsafe during `mvn verify`. They may depend on Testcontainers MySQL + Redis and can use Spring Boot Test. Keep them hermetic and idempotent.
- Base fixtures live under `com.bluecone.app.test` packages inside each module. Shared helpers like `TestDataFactory` sit in module test sources to avoid leaking into production jars.

## Multi-tenant & Security Notes
- Always set `TenantContext` explicitly in tests that touch tenant-aware code (mappers, Redis key builders, gateway middleware). Clean the context in `@AfterEach` hooks to avoid leakage between tests.
- When exercising security filters, prefer generating JWTs via `TokenProvider` and populate `Authorization` plus tenant headers the same way mobile/web clients do. Avoid bypassing filters by mocking MVC since we want the real wiring validated.
- Redis-backed helpers (`RedisOps`, rate limiter, distributed lock, idempotent executor) should use the provided flush utilities so every test starts from a clean slate.

## Fast vs Full Runs
- `mvn test` — execute all `*Test` classes. This should run before every commit to keep feedback fast (<1 minute) and avoid starting containers.
- `mvn verify` (default `test-containers` profile) — runs both unit tests and all `*IT` classes with Testcontainers. Execute before deploying to WeChat Cloud or any production-like environment.
- Disable containers locally with `-P -test-containers` to skip Failsafe/containers when you only need a quick unit-cycle.

## Adding Coverage for New Business Modules
- **Domain logic** (inventory, store, payment, billing, etc.) lives next to the module and should get `*Test` classes focusing on invariants and calculations.
- **Infrastructure-facing code** (mappers, async pipelines, outbox handlers) should extend the integration base classes inside their module and add `*IT` suites that interact with MySQL/Redis through Testcontainers.
- **Exposed APIs** go under `app-application/src/test/java/com/bluecone/app/api/...` and extend `AbstractWebIntegrationTest` so every endpoint is verified with real security, tenant headers, and middleware.
