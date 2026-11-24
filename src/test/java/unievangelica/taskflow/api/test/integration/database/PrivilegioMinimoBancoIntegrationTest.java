package unievangelica.taskflow.api.test.integration.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Executa deploy/scripts/db-privilegio-minimo.sql (ALT-30) num PostgreSQL 16 real, no cenário
 * de produção atual: schema criado pelo Flyway com o superusuário e banco do Keycloak na mesma
 * instância. Verifica o que o papel da API pode (DML) e não pode (DDL, superusuário, Keycloak,
 * histórico do Flyway) fazer, e que o dono do schema continua aplicando migrations.
 */
@Testcontainers(disabledWithoutDocker = true)
class PrivilegioMinimoBancoIntegrationTest {

    private static final String DONO = "taskflow_owner";
    private static final String SENHA_DONO = "senha-dono-teste";
    private static final String API = "taskflow_app";
    private static final String SENHA_API = "senha-api-teste";

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("taskflow")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("deploy/scripts/db-privilegio-minimo.sql")),
                    "/tmp/db-privilegio-minimo.sql");

    @BeforeAll
    static void cenarioDeProducaoEScript() throws Exception {
        // Produção atual: migrations aplicadas pelo superusuário
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        // Banco do Keycloak na mesma instância (como o init-multiple-dbs.sh faz)
        try (Connection c = conectar(POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement st = c.createStatement()) {
            st.execute("CREATE USER keycloak_teste WITH PASSWORD 'kc'");
            st.execute("CREATE DATABASE keycloak_teste OWNER keycloak_teste");
        }

        // Idempotência: o script roda duas vezes sem erro
        for (int vez = 1; vez <= 2; vez++) {
            ExecResult r = executarScript();
            assertThat(r.getExitCode()).as("execução %d: %s", vez, r.getStderr()).isZero();
            assertThat(r.getStdout()).contains("Privilégio mínimo aplicado com sucesso.");
        }
    }

    private static ExecResult executarScript() throws Exception {
        return POSTGRES.execInContainer("psql", "-v", "ON_ERROR_STOP=1",
                "-U", POSTGRES.getUsername(), "-d", POSTGRES.getDatabaseName(),
                "-v", "app_db=" + POSTGRES.getDatabaseName(),
                "-v", "kc_db=keycloak_teste", "-v", "kc_user=keycloak_teste",
                "-v", "owner_user=" + DONO, "-v", "owner_pass=" + SENHA_DONO,
                "-v", "app_user=" + API, "-v", "app_pass=" + SENHA_API,
                "-f", "/tmp/db-privilegio-minimo.sql");
    }

    private static Connection conectar(String usuario, String senha) throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), usuario, senha);
    }

    private static Connection comoApi() throws SQLException {
        return conectar(API, SENHA_API);
    }

    private static void falhaComo(Connection c, String sql) {
        assertThrows(SQLException.class, () -> {
            try (Statement st = c.createStatement()) {
                st.execute(sql);
            }
        }, sql);
    }

    @Test
    @DisplayName("Os dois papéis existem sem superusuário e sem poderes administrativos")
    void papeisSemPrivilegiosAdministrativos() throws Exception {
        try (Connection c = conectar(POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT rolname, rolsuper, rolcreatedb, rolcreaterole FROM pg_roles "
                     + "WHERE rolname IN ('" + DONO + "', '" + API + "') ORDER BY rolname")) {
            int papeis = 0;
            while (rs.next()) {
                papeis++;
                assertThat(rs.getBoolean("rolsuper")).as(rs.getString("rolname")).isFalse();
                assertThat(rs.getBoolean("rolcreatedb")).isFalse();
                assertThat(rs.getBoolean("rolcreaterole")).isFalse();
            }
            assertThat(papeis).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("API: lê e grava dados normalmente (inclusive a numeração atômica de tarefas)")
    void apiManipulaDados() throws Exception {
        try (Connection c = comoApi(); Statement st = c.createStatement()) {
            st.executeUpdate("INSERT INTO users (nome, email) VALUES ('Teste ALT-30', 'alt30@teste.local')");
            st.executeUpdate("UPDATE users SET nome = 'Teste ALT-30 editado' WHERE email = 'alt30@teste.local'");
            st.executeUpdate("INSERT INTO organizations (nome) VALUES ('Org ALT-30')");
            try (ResultSet rs = st.executeQuery("UPDATE organizations SET last_task_number = last_task_number + 1 "
                    + "WHERE nome = 'Org ALT-30' RETURNING last_task_number")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }
            assertThat(st.executeUpdate("DELETE FROM organizations WHERE nome = 'Org ALT-30'")).isEqualTo(1);
            assertThat(st.executeUpdate("DELETE FROM users WHERE email = 'alt30@teste.local'")).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("API: não altera a estrutura do banco (CREATE, DROP, ALTER, TRUNCATE)")
    void apiSemDdl() throws Exception {
        try (Connection c = comoApi()) {
            falhaComo(c, "CREATE TABLE intrusa (id int)");
            falhaComo(c, "DROP TABLE tasks");
            falhaComo(c, "ALTER TABLE users ADD COLUMN intrusa text");
            falhaComo(c, "TRUNCATE task_history");
            falhaComo(c, "CREATE FUNCTION intrusa() RETURNS int LANGUAGE sql AS 'SELECT 1'");
        }
    }

    @Test
    @DisplayName("API: sem poderes de superusuário (executar programas, ler arquivos, criar papéis)")
    void apiSemSuperusuario() throws Exception {
        try (Connection c = comoApi()) {
            falhaComo(c, "COPY (SELECT 1) TO PROGRAM 'id'");
            falhaComo(c, "SELECT pg_read_file('/etc/passwd')");
            falhaComo(c, "CREATE ROLE intruso LOGIN SUPERUSER");
            falhaComo(c, "ALTER ROLE " + API + " SUPERUSER");
        }
    }

    @Test
    @DisplayName("API: não altera o histórico do Flyway nem acessa o banco do Keycloak")
    void apiIsoladaDoFlywayEDoKeycloak() throws Exception {
        try (Connection c = comoApi()) {
            falhaComo(c, "SELECT * FROM flyway_schema_history");
            falhaComo(c, "DELETE FROM flyway_schema_history");
        }
        String urlKeycloak = POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/keycloak_teste");
        assertThrows(SQLException.class, () -> DriverManager.getConnection(urlKeycloak, API, SENHA_API).close());
        assertThrows(SQLException.class, () -> DriverManager.getConnection(urlKeycloak, DONO, SENHA_DONO).close());
        DriverManager.getConnection(urlKeycloak, "keycloak_teste", "kc").close();
    }

    @Test
    @DisplayName("Dono: o Flyway valida/aplica migrations e tabelas novas já nascem com DML para a API")
    void donoAplicaMigrationsEPrivilegiosPadrao() throws Exception {
        MigrateResult resultado = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), DONO, SENHA_DONO)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        assertThat(resultado.success).isTrue();

        try (Connection dono = conectar(DONO, SENHA_DONO); Statement st = dono.createStatement()) {
            st.execute("CREATE TABLE futura_migration (id serial PRIMARY KEY, valor text)");
        }
        try (Connection api = comoApi(); Statement st = api.createStatement()) {
            st.executeUpdate("INSERT INTO futura_migration (valor) VALUES ('ok')");
            assertThat(st.executeUpdate("DELETE FROM futura_migration")).isEqualTo(1);
        } finally {
            try (Connection dono = conectar(DONO, SENHA_DONO); Statement st = dono.createStatement()) {
                st.execute("DROP TABLE futura_migration");
            }
        }
    }
}
