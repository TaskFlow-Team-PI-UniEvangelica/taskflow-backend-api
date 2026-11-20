package unievangelica.taskflow.api.infra.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import unievangelica.taskflow.api.domain.exception.BusinessException;
import unievangelica.taskflow.api.domain.exception.ConflictException;
import unievangelica.taskflow.api.domain.exception.ResourceNotFoundException;

/**
 * Converte exceções em respostas de erro no formato ProblemDetail (RFC 9457, application/problem+json).
 * Além dos campos padrão (type, title, status, detail, instance), cada resposta mantém os campos
 * {@code message}, {@code error} e {@code timestamp} usados pelo frontend.
 *
 * Mensagens técnicas (nomes de classes, enums, parsers, stack traces) nunca chegam ao cliente:
 * erros inesperados respondem 500 com mensagem genérica e o detalhe vai apenas para o log.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String MENSAGEM_ERRO_INTERNO = "Erro interno. Tente novamente mais tarde.";

    @Value("${spring.servlet.multipart.max-file-size:2MB}")
    private String maxUploadFileSize = "2MB";

    // --- Exceções de domínio ---

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ProblemDetail> handleSecurityException(SecurityException ex) {
        return problem(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Acesso negado.");
    }

    // --- Entrada inválida ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Dados de entrada inválidos.");
        return problem(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException ex) {
        log.debug("Corpo da requisição ilegível: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + ex.getName() + "'.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return problem(HttpStatus.CONTENT_TOO_LARGE,
                "Arquivo muito grande. O tamanho máximo permitido é de " + maxUploadFileSize + ".");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ProblemDetail> handleMultipart(MultipartException ex) {
        log.debug("Requisição multipart inválida: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Requisição multipart inválida.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        // Lançada por bibliotecas/JDK: a mensagem é técnica e não é repassada ao cliente
        log.warn("Argumento inválido em {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Dados inválidos.");
    }

    // --- Persistência ---

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade de dados: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Conflito de integridade de dados.");
    }

    // --- Demais erros ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Exceções do Spring MVC que já carregam um status HTTP (rota inexistente, método não suportado,
        // parâmetro ausente, tipo de conteúdo não suportado etc.)
        if (ex instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            HttpStatusCode status = errorResponse.getStatusCode();
            log.debug("Requisição recusada com {} em {} {}: {}", status.value(), request.getMethod(),
                    request.getRequestURI(), ex.getMessage());
            return ResponseEntity.status(status)
                    .headers(errorResponse.getHeaders())
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(ProblemDetails.of(status, mensagemPara(status)));
        }
        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, MENSAGEM_ERRO_INTERNO);
    }

    private static String mensagemPara(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "Recurso não encontrado.";
            case 405 -> "Método HTTP não suportado para este recurso.";
            case 406 -> "Formato de resposta não suportado.";
            case 415 -> "Tipo de conteúdo não suportado.";
            default -> "Requisição inválida.";
        };
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatusCode status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetails.of(status, message));
    }
}
