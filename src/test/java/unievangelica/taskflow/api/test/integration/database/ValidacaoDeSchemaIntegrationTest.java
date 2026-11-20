package unievangelica.taskflow.api.test.integration.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.test.context.TestPropertySource;
import unievangelica.taskflow.api.test.integration.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Produção sobe com spring.jpa.hibernate.ddl-auto=validate: o Hibernate confere se o schema criado
 * pelas migrations corresponde às entidades e, se não corresponder, a aplicação não inicia.
 * Este teste sobe a aplicação inteira com a mesma validação contra um PostgreSQL migrado do zero.
 */
@IntegrationTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class ValidacaoDeSchemaIntegrationTest {

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("O schema das migrations passa na validação do Hibernate usada em produção")
    void schemaValidoComoEmProducao() throws Exception {
        String emProducao = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-prod.properties"))
                .getProperty("spring.jpa.hibernate.ddl-auto");

        assertThat(emProducao).as("o teste espelha a configuração de produção").isEqualTo("validate");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        // Chegar aqui significa que o contexto (e a validação do schema) subiu sem erro
    }
}
