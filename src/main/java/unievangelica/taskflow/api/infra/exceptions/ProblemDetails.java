package unievangelica.taskflow.api.infra.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Monta o corpo de erro padrão da API: ProblemDetail (RFC 9457) com os campos de compatibilidade
 * {@code message}, {@code error} e {@code timestamp}, lidos pelo frontend.
 */
public final class ProblemDetails {

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatusCode status, String message) {
        String reason = reasonPhrase(status);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setTitle(reason);
        problem.setProperty("message", message);
        problem.setProperty("error", reason);
        problem.setProperty("timestamp", LocalDateTime.now().toString());
        return problem;
    }

    /**
     * Mesmo corpo de {@link #of}, como mapa pronto para serializar fora do Spring MVC
     * (filtros e entry points de segurança, que escrevem a resposta diretamente).
     */
    public static Map<String, Object> asMap(HttpStatusCode status, String message, String instance) {
        String reason = reasonPhrase(status);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", reason);
        body.put("status", status.value());
        body.put("detail", message);
        body.put("instance", instance);
        body.put("message", message);
        body.put("error", reason);
        body.put("timestamp", LocalDateTime.now().toString());
        return body;
    }

    public static String reasonPhrase(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return resolved != null ? resolved.getReasonPhrase() : String.valueOf(status.value());
    }
}
