# LongId nodeId 工程化配置指引

> 目标：当启用 long 型 ID（Snowflake）时，为每个实例分配稳定且不冲突的 nodeId，并在配置缺失或错误时快速失败，避免“隐性撞号”。

## 1. 行为约束与失败策略

- 开关：`bluecone.id.long.enabled=true` 时启用 Snowflake long ID。
- 强约束：
  - 启用后，当前实例必须能解析出一个合法的 `nodeId`（0..1023）；
  - 否则应用启动失败，抛出 `IllegalStateException`。
- 解析顺序：
  1. 配置属性：`bluecone.id.long.node-id`
  2. 环境变量：`BLUECONE_NODE_ID`（兼容 `BLUECONE_ID_NODE_ID`）

> 实现类：`app-id/src/main/java/com/bluecone/app/id/config/InstanceNodeIdProvider.java`  
> 自动装配：`IdAutoConfiguration#snowflakeLongIdGenerator(...)`

## 2. 推荐配置方式

### 2.1 直接配置属性（本地/简单环境）

适用于本地开发或节点数量较少、由人工维护配置的场景：

```properties
bluecone.id.long.enabled=true
bluecone.id.long.node-id=0
```

多实例部署时，确保每个实例的 `node-id` 唯一即可。

### 2.2 环境变量：BLUECONE_NODE_ID（推荐）

在大多数容器/云托管环境下，更推荐通过环境变量注入 nodeId：

- 每个实例设置唯一的 `BLUECONE_NODE_ID`；
- Spring 中无需再显式配置 `bluecone.id.long.node-id`。

示例（docker run）：

```bash
docker run \
  -e BLUECONE_NODE_ID=1 \
  your-image:tag
```

示例（docker-compose）：

```yaml
services:
  app-0:
    image: your-image:tag
    environment:
      - BLUECONE_NODE_ID=0
  app-1:
    image: your-image:tag
    environment:
      - BLUECONE_NODE_ID=1
```

## 3. 微信云托管中的 nodeId 配置建议

微信云托管本质上是托管容器实例，建议结合平台特性为每个实例分配稳定 nodeId。

### 3.1 手工配置（实例数较少）

适用场景：实例数固定且较少（例如 2~3 个），并且可以人工维护不同版本/环境的配置。

做法：

- 为不同实例构建不同镜像或使用不同“服务版本”；
- 在每个版本的启动参数/环境变量中设置不同的 `BLUECONE_NODE_ID`。

示例（伪代码，具体以平台控制台/配置为准）：

- 版本 A：`BLUECONE_NODE_ID=0`
- 版本 B：`BLUECONE_NODE_ID=1`

注意：

- 升级发布时，需要保持 nodeId 与实例角色的一致性，避免一个物理实例在升级后换了 nodeId。

### 3.2 启动脚本计算 nodeId

当平台提供实例标识（如：实例 ID、Pod 名等）时，可以在 ENTRYPOINT/启动脚本中计算 nodeId：

```bash
#!/usr/bin/env bash
set -e

# 根据平台注入的实例标识计算一个 0..1023 的 nodeId
RAW_ID="${POD_NAME:-${INSTANCE_ID:-0}}"
HASH=$(echo -n "${RAW_ID}" | sha256sum | cut -c1-8)
NODE_ID=$(( 0x${HASH} % 1024 ))

export BLUECONE_NODE_ID="${NODE_ID}"
echo "Resolved BLUECONE_NODE_ID=${BLUECONE_NODE_ID} from ${RAW_ID}"

exec java -jar app.jar
```

要点：

- 使用哈希+取模，将任意字符串映射到 [0,1023]；
- 同一实例标识应保持稳定（平台通常保证 Pod 名/实例 ID 在生命周期内不变）。

### 3.3 有状态服务：StatefulSet ordinal（Kubernetes 模式）

如果微信云托管底层使用类似 StatefulSet 的有状态编排，可以使用 ordinal 作为 nodeId：

1. Pod 名通常形如：`app-0`、`app-1`、`app-2`；
2. 启动脚本解析 `-` 后缀作为 ordinal；
3. 将 ordinal（或加偏移量）作为 nodeId。

示例脚本：

```bash
POD_NAME="${POD_NAME:-app-0}"
ORDINAL="${POD_NAME##*-}"        # 例如 app-2 -> 2
NODE_ID="${ORDINAL}"             # 或者: NODE_ID=$((BASE + ORDINAL))

if [ "${NODE_ID}" -lt 0 ] || [ "${NODE_ID}" -gt 1023 ]; then
  echo "Invalid NODE_ID=${NODE_ID}, must be between 0 and 1023"
  exit 1
fi

export BLUECONE_NODE_ID="${NODE_ID}"
exec java -jar app.jar
```

## 4. 通用容器环境配置示例（Kubernetes）

### 4.1 StatefulSet + 环境变量

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: bluecone-app
spec:
  serviceName: bluecone-app
  replicas: 3
  selector:
    matchLabels:
      app: bluecone-app
  template:
    metadata:
      labels:
        app: bluecone-app
    spec:
      containers:
        - name: app
          image: your-image:tag
          env:
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
          command: ["bash", "-c"]
          args:
            - |
              ORDINAL="${POD_NAME##*-}"
              export BLUECONE_NODE_ID="${ORDINAL}"
              exec java -jar app.jar
```

要点：

- 保证 `replicas <= 1024`，否则需要重新设计位宽/ID 方案；
- 如需多集群部署，可为不同集群配置不同的偏移量（例如 `BASE_CLUSTER_OFFSET`），再加到 ordinal 上。

## 5. 排错与观测建议

- 启动失败且日志中包含：
  - “未配置节点 ID”：
    - 检查是否已设置 `bluecone.id.long.enabled=true`；
    - 检查配置中心/环境变量是否提供 `bluecone.id.long.node-id` 或 `BLUECONE_NODE_ID`。
  - “值必须在 [0, 1023] 范围内”：
    - 检查 nodeId 映射逻辑是否溢出（例如哈希取模或偏移量配置错误）。
- 建议在应用启动日志中打印当前实例解析出的 nodeId，便于排查多实例冲突问题。  
  （注意不要在业务日志中频繁输出，以免干扰正常观测。）

