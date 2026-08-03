FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml ./common/
COPY analytics-service/pom.xml ./analytics-service/
RUN mvn dependency:go-offline -pl common,analytics-service -am -q
COPY common/src ./common/src
COPY analytics-service/src ./analytics-service/src
RUN mvn package -pl common,analytics-service -am -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S smartqueue && adduser -S smartqueue -G smartqueue
COPY --from=builder /app/analytics-service/target/*.jar app.jar
RUN chown smartqueue:smartqueue app.jar
USER smartqueue
EXPOSE 8084
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
