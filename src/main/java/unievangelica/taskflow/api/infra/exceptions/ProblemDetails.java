package unievangelica.taskflow.api.infra.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.time.LocalDateTime;

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

    public static String reasonPhrase(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return resolved != null ? resolved.getReasonPhrase() : String.valueOf(status.value());
    }
}
