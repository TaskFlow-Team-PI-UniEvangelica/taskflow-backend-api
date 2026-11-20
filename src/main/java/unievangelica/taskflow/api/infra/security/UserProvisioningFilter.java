package unievangelica.taskflow.api.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.infra.exceptions.ProblemDetails;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/**
 * Provisionamento JIT: no primeiro acesso de um token (sub) desconhecido, cria o usuário local ou vincula
 * a conta do Keycloak a uma linha existente com o mesmo e-mail.
 *
 * O vínculo por e-mail só acontece se o e-mail do token estiver verificado ({@code email_verified=true})
 * e a linha ainda não pertencer a nenhuma conta ({@code keycloakId} nulo). Caso contrário a requisição é
 * recusada com 409, sem gravar nada: um token com e-mail não verificado (ex.: e-mail trocado no Account
 * Console) não pode assumir a linha, as organizações e as tarefas de outra pessoa.
 */
@Slf4j
@Component
public class UserProvisioningFilter extends OncePerRequestFilter {

    static final String MENSAGEM_CONFLITO =
            "Não foi possível vincular sua conta. Verifique seu e-mail ou contate o suporte.";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final UserRepository userRepository;

    public UserProvisioningFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtToken && !provisionUserIfNecessary(jwtToken.getToken())) {
            escreverConflito(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** @return false quando a conta não pode ser vinculada nem criada (conflito de e-mail). */
    private boolean provisionUserIfNecessary(Jwt jwt) {
        String keycloakId = jwt.getSubject();

        if (userRepository.findByKeycloakId(keycloakId).isPresent()) {
            return true;
        }

        String rawEmail = jwt.getClaimAsString("email");
        String email = (rawEmail != null && !rawEmail.trim().isEmpty())
                ? rawEmail.trim().toLowerCase(Locale.ROOT)
                : null;
        if (email == null) {
            log.warn("Provisionamento recusado: token sub {} sem claim email", keycloakId);
            return false;
        }

        Optional<UserEntity> existingByEmail = userRepository.findByEmail(email);
        if (existingByEmail.isPresent()) {
            return vincularPorEmail(existingByEmail.get(), keycloakId, Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")));
        }

        UserEntity newUser = new UserEntity();
        newUser.setKeycloakId(keycloakId);
        newUser.setNome(montarNome(jwt));
        newUser.setEmail(email);

        return salvarComTratamentoDeConcorrencia(newUser, keycloakId, "provisionar");
    }

    private boolean vincularPorEmail(UserEntity existing, String keycloakId, boolean emailVerificado) {
        if (!emailVerificado || existing.getKeycloakId() != null) {
            log.warn("Vínculo por e-mail recusado: usuário local {} (keycloakId {}), token sub {} com email_verified={}",
                    existing.getId(), existing.getKeycloakId() != null ? "preenchido" : "nulo", keycloakId, emailVerificado);
            return false;
        }
        existing.setKeycloakId(keycloakId);
        return salvarComTratamentoDeConcorrencia(existing, keycloakId, "vincular");
    }

    private boolean salvarComTratamentoDeConcorrencia(UserEntity user, String keycloakId, String acao) {
        try {
            userRepository.save(user);
            return true;
        } catch (DataIntegrityViolationException e) {
            if (userRepository.findByKeycloakId(keycloakId).isPresent()) {
                log.debug("Usuário {} já atendido por requisição concorrente ao {}: {}",
                        keycloakId, acao, e.getMostSpecificCause().getMessage());
                return true;
            }
            log.warn("Conflito de integridade ao {} usuário {}: {}", acao, keycloakId, e.getMostSpecificCause().getMessage());
            return false;
        }
    }

    private static String montarNome(Jwt jwt) {
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String name = ((givenName != null ? givenName : "") + (familyName != null ? " " + familyName : "")).trim();

        if (name.isEmpty()) {
            String preferred = jwt.getClaimAsString("preferred_username");
            name = preferred != null ? preferred.trim() : "";
        }
        return name.isEmpty() ? "Novo Usuário" : name;
    }

    private static void escreverConflito(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.CONFLICT.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSON.writeValueAsString(
                ProblemDetails.asMap(HttpStatus.CONFLICT, MENSAGEM_CONFLITO, request.getRequestURI())));
    }
}
