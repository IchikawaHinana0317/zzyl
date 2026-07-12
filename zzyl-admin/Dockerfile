# Java 11 运行时环境
FROM eclipse-temurin:11-jre

# 容器中的工作目录
WORKDIR /app

# 设置时区
ENV TZ=Asia/Shanghai

# Jenkins 会在构建镜像前，将真正的可执行 jar 复制为 target/app.jar
COPY target/app.jar /app/app.jar

# Spring Boot 容器内部端口
EXPOSE 9000

# 启动 Spring Boot
ENTRYPOINT ["java", "-jar", "/app/app.jar"]