package unievangelica.taskflow.api.test.integration.invites;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import unievangelica.taskflow.api.test.integration.support.IntegrationTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aceite de convite vencido, com transação real: a resposta é 400 "Este convite expirou." e o
 * convite fica gravado como EXPIRED (antes, a exceção desfazia a mudança de status no rollback).
 */
@IntegrationTest
class ConviteExpiradoIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    private static RequestPostProcessor usuario(String email) {
        return jwt().jwt(j -> j.subject("sub-" + email).claim("email", email).claim("email_verified", true)
                .claim("given_name", "Teste").claim("family_name", "Convite"));
    }

    @Test
    @DisplayName("Aceitar convite vencido responde 400 e grava o status EXPIRED; o convidado não entra na organização")
    void conviteVencidoFicaExpirado() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String emailDono = "dono-" + sufixo + "@teste.local";
        String emailConvidado = "convidado-" + sufixo + "@teste.local";

        String org = mvc.perform(post("/organization").with(usuario(emailDono))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Org convite vencido\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long orgId = ((Number) JsonPath.read(org, "$.id")).longValue();
        long donoId = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, emailDono);
        mvc.perform(get("/user/me").with(usuario(emailConvidado))).andExpect(status().isOk());

        Long conviteId = jdbc.queryForObject("""
                INSERT INTO tb_organization_invites (organization_id, inviter_id, invited_email, role, status, created_at, expires_at)
                VALUES (?, ?, ?, 'MEMBER', 'PENDING', now() - interval '10 days', now() - interval '3 days')
                RETURNING id""", Long.class, orgId, donoId, emailConvidado);

        mvc.perform(post("/user/me/invites/{id}/accept", conviteId).with(usuario(emailConvidado)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Este convite expirou."));

        assertThat(jdbc.queryForObject("SELECT status FROM tb_organization_invites WHERE id = ?", String.class, conviteId))
                .isEqualTo("EXPIRED");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM organization_users ou JOIN users u ON u.id = ou.user_id
                WHERE ou.organization_id = ? AND u.email = ?""", Integer.class, orgId, emailConvidado)).isZero();

        // Nova tentativa: o convite já não está pendente
        mvc.perform(post("/user/me/invites/{id}/accept", conviteId).with(usuario(emailConvidado)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Este convite já foi processado."));
    }
}
