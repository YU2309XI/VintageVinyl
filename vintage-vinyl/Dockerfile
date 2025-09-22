# ===== Build stage =====
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# ===== Run stage =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Set the active profile to 'prod' by default. Override in your environment if needed.
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

# JVM opts can be overridden at runtime: e.g., -Xmx512m
ENTRYPOINT ["java","-jar","/app/app.jar"]