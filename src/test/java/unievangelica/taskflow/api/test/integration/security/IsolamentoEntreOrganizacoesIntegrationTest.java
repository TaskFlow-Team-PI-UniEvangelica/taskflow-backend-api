package unievangelica.taskflow.api.test.integration.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import unievangelica.taskflow.api.test.integration.support.IntegrationTest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz BOLA (Broken Object Level Authorization) pela API completa: filtros de segurança,
 * provisionamento do usuário a partir do token, controllers, serviços e PostgreSQL real.
 * A Ana e o Bruno pertencem a organizações diferentes; o Bruno tenta ler, alterar e listar
 * dados da organização da Ana e deve ser barrado em todas as rotas, sem alterar nada.
 */
@IntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsolamentoEntreOrganizacoesIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    private final String sufixo = UUID.randomUUID().toString().substring(0, 8);
    private final RequestPostProcessor ana = usuario("ana");
    private final RequestPostProcessor bruno = usuario("bruno");

    private long orgDaAna;
    private long tarefaDaAna;
    private long idDaAna;
    private long orgDoBruno;

    private RequestPostProcessor usuario(String nome) {
        String email = nome + "-" + sufixo + "@teste.local";
        return jwt().jwt(j -> j.subject("sub-" + nome + "-" + sufixo)
                .claim("email", email)
                .claim("email_verified", true)
                .claim("given_name", nome)
                .claim("family_name", "Teste"));
    }

    private String json(MockHttpServletRequestBuilder req, RequestPostProcessor quem) throws Exception {
        return mvc.perform(req.with(quem)).andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
    }

    private static long id(String corpo) {
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    @BeforeAll
    void cenario() throws Exception {
        idDaAna = id(json(get("/user/me"), ana));
        orgDaAna = id(json(post("/organization").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Org da Ana\"}"), ana));
        tarefaDaAna = id(json(post("/task").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Tarefa da Ana\",\"organizationId\":" + orgDaAna + "}"), ana));
        orgDoBruno = id(json(post("/organization").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Org do Bruno\"}"), bruno));
    }

    static Stream<Arguments> rotasDaOrganizacaoDaAna() {
        return Stream.of(
                Arguments.of("GET", "/task?organizationId={org}", null, 403),
                Arguments.of("POST", "/task", "{\"titulo\":\"intrusa\",\"organizationId\":{org}}", 403),
                Arguments.of("PUT", "/task/{task}", "{\"titulo\":\"alterada pelo Bruno\"}", 403),
                Arguments.of("PATCH", "/task/{task}/status", "{\"status\":\"concluida\"}", 403),
                Arguments.of("DELETE", "/task/{task}", null, 403),
                Arguments.of("GET", "/task/{task}/history", null, 403),
                Arguments.of("GET", "/task/organization/{org}/history", null, 403),
                Arguments.of("GET", "/task/organization/{org}/deleted-history", null, 403),
                Arguments.of("PUT", "/organization/{org}", "{\"nome\":\"tomada\"}", 403),
                // 400 (e não 403): deletarOrganizacao e transferirPosse tratam "não é membro" como
                // BusinessException. O acesso é barrado; a padronização para 403 depende de autorização.
                Arguments.of("DELETE", "/organization/{org}", null, 400),
                Arguments.of("GET", "/organization/{org}/members", null, 403),
                Arguments.of("PUT", "/organization/{org}/members/{ana}/role", "{\"role\":\"MEMBER\"}", 403),
                Arguments.of("PUT", "/organization/{org}/members/{ana}/transfer-ownership", null, 400),
                Arguments.of("DELETE", "/organization/{org}/members/{ana}", null, 403),
                Arguments.of("POST", "/organization/{org}/invites", "{\"email\":\"intruso@teste.local\",\"role\":\"SUB_OWNER\"}", 403),
                Arguments.of("GET", "/user/{ana}/avatar", null, 404),
                Arguments.of("PUT", "/user/{ana}", "{\"nome\":\"renomeada\"}", 403),
                Arguments.of("DELETE", "/user/{ana}", null, 403)
        );
    }

    @ParameterizedTest(name = "Bruno {0} {1} -> {3}")
    @MethodSource("rotasDaOrganizacaoDaAna")
    @DisplayName("Usuário de outra organização é barrado")
    void outraOrganizacaoEBarrada(String metodo, String rota, String corpo, int esperado) throws Exception {
        String caminho = rota.replace("{org}", String.valueOf(orgDaAna))
                .replace("{task}", String.valueOf(tarefaDaAna))
                .replace("{ana}", String.valueOf(idDaAna));
        MockHttpServletRequestBuilder req = request(HttpMethod.valueOf(metodo), caminho);
        if (corpo != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(corpo.replace("{org}", String.valueOf(orgDaAna)));
        }

        mvc.perform(req.with(bruno)).andExpect(status().is(esperado));

        // Nada da Ana foi alterado
        assertThat(jdbc.queryForObject("SELECT titulo FROM tasks WHERE id = ?", String.class, tarefaDaAna)).isEqualTo("Tarefa da Ana");
        assertThat(jdbc.queryForObject("SELECT status FROM tasks WHERE id = ?", String.class, tarefaDaAna)).isEqualTo("pendente");
        assertThat(jdbc.queryForObject("SELECT nome FROM organizations WHERE id = ?", String.class, orgDaAna)).isEqualTo("Org da Ana");
        assertThat(jdbc.queryForObject("SELECT role FROM organization_users WHERE organization_id = ? AND user_id = ?",
                String.class, orgDaAna, idDaAna)).isEqualTo("OWNER");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM organization_users WHERE organization_id = ?", Integer.class, orgDaAna)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT nome FROM users WHERE id = ?", String.class, idDaAna)).isEqualTo("ana Teste");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tasks WHERE organization_id = ?", Integer.class, orgDaAna)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_organization_invites WHERE organization_id = ?", Integer.class, orgDaAna)).isZero();
    }

    @Test
    @DisplayName("Listagens gerais do Bruno não trazem dados da organização da Ana")
    void listagensNaoVazamDados() throws Exception {
        String tarefas = json(get("/task"), bruno);
        String organizacoes = json(get("/organization/me"), bruno);

        List<Integer> idsTarefas = JsonPath.read(tarefas, "$[*].id");
        List<Integer> idsOrgs = JsonPath.read(organizacoes, "$[*].id");
        assertThat(idsTarefas).doesNotContain((int) tarefaDaAna);
        assertThat(idsOrgs).containsExactly((int) orgDoBruno);
    }

    @Test
    @DisplayName("Controle positivo: a própria Ana acessa a sua organização e a sua tarefa")
    void donaAcessaSeusDados() throws Exception {
        String tarefas = json(get("/task").param("organizationId", String.valueOf(orgDaAna)), ana);
        List<Integer> ids = JsonPath.read(tarefas, "$[*].id");
        assertThat(ids).containsExactly((int) tarefaDaAna);

        mvc.perform(get("/organization/{id}/members", orgDaAna).with(ana)).andExpect(status().isOk());
        mvc.perform(get("/task/{id}/history", tarefaDaAna).with(ana)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sem token, rotas protegidas respondem 401")
    void semToken() throws Exception {
        mvc.perform(get("/task").param("organizationId", String.valueOf(orgDaAna))).andExpect(status().isUnauthorized());
    }
}
