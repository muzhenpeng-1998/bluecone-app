# Store/Order/Product 相关模块依赖概览

- `app-application`（入口聚合）：依赖 `app-core`、`app-infra`、`app-security`、`app-order`、`app-tenant`、`app-store`、`app-product`、`app-resource`、`app-payment`，用于统一启动和装配各域能力。
- `app-order`（订单域）：依赖 `app-core`、`app-infra`、`app-payment`、`app-security`、`app-product`。当前不反向依赖门店模块，避免 `app-store` ←→ `app-order` 循环。
- `app-product`（商品域）：依赖 `app-core`、`app-infra`。不依赖门店/订单，保持商品域的独立。
- `app-store`（门店域）：依赖 `app-core`、`app-infra`、`app-resource-api`。不依赖订单/商品，保证后续 StoreRuntime 改造可独立演进。

约定与注意事项：
- 若确需跨域调用，请优先使用对应的 *-api（如需新增对外契约时单独抽取），避免直接依赖实现模块并防止新的循环依赖。
- 编译环境请使用 JDK 21（`JAVA_HOME=$(/usr/libexec/java_home -v 21)`）以与当前 `maven-compiler-plugin` 配置保持一致。

