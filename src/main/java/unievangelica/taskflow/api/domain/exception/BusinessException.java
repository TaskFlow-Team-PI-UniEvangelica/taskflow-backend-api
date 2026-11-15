package unievangelica.taskflow.api.domain.exception;

/**
 * Violação de regra de negócio ou dado de entrada inválido. Respondida como 400.
 * A mensagem é exibida ao usuário, portanto deve ser amigável e não conter detalhes internos.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
