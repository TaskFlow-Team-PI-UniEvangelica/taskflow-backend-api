package unievangelica.taskflow.api.test.integration.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Aplicação completa (filtros de segurança, controllers, serviços, Flyway) contra PostgreSQL real.
 * Sem Docker disponível, a classe é ignorada (os testes unitários continuam rodando).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Import(PostgresTestcontainer.class)
@Testcontainers(disabledWithoutDocker = true)
public @interface IntegrationTest {
}
