# fault-patrol-agent 主应用镜像（多阶段构建）
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
COPY docker/maven-settings.xml /root/.m2/settings.xml
RUN --mount=type=cache,target=/root/.m2/repository mvn -q -s /root/.m2/settings.xml clean package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/fault-patrol-agent-app/target/fault-patrol-agent-app.jar app.jar
EXPOSE 8091
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
