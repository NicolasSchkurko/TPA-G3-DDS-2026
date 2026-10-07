# ==========================
# Etapa 1: Build de Maven
# ==========================
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

ARG SERVICE_NAME=donaciones-service

COPY . .

RUN mvn -B -ntp package -pl ${SERVICE_NAME} -am -DskipTests


# ==========================
# Etapa 2: Imagen final (solo el JAR)
# ==========================
FROM eclipse-temurin:21-jre

# Metadata
LABEL maintainer="DDSI Project"
LABEL description="Donaciones Service - Spring Boot microservice"

WORKDIR /app

ARG SERVICE_NAME=donaciones-service

COPY --from=builder /app/${SERVICE_NAME}/target/${SERVICE_NAME}-*.jar app.jar

EXPOSE 8084

# Ejecutar con JVM optimizado para contenedores
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]