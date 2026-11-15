package unievangelica.taskflow.api.infra.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resposta 401 da API: JSON serializado (sem concatenação de strings), no mesmo formato do
 * GlobalExceptionHandler, com mensagem fixa. O motivo técnico da recusa do token fica apenas no log (DEBUG),
 * para não expor detalhes da validação ao cliente. Inclui o cabeçalho WWW-Authenticate (RFC 6750).
 */
@Slf4j
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String MENSAGEM = "Token de acesso ausente, inválido ou expirado.";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        log.debug("Requisição não autenticada em {} {}: {}", request.getMethod(), request.getRequestURI(),
                authException != null ? authException.getMessage() : "sem detalhes");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
        body.put("message", MENSAGEM);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSON.writeValueAsString(body));
    }
}
