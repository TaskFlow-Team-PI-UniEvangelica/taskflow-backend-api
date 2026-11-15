package unievangelica.taskflow.api.test.unit.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import unievangelica.taskflow.api.infra.security.SpringSecurityConfig;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercita a cadeia real do {@link SpringSecurityConfig} (autenticação, rotas públicas, CORS e
 * conversão de papéis do JWT) em um contexto mínimo: sem banco, sem Keycloak e sem o
 * UserProvisioningFilter. O JwtDecoder é substituído por um decodificador de teste.
 */
@SpringJUnitWebConfig(classes = {SpringSecurityConfig.class, SpringSecurityConfigTest.TestConfig.class})
@TestPropertySource(properties = {
        "api.cors.frontend.url=" + SpringSecurityConfigTest.ORIGEM_FRONTEND,
        "api.cors.frontend.url.vite=" + SpringSecurityConfigTest.ORIGEM_VITE
})
public class SpringSecurityConfigTest {

    static final String ORIGEM_FRONTEND = "http://frontend.teste";
    static final String ORIGEM_VITE = "http://localhost:5173";
    static final String TOKEN_VALIDO = "token-valido";
    static final String MENSAGEM_401 = "Token de acesso ausente, inválido ou expirado.";

    @Configuration
    @EnableWebMvc
    static class TestConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                if (!TOKEN_VALIDO.equals(token)) {
                    throw new BadJwtException("assinatura inválida");
                }
                return Jwt.withTokenValue(token)
                        .header("alg", "RS256")
                        .subject("usuario-teste")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .build();
            };
        }

        @Bean
        RotasDeTesteController rotasDeTesteController() {
            return new RotasDeTesteController();
        }
    }

    @RestController
    static class RotasDeTesteController {
        @GetMapping("/protegido")
        String protegido() { return "ok"; }

        @GetMapping("/actuator/health")
        String health() { return "UP"; }

        @GetMapping("/swagger-ui/index.html")
        String swagger() { return "swagger"; }

        @GetMapping("/v3/api-docs")
        String apiDocs() { return "{}"; }

        @GetMapping("/error")
        String error() { return "erro"; }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SpringSecurityConfig springSecurityConfig;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // --- Autenticação ---

    @Test
    @DisplayName("Rota protegida sem token deve retornar 401 em ProblemDetail, mensagem fixa e WWW-Authenticate")
    void rotaProtegidaSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/protegido"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").value(MENSAGEM_401))
                .andExpect(jsonPath("$.instance").value("/protegido"))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(MENSAGEM_401))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Rota protegida com token inválido deve retornar 401")
    void rotaProtegidaComTokenInvalidoRetorna401() throws Exception {
        // A mensagem interna ("assinatura inválida") não pode chegar ao cliente
        mockMvc.perform(get("/protegido").header(HttpHeaders.AUTHORIZATION, "Bearer token-adulterado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(MENSAGEM_401));
    }

    @Test
    @DisplayName("Token malformado com aspas, barra invertida e chave deve gerar 401 com JSON válido")
    void tokenComCaracteresEspeciaisGeraJsonValido() throws Exception {
        mockMvc.perform(get("/protegido").header(HttpHeaders.AUTHORIZATION, "Bearer a\"b\\c}.d"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(MENSAGEM_401));
    }

    @Test
    @DisplayName("Rota protegida com token válido deve retornar 200")
    void rotaProtegidaComTokenValidoRetorna200() throws Exception {
        mockMvc.perform(get("/protegido").header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_VALIDO))
                .andExpect(status().isOk());
    }

    // --- Rotas públicas ---

    @Test
    @DisplayName("Health, Swagger, OpenAPI e /error devem ser acessíveis sem token")
    void rotasPublicasNaoExigemToken() throws Exception {
        for (String rota : List.of("/actuator/health", "/swagger-ui/index.html", "/v3/api-docs", "/error")) {
            mockMvc.perform(get(rota)).andExpect(status().isOk());
        }
    }

    // --- CORS ---

    @Test
    @DisplayName("Preflight de origem permitida deve ser aceito com cabeçalhos CORS")
    void preflightDeOrigemPermitida() throws Exception {
        for (String origem : List.of(ORIGEM_FRONTEND, ORIGEM_VITE)) {
            mockMvc.perform(options("/protegido")
                            .header(HttpHeaders.ORIGIN, origem)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origem))
                    // API stateless com token Bearer: não libera envio de credenciais (cookies) entre origens
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        }
    }

    @Test
    @DisplayName("Preflight de origem não permitida deve ser recusado sem cabeçalhos CORS")
    void preflightDeOrigemNaoPermitida() throws Exception {
        mockMvc.perform(options("/protegido")
                        .header(HttpHeaders.ORIGIN, "http://site-malicioso.teste")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Requisição autenticada de origem não permitida não deve receber cabeçalho CORS")
    void requisicaoDeOrigemNaoPermitidaSemCabecalhoCors() throws Exception {
        mockMvc.perform(get("/protegido")
                        .header(HttpHeaders.ORIGIN, "http://site-malicioso.teste")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    // --- Conversão de papéis do JWT ---

    @Test
    @DisplayName("Papéis de realm_access.roles devem virar ROLE_<PAPEL> em maiúsculas")
    void converteRealmAccessRoles() {
        Jwt jwt = jwtComClaims(Map.of("realm_access", Map.of("roles", List.of("admin", "offline_access"))));

        Set<String> autoridades = autoridadesDe(jwt);

        assertTrue(autoridades.containsAll(Set.of("ROLE_ADMIN", "ROLE_OFFLINE_ACCESS")), autoridades.toString());
    }

    @Test
    @DisplayName("Claim roles deve virar ROLE_<papel> e token sem papéis não deve gerar ROLE_*")
    void converteClaimRolesEAusenciaDePapeis() {
        Set<String> comRoles = autoridadesDe(jwtComClaims(Map.of("roles", List.of("gestor"))));
        Set<String> semPapeis = autoridadesDe(jwtComClaims(Map.of("email", "a@b.c")));

        assertTrue(comRoles.contains("ROLE_gestor"), comRoles.toString());
        // O Spring Security 7 acrescenta FACTOR_BEARER (fator de autenticação); papéis não devem existir.
        assertTrue(semPapeis.stream().noneMatch(a -> a.startsWith("ROLE_")), semPapeis.toString());
    }

    private Jwt jwtComClaims(Map<String, Object> claims) {
        return Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject("usuario-teste")
                .claims(c -> c.putAll(claims))
                .build();
    }

    private Set<String> autoridadesDe(Jwt jwt) {
        AbstractAuthenticationToken auth = springSecurityConfig.jwtAuthenticationConverter().convert(jwt);
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }
}
