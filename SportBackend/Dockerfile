# ========== 构建阶段：编译 Spring Boot 可执行 jar ==========
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 先拷贝 pom.xml，充分利用 Maven 依赖层缓存
COPY pom.xml .
RUN mvn -q dependency:go-offline || true

COPY src ./src
RUN mvn -q clean package -DskipTests

# ========== 运行阶段：最小 JRE 镜像 ==========
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=Asia/Shanghai

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
