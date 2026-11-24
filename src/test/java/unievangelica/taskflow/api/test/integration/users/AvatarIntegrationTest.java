package unievangelica.taskflow.api.test.integration.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import unievangelica.taskflow.api.test.integration.support.IntegrationTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo do avatar pela API completa, com o avatar em user_avatars (ALT-31): envio, troca,
 * leitura, exclusão e remoção em cascata quando o usuário é excluído.
 */
@IntegrationTest
class AvatarIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 7, 7};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 9};

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    private static RequestPostProcessor usuario(String email) {
        return jwt().jwt(j -> j.subject("sub-" + email).claim("email", email).claim("email_verified", true)
                .claim("given_name", "Teste").claim("family_name", "Avatar"));
    }

    @Test
    @DisplayName("Envia, troca, lê e exclui o avatar; ao excluir a conta, o avatar sai junto")
    void cicloDoAvatar() throws Exception {
        String email = "avatar-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.local";
        RequestPostProcessor eu = usuario(email);
        mvc.perform(get("/user/me").with(eu)).andExpect(status().isOk());
        long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);

        mvc.perform(get("/user/{id}/avatar", id).with(eu)).andExpect(status().isNotFound());

        mvc.perform(multipart("/user/me/avatar").file(new MockMultipartFile("file", "a.png", "image/png", PNG)).with(eu))
                .andExpect(status().isOk());
        mvc.perform(get("/user/{id}/avatar", id).with(eu))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(content().bytes(PNG));

        // Troca: continua uma única linha, agora JPEG
        mvc.perform(multipart("/user/me/avatar").file(new MockMultipartFile("file", "b.jpg", "image/jpeg", JPEG)).with(eu))
                .andExpect(status().isOk());
        mvc.perform(get("/user/{id}/avatar", id).with(eu))
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(content().bytes(JPEG));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_avatars WHERE user_id = ?", Integer.class, id)).isEqualTo(1);

        mvc.perform(delete("/user/me/avatar").with(eu)).andExpect(status().isNoContent());
        mvc.perform(get("/user/{id}/avatar", id).with(eu)).andExpect(status().isNotFound());

        // Cascata: excluir a conta remove o avatar
        mvc.perform(multipart("/user/me/avatar").file(new MockMultipartFile("file", "a.png", "image/png", PNG)).with(eu))
                .andExpect(status().isOk());
        mvc.perform(delete("/user/{id}", id).with(eu)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_avatars WHERE user_id = ?", Integer.class, id)).isZero();
    }
}
