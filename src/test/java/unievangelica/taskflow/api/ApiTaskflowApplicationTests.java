package unievangelica.taskflow.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import unievangelica.taskflow.api.test.integration.support.IntegrationTest;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sobe a aplicação inteira contra PostgreSQL real: valida a ligação dos beans, todas as consultas
 * JPQL (validadas na inicialização) e a aplicação das migrations do Flyway do zero.
 */
@IntegrationTest
class ApiTaskflowApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("Contexto sobe e o Flyway aplica todas as migrations com sucesso")
	void contextLoads() throws Exception {
		Integer falhas = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE NOT success", Integer.class);
		String ultimaVersao = jdbcTemplate.queryForObject(
				"SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1", String.class);

		assertThat(falhas).isZero();
		assertThat(ultimaVersao).isEqualTo(String.valueOf(maiorMigrationDoProjeto()));
	}

	/** Maior número de versão entre os arquivos V*__*.sql de db/migration. */
	private static int maiorMigrationDoProjeto() throws Exception {
		Resource[] arquivos = new PathMatchingResourcePatternResolver().getResources("classpath:db/migration/V*__*.sql");
		return Arrays.stream(arquivos)
				.map(Resource::getFilename)
				.mapToInt(nome -> Integer.parseInt(nome.substring(1, nome.indexOf("__"))))
				.max()
				.orElseThrow();
	}
}
