# Build maven usando jdk 21
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Runtime com temurin 21

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# criar um usuário não-root por boas práticas de segurança de api
RUN useradd -m taskflowuser
USER taskflowuser

# copia apenas o .jar gerado no estágio anterior
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]