package unievangelica.taskflow.api.test.unit.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.infra.security.UserProvisioningFilter;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserProvisioningFilterTest {

    private static final String SUB = "sub-novo";
    private static final String EMAIL = "pessoa@exemplo.com";

    @Mock
    private UserRepository userRepository;

    private UserProvisioningFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new UserProvisioningFilter(userRepository);
        request = new MockHttpServletRequest("GET", "/user/me");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarCom(Map<String, Object> claims) {
        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject(SUB)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claims(c -> c.putAll(claims))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private static UserEntity linhaExistente(String keycloakId) {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setEmail(EMAIL);
        user.setNome("Pré-cadastrado");
        user.setKeycloakId(keycloakId);
        return user;
    }

    private void assertSeguiuACadeia() {
        assertThat(chain.getRequest()).as("a requisição deve seguir para o controller").isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private void assertConflito() throws Exception {
        assertThat(chain.getRequest()).as("a requisição não deve seguir").isNull();
        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString())
                .contains("\"status\":409")
                .contains("Não foi possível vincular sua conta")
                .doesNotContain(SUB);
    }

    @Test
    @DisplayName("Usuário já provisionado (sub conhecido) segue sem gravar nada")
    void usuarioExistenteNaoGrava() throws Exception {
        autenticarCom(Map.of("email", EMAIL, "email_verified", true));
        when(userRepository.findByKeycloakId(SUB)).thenReturn(Optional.of(linhaExistente(SUB)));

        filter.doFilter(request, response, chain);

        assertSeguiuACadeia();
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Usuário novo (sem linha com o mesmo e-mail) é criado com nome, e-mail e sub do token")
    void usuarioNovoECriado() throws Exception {
        autenticarCom(Map.of("email", EMAIL, "email_verified", false, "given_name", "Ana", "family_name", "Lima"));
        when(userRepository.findByKeycloakId(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        ArgumentCaptor<UserEntity> salvo = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(salvo.capture());
        assertThat(salvo.getValue().getKeycloakId()).isEqualTo(SUB);
        assertThat(salvo.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(salvo.getValue().getNome()).isEqualTo("Ana Lima");
        assertSeguiuACadeia();
    }

    @Test
    @DisplayName("Sem given/family name usa preferred_username; sem nenhum, 'Novo Usuário'")
    void montagemDoNome() throws Exception {
        autenticarCom(Map.of("email", EMAIL, "preferred_username", " ana.lima "));
        when(userRepository.findByKeycloakId(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        ArgumentCaptor<UserEntity> salvo = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(salvo.capture());
        assertThat(salvo.getValue().getNome()).isEqualTo("ana.lima");

        SecurityContextHolder.clearContext();
        autenticarCom(Map.of("email", "outra@exemplo.com"));
        when(userRepository.findByEmail("outra@exemplo.com")).thenReturn(Optional.empty());
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(userRepository, org.mockito.Mockito.times(2)).save(salvo.capture());
        assertThat(salvo.getValue().getNome()).isEqualTo("Novo Usuário");
    }

    @Test
    @DisplayName("E-mail verificado e linha sem dono: vincula o sub à linha existente")
    void vinculaComEmailVerificadoELinhaLivre() throws Exception {
        UserEntity existente = linhaExistente(null);
        autenticarCom(Map.of("email", EMAIL, "email_verified", true));
        when(userRepository.findByKeycloakId(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existente));

        filter.doFilter(request, response, chain);

        assertThat(existente.getKeycloakId()).isEqualTo(SUB);
        verify(userRepository).save(existente);
        assertSeguiuACadeia();
    }

    @Test
    @DisplayName("E-mail NÃO verificado: recusa o vínculo com 409 e não grava (ataque de pré-cadastro / troca de e-mail)")
    void recusaEmailNaoVerificado() throws Exception {
        UserEntity existente = linhaExistente(null);
        autenticarCom(Map.of("email", EMAIL, "email_verified", false));
        when(userRepository.findByKeycloakId(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existente));

        filter.doFilter(request, response, chain);

        assertConflito();
        assertThat(existente.getKeycloakId()).isNull();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Claim email_verified ausente é tratada como não verificada")
    void recusaSemClaimEmailVerified() throws Exception {
        autenticarCom(Map.of("email", EMAIL));
        when(userRepository.findByKeycloakId(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(linhaExistente(null)));

        filter.doFilter(request, response, chain);

        assertConflito();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Linha já pertence a outro sub: recusa com 409 mesmo com e-mail verificado (não sobrescreve keycloakId)")
    void recusaLinhaDeOutraConta() throws Exception {
        UserEntity deOutro = linhaExistente("sub-do-dono");
        autenticarCom(Map.of("email", EMAIL, "email_verified", true));
        when(userRepository.findByKeycloakId(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(deOutro));

        filter.doFilter(request, response, chain);

        assertConflito();
        assertThat(deOutro.getKeycloakId()).isEqualTo("sub-do-dono");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("E-mail com maiúsculas e espaços é normalizado antes de buscar e gravar")
    void normalizaEmailAntesDeBuscarEGravar() throws Exception {
        autenticarCom(Map.of("email", "  Ana@Exemplo.COM ", "email_verified", true));
        when(userRepository.findByKeycloakId(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ana@exemplo.com")).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        ArgumentCaptor<UserEntity> salvo = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).findByEmail("ana@exemplo.com");
        verify(userRepository).save(salvo.capture());
        assertThat(salvo.getValue().getEmail()).isEqualTo("ana@exemplo.com");
        assertSeguiuACadeia();
    }

    @Test
    @DisplayName("Token sem claim email ou em branco é recusado com 409 sem gravar")
    void recusaTokenSemEmail() throws Exception {
        autenticarCom(Map.of("given_name", "SemEmail"));
        when(userRepository.findByKeycloakId(SUB)).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        assertConflito();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Corrida no primeiro login (sub já gravado por outra thread) é absorvida e a requisição segue")
    void corridaNoPrimeiroLogin() throws Exception {
        autenticarCom(Map.of("email", EMAIL, "email_verified", true));
        when(userRepository.findByKeycloakId(SUB)).thenReturn(Optional.empty(), Optional.of(linhaExistente(SUB)));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenThrow(new DataIntegrityViolationException("users_keycloak_id_key"));

        filter.doFilter(request, response, chain);

        assertSeguiuACadeia();
    }

    @Test
    @DisplayName("Violação de unicidade sem o sub persistido (conflito de e-mail concorrente) responde 409")
    void conflitoDeEmailConcorrenteRetorna409() throws Exception {
        autenticarCom(Map.of("email", EMAIL, "email_verified", true));
        when(userRepository.findByKeycloakId(SUB)).thenReturn(Optional.empty(), Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenThrow(new DataIntegrityViolationException("users_email_key"));

        filter.doFilter(request, response, chain);

        assertConflito();
    }

    @Test
    @DisplayName("Outros erros de banco não são mais engolidos")
    void outrosErrosNaoSaoEngolidos() {
        autenticarCom(Map.of("email", EMAIL, "email_verified", true));
        when(userRepository.findByKeycloakId(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenThrow(new DataAccessResourceFailureException("banco fora do ar"));

        assertThrows(DataAccessResourceFailureException.class, () -> filter.doFilter(request, response, chain));
    }

    @Test
    @DisplayName("Requisição sem autenticação JWT segue sem consultar o banco")
    void semJwtNaoConsultaBanco() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonimo", null));

        filter.doFilter(request, response, chain);

        assertSeguiuACadeia();
        verifyNoInteractions(userRepository);
    }
}
