FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY settings.gradle .
COPY build.gradle .
COPY gradle gradle
COPY gradlew .
COPY gradlew.bat .
COPY payment-service payment-service/
COPY common-dto common-dto/
WORKDIR /app/payment-service
RUN gradle bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/payment-service/build/libs/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
