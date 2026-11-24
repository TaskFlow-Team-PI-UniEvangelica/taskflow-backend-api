package unievangelica.taskflow.api.test.unit.database;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import unievangelica.taskflow.api.infra.database.AvisoUsuarioDeBanco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvisoUsuarioDeBancoTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(AvisoUsuarioDeBanco.class);

    @BeforeEach
    void capturarLogs() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void soltarLogs() {
        logger.detachAppender(logs);
    }

    @Test
    @DisplayName("Conectada como superusuário: registra WARN orientando a aplicar o privilégio mínimo")
    void avisaSuperusuario() {
        when(jdbc.queryForObject(anyString(), eq(Boolean.class))).thenReturn(true);

        new AvisoUsuarioDeBanco(jdbc).run(null);

        assertThat(logs.list).anySatisfy(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.WARN);
            assertThat(e.getFormattedMessage()).contains("SUPERUSUÁRIO").contains("db-privilegio-minimo.sh");
        });
    }

    @Test
    @DisplayName("Papel restrito: sem aviso; falha na consulta não impede a inicialização")
    void semAvisoComPapelRestrito() {
        when(jdbc.queryForObject(anyString(), eq(Boolean.class))).thenReturn(false);
        new AvisoUsuarioDeBanco(jdbc).run(null);
        assertThat(logs.list).noneMatch(e -> e.getLevel() == Level.WARN);

        when(jdbc.queryForObject(anyString(), eq(Boolean.class))).thenThrow(new DataAccessResourceFailureException("fora do ar"));
        new AvisoUsuarioDeBanco(jdbc).run(null);
        assertThat(logs.list).noneMatch(e -> e.getLevel() == Level.WARN);
    }
}
