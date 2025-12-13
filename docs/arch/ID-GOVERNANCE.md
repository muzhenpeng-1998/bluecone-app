# ID 使用治理规范（ArchUnit 规则说明）

本规范描述了在 BlueCone 多模块工程中使用 ID（internal_id / public_id）的统一约束，
通过 ArchUnit 架构测试在 CI 中自动检查，避免业务模块绕开 app-id 门面直接操作 ULID 或底层实现。

## 1. 为什么要禁用直接 ULID / 生成器依赖

- **安全性**：直接在业务模块中使用 ULID 三方库或 new 生成器，容易导致：
  - 不一致的生成策略（有的单调、有的非单调）；
  - 多种 ID 格式并存，难以治理；
  - 将来迁移到 BINARY(16) / 强类型 ID 时成本极高。
- **可演进性**：所有 ID 生成逻辑集中在 `app-id` 模块中：
  - 可统一升级时钟回拨处理、STRIPED 模式、指标采集；
  - 可统一引入幂等、PublicId、TypedId 等能力。
- **可观测性**：只有通过 `IdService` / `PublicIdCodec` 等门面才能保证指标/日志统一上报。

因此：

> **业务模块禁止直接依赖 `de.huxhorn.sulky.ulid` 和 `UlidIdGenerator` 等内部实现，只能依赖 app-id 暴露的门面 API。**

ArchUnit 中的 Rule 1/2 会自动检查这一点。

---

## 2. 正确 ID 使用方式（门面接口）

### 2.1 获取内部 ID（internal_id）

统一通过 `IdService`：

```java
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.core.Ulid128;

public class SomeDomainService {

    private final IdService idService;

    public SomeDomainService(IdService idService) {
        this.idService = idService;
    }

    public void doSomething() {
        Ulid128 internalId = idService.nextUlid();      // 强类型 128 位
        String ulidString = idService.nextUlidString(); // 兼容旧接口
        byte[] bytes16 = idService.nextUlidBytes();     // BINARY(16) 存库
    }
}
```

### 2.2 生成/解析对外 ID（public_id）

统一通过 `PublicIdCodec`：

```java
import com.bluecone.app.id.publicid.api.PublicIdCodec;

public class PublicIdFacade {

    private final IdService idService;
    private final PublicIdCodec publicIdCodec;

    public PublicIdFacade(IdService idService, PublicIdCodec publicIdCodec) {
        this.idService = idService;
        this.publicIdCodec = publicIdCodec;
    }

    public String nextOrderPublicId() {
        var ulid = idService.nextUlid();
        return publicIdCodec.encode("ord", ulid).asString();
    }
}
```

对外 API DTO 只暴露 `public_id` 字符串，不暴露 internal_id。

### 2.3 使用强类型 ID（TypedId）

在领域层使用 `OrderId` / `StoreId` 等强类型可减少串用错误：

```java
OrderId orderId = TypedIds.newOrderId(idService);
String publicId = orderId.asPublic(publicIdCodec);
```

从 `public_id` 解析：

```java
OrderId orderId = TypedIds.fromPublic(req.publicId(), publicIdCodec, OrderId::new, "ord");
```

---

## 3. ArchUnit 规则简要说明与修复建议

ArchUnit 测试类：`app-application/src/test/java/com/bluecone/app/arch/ArchIdGovernanceTest.java`

### Rule 1：禁止非 app-id 依赖 ULID 三方库

```java
noClasses()
  .that().resideOutsideOfPackage("com.bluecone.app.id..")
  .and().areNotAnnotatedWith(AllowIdInfraAccess.class)
  .should().notDependOnClassesThat().resideInAnyPackage("de.huxhorn.sulky.ulid..");
```

**典型报错与修复：**

- 报错：某业务类依赖 `de.huxhorn.sulky.ulid.ULID`。
- 修复：
  - 删除直接依赖；
  - 使用 `IdService` 生成 ULID；
  - 如确为 ID 基础设施适配层，请移动/重构到 app-id 或使用豁免注解（见第 4 节）。

### Rule 2：禁止非 app-id 依赖 app-id 内部实现

```java
noClasses()
  .that().resideOutsideOfPackage("com.bluecone.app.id..")
  .and().areNotAnnotatedWith(AllowIdInfraAccess.class)
  .should().notDependOnClassesThat(internalImplPackages);
```

其中 `internalImplPackages` 包括：

- `com.bluecone.app.id.core..`
- `com.bluecone.app.id.metrics..`
- `com.bluecone.app.id.publicid.core..`
- `com.bluecone.app.id.autoconfigure..`
- `com.bluecone.app.id.mybatis..`

**允许依赖的门面：**

- `com.bluecone.app.id.api.IdService`
- `com.bluecone.app.id.publicid.api.PublicIdCodec`
- `com.bluecone.app.id.typed.api.*`

**典型报错与修复：**

- 报错：业务直接 new `UlidIdGenerator` 或依赖 `UlidMetrics` 等内部类。
- 修复：
  - 注入 `IdService` 或 `PublicIdCodec` 替代；
  - 若需要扩展 ID 能力，应在 app-id 内新增门面/配置，而非在业务模块中直接操作内部实现。

### Rule 3：API/Controller 层不得依赖 Ulid128

```java
noClasses()
  .that().resideInAnyPackage("com.bluecone.app..api..",
                             "com.bluecone.app..controller..",
                             "com.bluecone.app..web..")
  .and().areNotAnnotatedWith(AllowIdInfraAccess.class)
  .should().notDependOnClassesThat()
  .haveFullyQualifiedName("com.bluecone.app.id.core.Ulid128");
```

**典型报错与修复：**

- 报错：Controller/DTO 中使用 `Ulid128` 作为字段类型或方法返回值。
- 修复：
  - DTO 中字段改为 `String publicId`（或 `TypedId`，依赖 Jackson 序列化为 `public_id` 字符串）；
  - 内部使用 `Ulid128` / `TypedId`，对外只暴露 `public_id`。

---

## 4. 白名单豁免：@AllowIdInfraAccess

在极少数基础设施桥接层（例如旧 ID 模块桥接到新 app-id）可能需要直接访问内部实现。
为此提供注解：

```java
package com.bluecone.app.id.governance;

@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowIdInfraAccess {
}
```

使用方式（示例，仅限基础设施桥接类）：

```java
@Service
@AllowIdInfraAccess
public class DefaultIdService implements com.bluecone.app.core.id.IdService {
    // 旧 core IdService -> 新 app-id IdService 的桥接
}
```

**审批原则：**

1. 默认禁止：业务代码不得随意加此注解。
2. 仅限基础设施／桥接层：
   - 例如旧 core ID 接口到新 app-id 的适配，实现迁移过程中的兼容。
3. 如需新增豁免：
   - 评估是否可以通过调整依赖关系或扩展 app-id 门面解决；
   - 如确需豁免，应在代码评审中明确说明原因，并在注释中写清用途与后续移除计划。

---

## 5. 如何运行与调试 ArchUnit 规则

- 在本地或 CI 中运行：

```bash
mvn -pl app-application test
```

- 若全仓运行：

```bash
mvn test
```

当规则违反时，`ArchIdGovernanceTest` 中会给出具体违反类与依赖关系，
并在 `because(...)` 中提示推荐的修复方式（如使用 IdService/PublicIdCodec/TypedId 等）。 

