# Store 模块工程收口 - 交付清单

## 📋 概述

本文档记录 Store 模块工程收口的完整改动清单、验证方法和启动说明。

**完成时间**: 2025-12-16  
**状态**: ✅ 已完成

---

## 一、改动文件清单（按模块分组）

### app-infra 模块

#### 新增文件
- `app-infra/src/main/resources/db/migration/V20251216__create_store_tables.sql`
  - 创建完整的 Store 相关表结构（bc_store、bc_store_capability、bc_store_opening_hours、bc_store_special_day、bc_store_channel、bc_store_read_model）
  - 包含完整的中文字段注释和索引

### app-store 模块

#### 修改文件
- `app-store/src/main/java/com/bluecone/app/store/infrastructure/repository/StoreRepositoryImpl.java`
  - 补齐 `updateOpeningSchedule()` 方法实现（先删后插策略）
  - 补齐 `updateCapabilities()` 方法实现（先删后插策略）
  - 补充完整的中文 JavaDoc 注释

- `app-store/src/main/java/com/bluecone/app/store/application/service/StoreDeviceAssembler.java`
  - 补充 `configSummary` 字段解析逻辑（JSON 前 50 字符摘要）

- `app-store/src/main/java/com/bluecone/app/store/application/service/StoreChannelAssembler.java`
  - 补充 `configSummary` 字段解析逻辑（JSON 前 50 字符摘要）

- `app-store/src/main/java/com/bluecone/app/store/application/service/StoreResourceAssembler.java`
  - 补充 `metadataSummary` 字段解析逻辑（优先返回资源类型）

- `app-store/src/main/java/com/bluecone/app/store/domain/service/impl/StoreOpenStateServiceImpl.java`
  - 增强渠道校验逻辑（检查渠道是否已绑定且状态为 ACTIVE）
  - 补充营业时间和特殊日校验的中文注释

- `app-store/src/main/java/com/bluecone/app/store/handler/StoreConfigChangedHandler.java`
  - 补充事件处理逻辑（本地缓存失效）
  - 补充完整的中文 JavaDoc 注释

- `app-store/src/main/java/com/bluecone/app/store/domain/repository/StoreRepository.java`
  - 更新 `updateOpeningSchedule()` 和 `updateCapabilities()` 的 JavaDoc（已实现）

- `app-store/src/main/java/com/bluecone/app/store/application/service/StoreQueryService.java`
  - 补充所有方法的完整 JavaDoc 注释

- `app-store/src/main/java/com/bluecone/app/store/domain/error/StoreErrorCode.java`
  - 新增 `STORE_CHANNEL_NOT_BOUND` 错误码

#### 删除文件
- `app-store/src/main/java/com/bluecone/app/store/application/StoreApplicationService.java`
  - 删除冗余类（功能已由 StoreCommandService 和 StoreQueryService 实现）

### app-application 模块

#### 修改文件
- `app-application/src/main/resources/application.yml`
  - 已使用环境变量占位符（无需修改）
  - Flyway 配置已开启 `validate-on-migrate: true`

- `app-application/src/test/resources/application-test.yml`
  - 补充 `validate-on-migrate: true` 配置

#### 新增文件
- `app-application/src/main/resources/application-example.yml`
  - 配置结构示例模板（所有敏感信息已脱敏）

- `app-application/src/main/resources/application-local.yml.template`
  - 本地开发配置模板

- `app-application/src/test/java/com/bluecone/app/store/StoreIntegrationTest.java`
  - 集成测试（继承 AbstractWebIntegrationTest）
  - 测试用例：创建门店、查询详情、更新信息、更新能力、更新营业时间、并发冲突、列表查询

### 根目录

#### 新增文件
- `.gitignore`
  - 添加 `application-local.yml`、`*-local.yml` 等忽略规则

- `LOCAL-SETUP.md`
  - 本地开发环境配置指南（环境变量清单、启动方式、验证方法）

- `docs/STORE-API-EXAMPLES.md`
  - REST API 调用示例（curl 命令）

- `docs/STORE-ENGINEERING-SUMMARY.md`
  - 本文档（交付清单）

---

## 二、新增/修改的 Flyway 脚本列表

### 新增脚本
- `V20251216__create_store_tables.sql`
  - 创建 6 张表：
    1. `bc_store` - 门店主表
    2. `bc_store_capability` - 门店能力配置表
    3. `bc_store_opening_hours` - 常规营业时间表
    4. `bc_store_special_day` - 特殊日配置表
    5. `bc_store_channel` - 渠道绑定表
    6. `bc_store_read_model` - 读模型快照表
  - 所有字段包含中文 COMMENT
  - 包含完整的索引（唯一索引、普通索引）

### Flyway 配置变更
- `application.yml`: `validate-on-migrate: true`（已开启）
- `application-test.yml`: `validate-on-migrate: true`（已开启）

---

## 三、如何本地启动

### 方式 1：使用 application-local.yml（推荐本地开发）

```bash
# 1. 复制配置模板
cd app-application/src/main/resources
cp application-local.yml.template application-local.yml

# 2. 编辑 application-local.yml，填入本地数据库、Redis、OSS 等配置信息

# 3. 启动应用
mvn -pl app-application -am spring-boot:run -Dspring-boot.run.profiles=local
```

### 方式 2：使用环境变量（推荐生产环境）

```bash
# 设置环境变量
export DB_URL=jdbc:mysql://localhost:3306/bluecone?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
export DB_USERNAME=root
export DB_PASSWORD=yourpassword
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_DATABASE=0
export REDIS_PASSWORD=
export OSS_ENDPOINT=https://oss-cn-hangzhou.aliyuncs.com
export OSS_ACCESS_KEY_ID=your_access_key_id
export OSS_ACCESS_KEY_SECRET=your_access_key_secret
export OSS_BUCKET=bluecone

# 启动应用
mvn -pl app-application -am spring-boot:run
```

### 环境变量清单

详见 [LOCAL-SETUP.md](LOCAL-SETUP.md)

---

## 四、如何验证

### 4.1 构建验证

```bash
# 编译验证（跳过测试）
mvn -pl app-application -am clean compile -DskipTests

# 预期输出：BUILD SUCCESS
```

### 4.2 集成测试验证

```bash
# 运行 Store 集成测试（需要 Docker 支持 Testcontainers）
mvn -pl app-application -am test -Dtest=StoreIntegrationTest

# 或运行所有测试
mvn -pl app-application -am test
```

### 4.3 REST API 验证

#### 创建门店
```bash
curl -X POST http://localhost:80/api/admin/store \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -H "Idempotency-Key: test-create-001" \
  -d '{
    "name": "测试门店",
    "shortName": "测试",
    "industryType": "FOOD",
    "cityCode": "330100",
    "openForOrders": true
  }'
```

#### 查询门店详情
```bash
curl -X GET "http://localhost:80/api/admin/store/detail?storeId=123" \
  -H "X-Tenant-Id: 1001"
```

#### 更新门店基础信息
```bash
curl -X PUT http://localhost:80/api/admin/store/base \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1001" \
  -d '{
    "tenantId": 1001,
    "storeId": 123,
    "expectedConfigVersion": 1,
    "name": "测试门店（已更新）"
  }'
```

#### 检查是否可接单
```bash
curl -X GET "http://localhost:80/api/open/store/check-acceptable?storeId=123&capability=DINE_IN" \
  -H "X-Tenant-Id: 1001"
```

**完整 API 示例**: 详见 [STORE-API-EXAMPLES.md](STORE-API-EXAMPLES.md)

---

## 五、核心功能实现清单

### ✅ 已完成功能

1. **创建门店** (`createStore`)
   - 生成 internalId、publicId、storeNo
   - 初始化 configVersion = 1
   - 支持幂等创建（Idempotency-Key）

2. **更新门店基础信息** (`updateStoreBase`)
   - 乐观锁控制（configVersion）
   - 版本冲突时抛出 `StoreConfigVersionConflictException`

3. **查询门店详情** (`getStoreDetail` / `getStoreBase`)
   - 支持通过 storeId、storePublicId、storeCode 查询
   - 返回完整的门店基础信息视图

4. **查询门店列表** (`storeList`)
   - 支持按 tenantId、cityCode、industryType、status、keyword 筛选

5. **生成订单快照** (`getOrderSnapshot`)
   - 包含门店 id/name/地址/营业态/可接单状态
   - 支持按渠道类型获取快照

6. **门店可接单判断** (`checkOrderAcceptable`)
   - 判断顺序：
     1. 门店状态是否为 OPEN
     2. openForOrders 是否为 true
     3. capability 是否已启用
     4. 特殊日校验（优先级最高）
     5. 常规营业时间校验
     6. 渠道绑定状态校验

7. **乐观锁/版本号控制**
   - 所有配置更新操作都使用 configVersion 做乐观锁
   - 更新失败时抛出 `StoreConfigVersionConflictException`

8. **中文注释**
   - 所有 public 类、接口、方法都有完整的 JavaDoc（中文）
   - 关键业务分支都有行内中文注释

---

## 六、测试覆盖

### 集成测试（StoreIntegrationTest）

1. ✅ 创建门店 → 查询门店详情 → 更新门店 → 再次查询
2. ✅ 更新能力配置
3. ✅ 更新营业时间
4. ✅ 并发更新冲突（乐观锁）
5. ✅ 门店列表查询

### REST API 测试

所有 API 端点都有对应的 curl 示例，详见 [STORE-API-EXAMPLES.md](STORE-API-EXAMPLES.md)

---

## 七、注意事项

1. **数据库迁移**
   - 首次启动会自动执行 Flyway 迁移脚本
   - 确保数据库用户有 CREATE TABLE、ALTER TABLE 权限
   - 如已有历史库，请先执行 baseline 或使用 `IF NOT EXISTS` 策略

2. **配置安全**
   - `application-local.yml` 已加入 `.gitignore`，不会被提交
   - 生产环境必须使用环境变量，禁止在配置文件中硬编码敏感信息

3. **乐观锁使用**
   - 所有更新操作必须提供 `expectedConfigVersion`
   - 更新失败时应提示用户刷新页面后重试

4. **测试环境**
   - 集成测试需要 Docker 支持（Testcontainers）
   - 如无 Docker，可跳过集成测试或使用本地数据库

---

## 八、后续优化建议

1. **缓存优化**
   - 当前 StoreConfigCache 为内存 Map 实现，后续可接入 Redis
   - 实现多级缓存（本地缓存 + Redis）

2. **分页支持**
   - 门店列表查询当前未分页，后续可添加分页功能

3. **搜索索引同步**
   - StoreConfigChangedHandler 中预留了搜索索引同步的扩展点

4. **性能优化**
   - 批量查询门店配置时可使用批量加载优化
   - 营业时间判断可缓存计算结果

---

**最后更新**: 2025-12-16

