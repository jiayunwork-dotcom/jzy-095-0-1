# syntax=docker/dockerfile:1

# ===== 构建阶段：Maven 编译，镜像构建过程中会执行全部自动化测试 =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先单独拷贝 pom 预热依赖缓存
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 拷贝源码（含 src/test 测试），package 前会先跑 test
COPY src ./src
RUN mvn -B clean package

# ===== 运行阶段：仅 JRE，固定 8080 端口对外提供 HTTP 调用 =====
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/notch-fatigue-1.0.0.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
