# --- Etapa 1: build ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# --- Etapa 2: runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/target/*.jar app.jar
# Directorio de la llave RSA del IdP, montado como volumen para que la firma
# sobreviva a los reinicios y el JWKS cacheado por AWS siga siendo valido.
RUN mkdir -p /app/llaves && chown spring:spring /app/llaves
VOLUME ["/app/llaves"]

USER spring:spring
EXPOSE 9000
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
