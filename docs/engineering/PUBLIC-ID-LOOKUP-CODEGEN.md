# Public ID Lookup 代码生成与索引校验

本文档描述如何用一份 YAML 自动生成 `PublicIdLookup` 实现，并在应用启动时校验 `(tenant_id, public_id)` 联合索引契约。

## 为什么要 codegen
- 避免手写重复的 Lookup（SQL 结构高度一致）
- 强一致：资源类型拼写与 `ResourceType` 枚举对齐
- 只查询必要列，减少网络与反序列化开销
- 生成统一的批量查询拆分（默认单批 200）

## YAML 格式
文件：`app-infra/src/main/resources/public-id-resources.yaml`

```
resources:
  - type: STORE           # 必须与 app-id ResourceType 完全一致
    table: bc_store
    pkColumn: id
    tenantColumn: tenant_id
    publicIdColumn: public_id
  - type: PRODUCT
    table: bc_product
    pkColumn: id
    tenantColumn: tenant_id
    publicIdColumn: public_id
  - type: SKU
    table: bc_product_sku
    pkColumn: id
    tenantColumn: tenant_id
    publicIdColumn: public_id
```

## 如何新增资源类型
1. 在 YAML 追加一条资源定义（确保表名/列名存在且与 DDL 对齐）。
2. 运行 `mvn package`（generate-sources 阶段自动生成代码）。
3. 启动应用：生成的 Lookup Bean 会被自动装配到 `DefaultPublicIdGovernanceResolver`。

## 生成产物
- 输出目录：`app-infra/target/generated-sources/publicid`
- 包名：`com.bluecone.app.infra.publicid.lookup`
- 每个资源生成 `<Type>PublicIdLookup implements PublicIdLookup`
  - 单查：`SELECT {pk} FROM {table} WHERE {tenant}=:tenantId AND {publicId}=:publicId LIMIT 1`
  - 批量：`SELECT {publicId} AS pid, {pk} AS pk FROM {table} WHERE {tenant}=:tenantId AND {publicId} IN (:publicIds)`
  - 空集合直接返回空 Map；批量 IN 拆分，单批默认 200
- 聚合配置：`PublicIdLookupGeneratedConfiguration` 暴露所有 Lookup 的 `@Bean`

## 关键代码（生成器主类）
路径：`app-platform-codegen/src/main/java/com/bluecone/app/platform/codegen/publicid/PublicIdLookupCodegenMain.java`

```
PublicIdLookupCodegenMain.main(
    yamlPath,                    // 默认 src/main/resources/public-id-resources.yaml
    outputDir,                   // 默认 target/generated-sources/publicid
    maxBatchSize                 // 默认 200
)
```

## Maven 集成
`app-infra/pom.xml` 中：
- `exec-maven-plugin`（generate-sources 阶段执行 codegen main）
- `build-helper-maven-plugin`（将 `target/generated-sources/publicid` 加入编译源）
- `maven-resources-plugin` 确保 YAML 可读

## 启动期索引校验
类：`com.bluecone.app.infra.publicid.verify.PublicIdSchemaVerifier`
- 读取同一份 `public-id-resources.yaml`
- 校验：
  - 表存在：`information_schema.tables`
  - 列存在：`information_schema.columns`
  - 联合索引存在：`information_schema.statistics`，前两列必须为 `(tenant_id, public_id)`（顺序敏感）
- 配置：`bluecone.publicid.verify.*`
  - enabled=true（默认）
  - failFast=true（默认，失败则阻止启动）
  - requireCompositeIndex=true
  - indexColumns=["tenant_id","public_id"]
- 推荐 DDL 输出（不自动执行）：
  - `CREATE INDEX idx_<table>_tenant_public ON <table>(tenant_id, public_id);`

## 测试
### Codegen 单测
- 文件：`app-platform-codegen/src/test/java/.../PublicIdLookupCodegenMainTest.java`
- 验证生成文件存在、类名正确、SQL 片段包含 tenant/publicId/pk

### Verifier 集成测试
- 文件：`app-infra/src/test/java/.../PublicIdSchemaVerifierIT.java`
- 使用 Testcontainers MySQL：
  - 无索引表 -> failFast 抛出异常
  - 有联合索引表 -> 校验通过

## 运行与验证
- 构建：`mvn test`（默认开启 testcontainers profile）
- 启动：正常启动即会执行索引校验；如需跳过，可设置 `bluecone.publicid.verify.enabled=false`
- 批量查询拆分：默认单批 200，可在 codegen 执行参数中调整

## 文件清单
- 新增：`app-infra/src/main/resources/public-id-resources.yaml`
- 新增：`app-platform-codegen` 模块及生成器主类
- 新增：`app-infra` 中的校验配置与自动配置
- 新增：文档 `docs/engineering/PUBLIC-ID-LOOKUP-CODEGEN.md`

