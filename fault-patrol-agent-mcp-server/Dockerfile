# fault-patrol-agent 巡检工具 MCP 服务器镜像
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
COPY docker/maven-settings.xml /root/.m2/settings.xml
RUN --mount=type=cache,target=/root/.m2/repository mvn -q -s /root/.m2/settings.xml clean package -pl fault-patrol-agent-mcp-server -am -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/fault-patrol-agent-mcp-server/target/fault-patrol-agent-mcp-server.jar app.jar
EXPOSE 8092
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
