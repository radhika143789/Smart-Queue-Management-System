FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml ./common/
COPY notification-service/pom.xml ./notification-service/
RUN mvn dependency:go-offline -pl common,notification-service -am -q
COPY common/src ./common/src
COPY notification-service/src ./notification-service/src
RUN mvn package -pl common,notification-service -am -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S smartqueue && adduser -S smartqueue -G smartqueue
COPY --from=builder /app/notification-service/target/*.jar app.jar
RUN chown smartqueue:smartqueue app.jar
USER smartqueue
EXPOSE 8083
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
