package unievangelica.taskflow.api.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;

import java.io.IOException;

@Component // anotação para injeção de dependências
public class SecurityFilter extends OncePerRequestFilter { // a cada requisição o filtro é chamado uma vez
    @Autowired
    TokenService tokenService;
    @Autowired
    UserRepository userRepository;

    // método que faz toda a lógica de montagem do token
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request); // chama recoverToken
        if (token != null) { // verifica se token é null
            var email = tokenService.validarToken(token); // valida o token usando o service
            UserDetails user = userRepository.userFindByEmail(email); // busca no db o email do usuário linkado ao token

            var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()); // cria um objeto de autenticação vinculado ao email e ao cargo
            SecurityContextHolder.getContext().setAuthentication(authentication); // consolida o usuário autenticado para a requisição
        }
        filterChain.doFilter(request, response); // repassa o filtro
    }

    // método auxiliar usado para recuperar o token
    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}
