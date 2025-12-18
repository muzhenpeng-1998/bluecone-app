# App-ID Isolation Baseline (Phase 0)

## Git Baseline
- Branch: `main` (upstream `github/main`), feature branch to create: `feature/app-id-isolation`.
- Working tree: dirty with numerous untracked files/targets; no staged changes.
- Last commit: `0d5230a` “把 public_id 从…升级为平台强制…”.

## Current app-id Surface (classes outside any `internal` namespace)
- `com.bluecone.app.id.api`: `IdService`, `IdScope`, `ResourceType`.
- `autoconfigure`: `IdAutoConfiguration`, `IdJacksonAutoConfiguration`, `IdMybatisAutoConfiguration`.
- `config`: `BlueconeIdProperties`, `IdConfiguration`, `InstanceNodeIdProvider`.
- `core`: `Ulid128`, `UlidIdGenerator`, `UlidIdService`, `EnhancedIdService`, `SnowflakeLongIdGenerator`, `PublicIdFactory`, `ClockRollbackException`, `MonotonicUlidGenerator`.
- `metrics`: `UlidMetrics`.
- `segment`: `IdSegmentRepository`, `SegmentRange`, `SegmentLongIdGenerator`.
- `publicid.api`: `PublicId`, `DecodedPublicId`, `PublicIdCodec`.
- `publicid.core`: `DefaultPublicIdCodec`, `Base32Crockford`, `Base62`, `Crc8`.
- `typed.api`: `TypedId`, `TenantId`, `StoreId`, `OrderId`, `PaymentId`, `UserId`.
- `typed.core`: `TypedIds`.
- `jackson`: `BlueconeIdJacksonModule`, `TypedIdJsonSerializer`, `Ulid128JacksonModule`.
- `mybatis`: `Ulid128BinaryTypeHandler`, `Ulid128Char26TypeHandler`.
- `governance`: `AllowIdInfraAccess`.

## Repo Usage Summary (`import com.bluecone.app.id…`)
- app-core: 58 matches / 38 files (public-id governance, idresolve API/SPI, store context, events, typed users).
- app-application: 28 matches / 19 files (controllers, middleware, migrations, id resolve config/tests).
- app-infra: 23 matches / 10 files (segment repo, id service impl, public-id infra, user snapshot).
- app-store: 19 matches / 13 files (public-id lookup, store snapshots, DAO/entities, tests).
- app-product: 7 matches / 7 files (public-id lookup, snapshot providers, APIs).
- app-inventory: 5 matches / 5 files (inventory scope/snapshots, tests).
- app-platform-codegen: 2 matches / 1 file (public-id lookup codegen).
- app-platform-starter: 5 matches / 2 test files.
- app-ops: 1 match / 1 file.
- app-id: multiple self-references (API, codecs, segment, typed, auto-config).
- Docs mention: `docs/engineering/ID-MAPPING.md`, `docs/arch/ID-GOVERNANCE.md`.

## Migration Map (proposed)
- To `app-id-api` (pure contracts): `IdService`, `IdScope`, `ResourceType`, `Ulid128`, ULID outputs (`nextUlid*`), long-id SPI (`IdSegmentRepository`, `SegmentRange`), public-id contracts (`PublicId`, `DecodedPublicId`, `PublicIdCodec`, `PublicIdValidator` if present), governance/resolve SPIs (`PublicIdLookup`, `PublicIdResolverApi`/equiv), typed ID value objects (`TypedId` + strong types), public-id DTOs (`ResolvedPublicId`, `DecodedPublicId` if separate), any public exceptions.
- Remain in `app-id` under `com.bluecone.app.id.internal`: generators (`MonotonicUlidGenerator`, `UlidIdGenerator`, `UlidIdService`, `EnhancedIdService`, `SnowflakeLongIdGenerator`, `SegmentLongIdGenerator`), factories/validators (`PublicIdFactory`, codec impls), infra adapters (Jackson/MyBatis modules, properties, auto-config), governance annotations (`AllowIdInfraAccess`) if still needed, config helpers.
- Dependency changes: business modules (`app-core`, `app-store`, `app-product`, `app-inventory`, etc.) switch to `app-id-api`; bootstrap modules keep `app-id` for wiring. Imports update to `com.bluecone.app.id.api.*` / `.publicid.api.*` / typed IDs.

## Gaps/Risks to address next phases
- Need ArchUnit + Enforcer guards to block imports/deps on `com.bluecone.app.id.internal` and on `app-id` artifact (except whitelisted boot modules).
- Segment generator scope handling needs verification before setting default.
- Public ID/Typed ID codecs currently exposed from implementation packages; must be relocated or adapted.
