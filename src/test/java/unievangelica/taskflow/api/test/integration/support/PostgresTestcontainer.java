package unievangelica.taskflow.api.test.integration.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL real em container para os testes de integração (mesma versão de produção).
 * O @ServiceConnection faz o Spring usar este banco no lugar do configurado em DB_URL.
 * Cada contexto de teste do Spring tem o seu container; o Flyway aplica as migrations do zero.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainer {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
    }
}
