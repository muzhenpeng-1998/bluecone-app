# ID Backfill Job 指南（internal_id / public_id 回填）

本说明文档描述如何使用应用内置的通用 ID 回填 Job，将历史表中的
`internal_id` / `public_id` 字段补齐为 ULID128 + PublicId。

Job 实现位于 `app-application` 模块：

- 包路径：`com.bluecone.app.migration.id`
- 入口类：`IdBackfillRunner`（实现 `CommandLineRunner`）

---

## 1. 功能与设计要点

- 对配置的一个或多个业务表进行批量回填：
  - 为每行生成：
    - `internal_id`：`BINARY(16)`，对应 `Ulid128`；
    - `public_id`：`VARCHAR(40)`，对应 `PublicId`（`prefix_ulid` 格式）。
- 分页扫描：
  - 按旧主键 `id` 升序；
  - 每批 500～2000 行（可配置）。
- 幂等性：
  - 只处理 `internal_id IS NULL` 的行；
  - `UPDATE ... WHERE id=? AND internal_id IS NULL`，重复执行不会覆盖已回填行。
- 内部使用：
  - `IdService` 生成 ULID128；
  - `PublicIdCodec` 按 type 前缀生成 `public_id`。
- 安全性：
  - 日志中仅打印 `public_id` 的前 8 位作为样例，不输出完整 ID。
- 资源保护：
  - 批次之间可配置 `sleepMillis`，防止持续写库压垮 RDS。

---

## 2. 配置项说明

配置前缀：`bluecone.migration.id-backfill`

```yaml
bluecone:
  migration:
    id-backfill:
      enabled: true                 # 是否启用回填 Job（默认 false）
      targets:                      # 目标表列表，可逗号分隔或 YAML list
        - bc_store
        - bc_order
      batch-size: 1000              # 每批大小（实际会被夹在 500~2000）
      start-id: 0                   # 起始游标（旧主键 id > start-id）
      dry-run: true                 # 是否为 dry-run：只打印日志，不执行 UPDATE
      sleep-millis: 100             # 每批之间 sleep 毫秒数，0 表示不 sleep
```

说明：

- `enabled`：
  - 需显式设置为 `true` 才会执行回填逻辑；
  - 关闭时 `IdBackfillRunner` 不会运行。
- `targets`：
  - 目标表名列表，如：`bc_store,bc_order,bc_user,bc_tenant_member,bc_payment_order`；
  - 支持逗号分隔字符串，或 YAML list。
- `batch-size`：
  - 若小于 500，会自动提升为 500；
  - 若大于 2000，会自动降低为 2000。
- `start-id`：
  - 用于分阶段回填或重试时跳过已处理区间；
  - Job 会执行：`WHERE id > startId AND internal_id IS NULL`。
- `dry-run`：
  - `true`：只打印即将更新的批次信息，不发出 `UPDATE`；
  - `false`：实际执行 `UPDATE`。
- `sleep-millis`：
  - 每处理完一批后休眠指定毫秒；
  - 建议根据 RDS 压力调整，例如 50~200ms。

---

## 3. 表名与 PublicId type 映射规则

`IdBackfillRunner` 会根据表名推断 PublicId 的 type 前缀，用于调用 `PublicIdCodec`：

- `bc_store*`      → `"sto"`（门店）
- `bc_order*`      → `"ord"`（订单）
- `bc_user*`       → `"usr"`（用户）
- `bc_tenant*`     → `"tnt"`（租户/租户会员）
- `bc_payment_order` → `"pay"`（支付订单）

当某个表名不在上述映射规则中时，Job 会输出警告并跳过该表：

```text
[id-backfill] skip table=xxx, no PublicId type mapping found
```

如需支持其他表，可在代码中补充映射分支。

---

## 4. 回填执行逻辑

对每个配置的目标表 `tableName`：

1. 计算 `typePrefix`：
   - 例如 `bc_store` → `"sto"`、`bc_order` → `"ord"`。
2. 分批查询待回填行：

   ```sql
   SELECT id
   FROM tableName
   WHERE id > :lastId AND internal_id IS NULL
   ORDER BY id ASC
   LIMIT :batchSize
   ```

3. 对每一行：
   - 通过 `IdService.nextUlid()` 生成 `Ulid128`；
   - `internal_id = ulid.toBytes()`（BINARY(16)）；
   - `public_id = publicIdCodec.encode(typePrefix, ulid).asString()`。
4. 在非 dry-run 模式下，批量更新：

   ```sql
   UPDATE tableName
   SET internal_id = ?, public_id = ?
   WHERE id = ? AND internal_id IS NULL
   ```

   - `internal_id IS NULL` 保障幂等性：已回填的行不会被覆盖。
5. 每个批次打印摘要日志（不会打印完整 public_id）：

   ```text
   [id-backfill] UPDATED table=bc_order batchSize=1000 updated=1000 lastId=123456 samplePublicIdPrefix=ord_1ABC
   ```

6. 执行可选 `sleepMillis`，然后继续下一批，直到没有更多行。

---

## 5. 幂等性与安全性

### 5.1 幂等性

- 只扫描 `internal_id IS NULL` 的行；
- 每次更新带有 `WHERE id = ? AND internal_id IS NULL` 条件；
- 即使 Job 多次运行，已填充行不会被重复更新。

### 5.2 日志安全

- 日志中不打印完整 `public_id`；
- 每批仅打印 `public_id` 的前 8 个字符作为样例，避免长串 ID 外泄。

---

## 6. 运行示例

### 6.1 本地运行（dry-run 模式）

1. 确保已执行 schema 迁移，为目标表增加 `internal_id`/`public_id` 字段；
2. 在应用的 `application.yml` 或 `application-local.yml` 中加入：

```yaml
bluecone:
  migration:
    id-backfill:
      enabled: true
      targets: bc_store,bc_order,bc_user,bc_tenant_member,bc_payment_order
      batch-size: 1000
      start-id: 0
      dry-run: true
      sleep-millis: 50
```

3. 启动应用：

```bash
mvn -pl app-application spring-boot:run
```

观察日志，确认即将处理的记录范围与批次信息是否符合预期。

### 6.2 实际回填（非 dry-run）

在确认无误后，将配置改为：

```yaml
bluecone:
  migration:
    id-backfill:
      enabled: true
      targets: bc_store,bc_order
      batch-size: 1000
      start-id: 0
      dry-run: false
      sleep-millis: 100
```

然后重新启动应用或在专用环境中运行一次 Job。建议：

- 先在小范围（少量数据/影子库）验证；
- 观察 RDS 负载和锁等待情况；
- 如需分段执行，可以逐次提升 `start-id`，避免重复扫描前缀区间。

---

## 7. 与 schema 迁移的配合

本 Job 仅负责数据回填，不负责 DDL 迁移。推荐流程：

1. 先执行 `docs/sql/migration` 下的 DDL：
   - 如 `V20251213__add_internal_public_id__bc_order.sql` 等；
2. 确认表中已存在 `internal_id` / `public_id` 列；
3. 再在应用中开启 `IdBackfillRunner` 进行数据回填；
4. 回填完成并运行一段时间后，再考虑：
   - 将新写路径切换为基于 `internal_id`/`public_id` 的模式；
   - 最终视情况弱化或迁移旧的 BIGINT 主键使用。

