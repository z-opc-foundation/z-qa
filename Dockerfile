# 基础镜像：Eclipse Temurin 8 JDK（支持 amd64/arm64）
FROM eclipse-temurin:8-jdk

LABEL maintainer="zifang"

# 设置时区 + 装 wget (供 HEALTHCHECK 使用)
RUN apt-get update && apt-get install -y --no-install-recommends tzdata wget && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apt-get remove -y tzdata && apt-get autoremove -y && apt-get clean

WORKDIR /app

# 前端 dist 已由 z-qa-admin 的 frontend-maven-plugin + copy-resources 打进 jar 的 static/
COPY z-qa-admin/target/z-qa-admin-*.jar app.jar

# 容器内监听 8080（覆盖 jar 内 application.yml 的 8083；宿主映射 8083:8080）
ENV SERVER_PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q -O- http://127.0.0.1:8080/doc.html || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
