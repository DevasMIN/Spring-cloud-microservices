# Build stage
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app

# Copy gradle files first for better caching
COPY settings.gradle .
COPY build.gradle .
COPY gradle gradle
COPY gradlew .
COPY gradlew.bat .

# Copy source code
COPY order-service order-service/
COPY common-dto common-dto/

# Build using gradle wrapper
WORKDIR /app/order-service
RUN gradle bootJar -x test --no-daemon

# Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/order-service/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
