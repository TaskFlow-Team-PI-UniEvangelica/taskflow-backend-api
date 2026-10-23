# Build maven usando a plataforma nativa do runner ($BUILDPLATFORM) para máxima velocidade
FROM --platform=$BUILDPLATFORM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app

# Cache das dependências Maven
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Compilação do código
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Runtime Temurin 21 (construído para a arquitetura de destino)
FROM eclipse-temurin:21-jre-jammy
RUN useradd --system --uid 10001 --no-create-home taskflowuser
WORKDIR /app

# Copia apenas o .jar gerado no estágio anterior
COPY --from=build /app/target/*.jar app.jar
USER taskflowuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
