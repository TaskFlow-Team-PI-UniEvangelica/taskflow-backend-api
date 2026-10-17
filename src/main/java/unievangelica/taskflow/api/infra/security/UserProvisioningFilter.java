package unievangelica.taskflow.api.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;

import java.io.IOException;

@Component
public class UserProvisioningFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public UserProvisioningFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            provisionUserIfNecessary(jwtToken.getToken());
        }
        
        filterChain.doFilter(request, response);
    }

    private void provisionUserIfNecessary(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        
        if (userRepository.findByKeycloakId(keycloakId).isEmpty()) {
            String email = jwt.getClaimAsString("email");
            if (email != null) {
                var existingByEmail = userRepository.findByEmail(email);
                if (existingByEmail.isPresent()) {
                    UserEntity existing = existingByEmail.get();
                    existing.setKeycloakId(keycloakId);
                    userRepository.save(existing);
                    return;
                }
            }

            UserEntity newUser = new UserEntity();
            newUser.setKeycloakId(keycloakId);
            
            String givenName = jwt.getClaimAsString("given_name");
            String familyName = jwt.getClaimAsString("family_name");
            String name = ((givenName != null ? givenName : "") + (familyName != null ? " " + familyName : "")).trim();
            
            if (name.isEmpty()) {
                String preferred = jwt.getClaimAsString("preferred_username");
                name = preferred != null ? preferred.trim() : "";
            }
            if (name.isEmpty()) {
                name = "Novo Usuário";
            }
            
            newUser.setNome(name);
            newUser.setEmail(email);
            
            try {
                userRepository.save(newUser);
            } catch (Exception ignored) {
                // Em caso de requisições simultâneas no primeiro login (race condition),
                // a primeira requisição já persistiu o usuário.
            }
        }
    }
}
