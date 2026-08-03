FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml ./common/
COPY admin-service/pom.xml ./admin-service/
RUN mvn dependency:go-offline -pl common,admin-service -am -q
COPY common/src ./common/src
COPY admin-service/src ./admin-service/src
RUN mvn package -pl common,admin-service -am -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S smartqueue && adduser -S smartqueue -G smartqueue
COPY --from=builder /app/admin-service/target/*.jar app.jar
RUN chown smartqueue:smartqueue app.jar
USER smartqueue
EXPOSE 8085
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
