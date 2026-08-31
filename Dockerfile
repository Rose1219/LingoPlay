# syntax=docker/dockerfile:1

# ---------- 阶段 1：构建前端 ----------
FROM node:20-alpine AS web-build
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# ---------- 阶段 2：构建后端（内嵌前端产物） ----------
FROM maven:3.9.6-eclipse-temurin-8 AS app-build
WORKDIR /app
COPY backend/pom.xml ./
COPY backend/src ./src
COPY --from=web-build /web/dist ./src/main/resources/static
RUN mvn -B package -DskipTests

# ---------- 阶段 3：运行时 ----------
FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=app-build /app/target/*.jar ./app.jar
ENV TZ=Asia/Shanghai
# 端口由 PORT 环境变量决定（application.yml: server.port=${PORT:8080}），默认绑定 0.0.0.0
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]