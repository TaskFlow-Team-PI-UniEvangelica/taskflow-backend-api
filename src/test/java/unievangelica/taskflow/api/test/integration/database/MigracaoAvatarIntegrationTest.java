package unievangelica.taskflow.api.test.integration.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migração V12 (ALT-31) com dados existentes: o banco é levado até a V11 com avatares na tabela
 * users, migrado para a V12, e os avatares precisam estar em user_avatars byte a byte.
 */
@Testcontainers(disabledWithoutDocker = true)
class MigracaoAvatarIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    private static Flyway flywayAte(String versao) {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target(versao)
                .load();
    }

    @Test
    @DisplayName("V12 move os avatares (conteúdo e tipo) para user_avatars e remove as colunas de users")
    void moveAvataresExistentes() throws Exception {
        flywayAte("11").migrate();
        byte[] imagem = new byte[50_000];
        for (int i = 0; i < imagem.length; i++) {
            imagem[i] = (byte) (i * 31);
        }

        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users (nome, email, avatar, avatar_type) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, "Com avatar"); ps.setString(2, "com@teste.local"); ps.setBytes(3, imagem); ps.setString(4, "image/png");
                ps.executeUpdate();
                ps.setString(1, "Sem tipo"); ps.setString(2, "semtipo@teste.local"); ps.setBytes(3, new byte[]{1, 2}); ps.setString(4, null);
                ps.executeUpdate();
                ps.setString(1, "Sem avatar"); ps.setString(2, "sem@teste.local"); ps.setBytes(3, null); ps.setString(4, null);
                ps.executeUpdate();
            }

            flywayAte("12").migrate();

            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT u.email, a.content, a.content_type FROM user_avatars a "
                         + "JOIN users u ON u.id = a.user_id ORDER BY u.email")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("email")).isEqualTo("com@teste.local");
                assertThat(rs.getBytes("content")).isEqualTo(imagem);
                assertThat(rs.getString("content_type")).isEqualTo("image/png");
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("email")).isEqualTo("semtipo@teste.local");
                assertThat(rs.getString("content_type")).isEqualTo("image/jpeg");
                assertThat(rs.next()).as("usuário sem avatar não ganha linha").isFalse();
            }
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM information_schema.columns "
                         + "WHERE table_name = 'users' AND column_name IN ('avatar', 'avatar_type')")) {
                rs.next();
                assertThat(rs.getInt(1)).isZero();
            }
        }
    }
}
