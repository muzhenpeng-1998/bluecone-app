FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

# 指定构建目录
WORKDIR /workspace

# 拷贝 Maven 配置文件（使用国内镜像加速）
# 使用 mvn-config 而不是 .mvn 以避免微信云托管构建时的路径问题
COPY mvn-config/ .mvn/

# 仅拷贝 pom 文件，充分利用 Docker 层缓存
# 按照 pom.xml 中的模块顺序拷贝，确保构建依赖关系正确
COPY pom.xml ./

# Platform modules (must be built first)
COPY app-platform-bom/pom.xml app-platform-bom/
COPY app-id-api/pom.xml app-id-api/
COPY app-id/pom.xml app-id/
COPY app-core/pom.xml app-core/
COPY app-infra/pom.xml app-infra/
COPY app-ops/pom.xml app-ops/

# Platform starters
COPY app-platform-starter/pom.xml app-platform-starter/
COPY app-platform-starter-ops/pom.xml app-platform-starter-ops/
COPY app-platform-archkit/pom.xml app-platform-archkit/
COPY app-platform-codegen/pom.xml app-platform-codegen/

# Resource modules
COPY app-resource-api/pom.xml app-resource-api/
COPY app-resource/pom.xml app-resource/
COPY app-security/pom.xml app-security/

# Business modules
COPY app-tenant/pom.xml app-tenant/
COPY app-store/pom.xml app-store/
COPY app-product/pom.xml app-product/
COPY app-member-api/pom.xml app-member-api/
COPY app-member/pom.xml app-member/
COPY app-promo-api/pom.xml app-promo-api/
COPY app-promo/pom.xml app-promo/
COPY app-wallet-api/pom.xml app-wallet-api/
COPY app-wallet/pom.xml app-wallet/
COPY app-pricing-api/pom.xml app-pricing-api/
COPY app-pricing/pom.xml app-pricing/
COPY app-billing-api/pom.xml app-billing-api/
COPY app-billing/pom.xml app-billing/
COPY app-notify-api/pom.xml app-notify-api/
COPY app-notify/pom.xml app-notify/
COPY app-growth-api/pom.xml app-growth-api/
COPY app-growth/pom.xml app-growth/
COPY app-campaign-api/pom.xml app-campaign-api/
COPY app-campaign/pom.xml app-campaign/
COPY app-order/pom.xml app-order/
COPY app-payment/pom.xml app-payment/
COPY app-inventory/pom.xml app-inventory/
COPY app-wechat-api/pom.xml app-wechat-api/
COPY app-wechat/pom.xml app-wechat/

# Application module (must be last)
COPY app-application/pom.xml app-application/

# 预先下载依赖，提升重复构建速度
# 使用 -T 1C 参数启用多线程构建，加速依赖下载
# 使用 --settings 指定配置文件，启用国内镜像加速
RUN mvn -B -T 1C --settings .mvn/settings.xml -pl app-application -am dependency:go-offline || \
    mvn -B --settings .mvn/settings.xml -pl app-application -am dependency:resolve

# 拷贝完整源码并构建
COPY . .

# 构建项目，跳过测试，使用多线程加速构建
# 设置 Maven 参数以优化微信云托管环境的构建
RUN mvn -B -T 1C --settings .mvn/settings.xml -pl app-application -am clean package -DskipTests \
    -Dmaven.compiler.fork=true \
    -Dmaven.test.skip=true \
    -Dmaven.javadoc.skip=true

# 选择运行时基础镜像
FROM alpine:3.19

# 安装运行所需依赖并使用腾讯镜像，加速在微信云托管的构建
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.tencent.com/g' /etc/apk/repositories \
    && apk add --update --no-cache openjdk21-jre ca-certificates \
    && rm -f /var/cache/apk/*

# 容器默认时区为UTC，如需使用上海时间请启用以下时区设置命令
# RUN apk add tzdata && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo Asia/Shanghai > /etc/timezone

# 指定运行时的工作目录
WORKDIR /app

# 将构建产物 jar 包拷贝到运行时目录中
COPY --from=build /workspace/app-application/target/bluecone-app.jar /app/app.jar

# 暴露端口，并兼容云托管注入的 PORT 变量
EXPOSE 80

# 设置 JVM 参数和 Spring 配置
# - 优化内存使用，适配云托管环境
# - 使用 G1GC 垃圾收集器
# - 设置合理的堆内存大小
# - 启用 JVM 容器感知特性
# - 优化启动速度：使用分层编译、禁用字节码验证
ENV JAVA_OPTS="-XX:+UseG1GC \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseContainerSupport \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=1 \
    -Xverify:none \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Shanghai"

# 设置 Spring Profile 为 local（可通过环境变量覆盖）
ENV SPRING_PROFILES_ACTIVE=local

# 创建日志目录
RUN mkdir -p /app/logs

# 通过 PORT 变量监听微信云托管要求的端口
# 支持通过环境变量 SPRING_PROFILES_ACTIVE 覆盖 profile
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --server.port=${PORT:-80} --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
