# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml ./common/
COPY gateway/pom.xml ./gateway/
RUN mvn dependency:go-offline -pl common,gateway -am -q
COPY common/src ./common/src
COPY gateway/src ./gateway/src
RUN mvn package -pl common,gateway -am -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S smartqueue && adduser -S smartqueue -G smartqueue
COPY --from=builder /app/gateway/target/gateway-1.0.0.jar app.jar
RUN chown smartqueue:smartqueue app.jar
USER smartqueue
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
