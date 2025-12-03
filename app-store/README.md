# app-store 模块说明（门店/店铺领域）

- **定位**：沉淀店铺/门店相关的实体、DAO 与应用服务，聚焦店铺档案、营业状态、地址与配置等通用能力。
- **对外接口**：建议通过 `service` 下的应用服务对外暴露能力，避免直接依赖 Mapper。
- **目录建议**：
  - `dao/entity`：店铺、营业时间、地址、标签等表对应的实体。
  - `dao/mapper`：MyBatis-Plus Mapper。
  - `dao/service` & `impl`：DAO Service。
  - `model/command` 与 `model/query`：命令与查询对象。
  - `service` 与 `impl`：应用服务/编排层。
- **依赖**：继承父工程，默认引入 `app-core`、Spring Boot 基础依赖、MyBatis-Plus 与校验依赖，可按需增补。

> 暂未提供具体实现，后续可按上述结构补充领域模型与业务编排。
