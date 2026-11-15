package unievangelica.taskflow.api.domain.exception;

/**
 * A operação conflita com o estado atual do recurso (por exemplo, excluir a conta de quem ainda é
 * proprietário de uma organização). Respondida como 409.
 * A mensagem é exibida ao usuário, portanto deve ser amigável e orientar como resolver o conflito.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
