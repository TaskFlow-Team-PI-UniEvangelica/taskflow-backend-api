package unievangelica.taskflow.api.infra.database;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Em produção, avisa no log se a API estiver conectada ao PostgreSQL como superusuário.
 * O esperado é o papel restrito criado por deploy/scripts/db-privilegio-minimo.sh (ALT-30);
 * enquanto ele não for aplicado, o compose usa o usuário antigo e este aviso deixa isso visível.
 */
@Slf4j
@Component
@Profile("prod")
public class AvisoUsuarioDeBanco implements ApplicationRunner {

    static final String CONSULTA = "SELECT rolsuper FROM pg_roles WHERE rolname = current_user";

    private final JdbcTemplate jdbcTemplate;

    public AvisoUsuarioDeBanco(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (Boolean.TRUE.equals(jdbcTemplate.queryForObject(CONSULTA, Boolean.class))) {
                log.warn("A API está conectada ao banco como SUPERUSUÁRIO. Aplique deploy/scripts/db-privilegio-minimo.sh "
                        + "e defina DB_APP_USER/DB_APP_PASSWORD e DB_OWNER_USER/DB_OWNER_PASSWORD no .env (ALT-30).");
            } else {
                log.info("A API está conectada ao banco com um papel sem privilégios de superusuário.");
            }
        } catch (RuntimeException e) {
            log.debug("Não foi possível verificar o papel do usuário de banco: {}", e.getMessage());
        }
    }
}
