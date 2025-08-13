# -----------------------------------------------------------------------------
# Multi-stage Dockerfile to build and run the Spring Boot app (with Maven)
# -----------------------------------------------------------------------------

# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

# Copy Maven project files first to leverage Docker layer caching
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -q -e -DskipTests dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Build the application (package the jar)
RUN --mount=type=cache,target=/root/.m2 mvn -q -e -DskipTests package

# Stage 2: Run with JRE only
FROM eclipse-temurin:17-jre

ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV TZ=UTC

WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /workspace/target/student-matching-app-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]