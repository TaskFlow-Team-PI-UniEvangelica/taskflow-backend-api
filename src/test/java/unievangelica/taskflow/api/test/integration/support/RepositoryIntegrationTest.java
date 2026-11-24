package unievangelica.taskflow.api.test.integration.support;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Camada JPA (repositórios + Flyway) contra PostgreSQL real; cada teste roda em transação revertida.
 * Sem Docker disponível, a classe é ignorada.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("it")
@Import(PostgresTestcontainer.class)
@Testcontainers(disabledWithoutDocker = true)
public @interface RepositoryIntegrationTest {
}
