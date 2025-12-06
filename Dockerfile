FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

# 指定构建目录
WORKDIR /workspace

# 仅拷贝 pom 文件，充分利用 Docker 层缓存
COPY pom.xml ./
COPY app-core/pom.xml app-core/
COPY app-id/pom.xml app-id/
COPY app-infra/pom.xml app-infra/
COPY app-security/pom.xml app-security/
COPY app-order/pom.xml app-order/
COPY app-payment/pom.xml app-payment/
COPY app-tenant/pom.xml app-tenant/
COPY app-store/pom.xml app-store/
COPY app-product/pom.xml app-product/
COPY app-inventory/pom.xml app-inventory/
COPY app-application/pom.xml app-application/

# 预先下载依赖，提升重复构建速度
RUN mvn -B -pl app-application -am dependency:go-offline

# 拷贝完整源码并构建
COPY . .
RUN mvn -B -pl app-application -am clean package -DskipTests

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
ENV JAVA_OPTS=""

# 通过 PORT 变量监听微信云托管要求的端口
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --server.port=${PORT:-80}"]
