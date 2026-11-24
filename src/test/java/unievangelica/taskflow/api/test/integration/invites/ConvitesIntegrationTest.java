package unievangelica.taskflow.api.test.integration.invites;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import unievangelica.taskflow.api.domain.service.EmailService;
import unievangelica.taskflow.api.test.integration.support.IntegrationTest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Convites contra PostgreSQL real e com open-in-view desligado, como em produção: listagem sem
 * LazyInitializationException, reenvio após o vencimento e corrida entre aceite e recusa.
 */
@IntegrationTest
class ConvitesIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    // O reenvio publica o evento de e-mail: sem o mock, o teste dependeria de um SMTP
    @MockitoBean private EmailService emailService;

    private String emailDono;
    private String emailConvidado;
    private long orgId;
    private long donoId;

    @BeforeEach
    void preparar() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        emailDono = "dono-" + sufixo + "@teste.local";
        emailConvidado = "convidado-" + sufixo + "@teste.local";

        String org = mvc.perform(post("/organization").with(usuario(emailDono, true))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Org dos convites\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        orgId = ((Number) JsonPath.read(org, "$.id")).longValue();
        donoId = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, emailDono);
        mvc.perform(get("/user/me").with(usuario(emailConvidado, true))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Listar convites pendentes responde 200 com a organização e quem convidou")
    void listaConvitePendente() throws Exception {
        long conviteId = inserirConvitePendente(7);

        mvc.perform(get("/user/me/invites").with(usuario(emailConvidado, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(conviteId))
                .andExpect(jsonPath("$[0].organizationId").value(orgId))
                .andExpect(jsonPath("$[0].organizationName").value("Org dos convites"))
                .andExpect(jsonPath("$[0].inviterName").value("Teste Convite"));
    }

    @Test
    @DisplayName("Sem e-mail verificado no token, a lista de convites vem vazia")
    void listaVaziaSemEmailVerificado() throws Exception {
        inserirConvitePendente(7);

        mvc.perform(get("/user/me/invites").with(usuario(emailConvidado, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Reenviar após o vencimento expira o antigo e deixa um único convite pendente")
    void reenvioAposVencimento() throws Exception {
        long vencido = inserirConvitePendente(-3);
        String corpo = "{\"email\":\"" + emailConvidado + "\",\"role\":\"MEMBER\"}";

        mvc.perform(post("/organization/{id}/invites", orgId).with(usuario(emailDono, true))
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isOk());

        assertThat(statusDoConvite(vencido)).isEqualTo("EXPIRED");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM tb_organization_invites
                WHERE organization_id = ? AND invited_email = ? AND status = 'PENDING'""",
                Integer.class, orgId, emailConvidado)).isEqualTo(1);

        // Antes, o segundo pendente fazia a tentativa seguinte falhar com 500 (NonUniqueResultException)
        mvc.perform(post("/organization/{id}/invites", orgId).with(usuario(emailDono, true))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Já existe um convite pendente para este e-mail nesta organização."));
    }

    @Test
    @DisplayName("Aceite e recusa simultâneos: só um é processado e o outro responde 400")
    void aceiteERecusaSimultaneos() throws Exception {
        long conviteId = inserirConvitePendente(7);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // A linha fica travada até as duas requisições estarem esperando por ela: sem isso,
            // uma poderia terminar antes de a outra começar e a corrida não aconteceria
            List<Future<Integer>> respostas = new TransactionTemplate(transactionManager).execute(tx -> {
                jdbc.queryForObject("SELECT id FROM tb_organization_invites WHERE id = ? FOR UPDATE",
                        Long.class, conviteId);
                List<Future<Integer>> emAndamento = List.of(
                        executor.submit(() -> responder(conviteId, "accept")),
                        executor.submit(() -> responder(conviteId, "decline")));
                aguardarRequisicoesBloqueadas(2);
                return emAndamento;
            });

            int aceite = respostas.get(0).get(20, TimeUnit.SECONDS);
            int recusa = respostas.get(1).get(20, TimeUnit.SECONDS);

            assertThat(List.of(aceite, recusa)).containsExactlyInAnyOrder(200, 400);
            boolean aceito = aceite == 200;
            assertThat(statusDoConvite(conviteId)).isEqualTo(aceito ? "ACCEPTED" : "DECLINED");
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM organization_users ou JOIN users u ON u.id = ou.user_id
                    WHERE ou.organization_id = ? AND u.email = ?""", Integer.class, orgId, emailConvidado))
                    .isEqualTo(aceito ? 1 : 0);
        } finally {
            executor.shutdownNow();
        }
    }

    private static RequestPostProcessor usuario(String email, boolean emailVerificado) {
        return jwt().jwt(j -> j.subject("sub-" + email).claim("email", email)
                .claim("email_verified", emailVerificado)
                .claim("given_name", "Teste").claim("family_name", "Convite"));
    }

    private long inserirConvitePendente(int diasAteVencer) {
        return jdbc.queryForObject("""
                INSERT INTO tb_organization_invites (organization_id, inviter_id, invited_email, role, status, created_at, expires_at)
                VALUES (?, ?, ?, 'MEMBER', 'PENDING', now() - interval '10 days', now() + ? * interval '1 day')
                RETURNING id""", Long.class, orgId, donoId, emailConvidado, diasAteVencer);
    }

    private String statusDoConvite(long id) {
        return jdbc.queryForObject("SELECT status FROM tb_organization_invites WHERE id = ?", String.class, id);
    }

    private int responder(long conviteId, String acao) throws Exception {
        return mvc.perform(post("/user/me/invites/{id}/" + acao, conviteId).with(usuario(emailConvidado, true)))
                .andReturn().getResponse().getStatus();
    }

    private void aguardarRequisicoesBloqueadas(int quantidade) {
        long limite = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (System.nanoTime() < limite) {
            // O estado das conexões fica em cache durante a transação; sem limpar, a contagem não mudaria
            jdbc.execute("SELECT pg_stat_clear_snapshot()");
            Integer bloqueadas = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM pg_stat_activity
                    WHERE datname = current_database() AND wait_event_type = 'Lock'""", Integer.class);
            if (bloqueadas != null && bloqueadas >= quantidade) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("As requisições concorrentes não chegaram a esperar pela trava do convite");
    }
}
