# Build maven usando jdk 21
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app

# Copia código e dependências
COPY pom.xml .
COPY src ./src

# Compila apenas o necessário para o pacote da aplicação (rápido e sem dependências extras)
RUN mvn -B clean package -DskipTests

# Runtime Temurin 21
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Usuário não-root por segurança
RUN useradd -m taskflowuser
USER taskflowuser

# Copia apenas o .jar gerado no estágio anterior
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
