# Stage 1: Build using Maven
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
# Using the maven wrapper provided in your projects
RUN ./mvnw clean package -DskipTests

# Stage 2: Optimized JRE for banking security
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Non-root user for security compliance
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar
# Optimized for cloud/containers
ENTRYPOINT ["java", "-XX:+UseParallelGC", "-jar", "app.jar"]