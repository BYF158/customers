# ---------- 构建 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- 运行 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m"
COPY --from=build /app/target/customer-visit-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
