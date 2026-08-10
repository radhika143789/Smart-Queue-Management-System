# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
# Copy parent pom and install it first (cache layer)
COPY pom.xml .
COPY common/pom.xml ./common/
COPY auth-service/pom.xml ./auth-service/
RUN mvn dependency:go-offline -pl common,auth-service -am -q
# Copy source
COPY common/src ./common/src
COPY auth-service/src ./auth-service/src
# Build
RUN mvn package -pl common,auth-service -am -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S smartqueue && adduser -S smartqueue -G smartqueue
COPY --from=builder /app/auth-service/target/auth-service-1.0.0.jar app.jar
RUN chown smartqueue:smartqueue app.jar
USER smartqueue
EXPOSE 8081
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
