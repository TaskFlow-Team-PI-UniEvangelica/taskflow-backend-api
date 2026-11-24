package unievangelica.taskflow.api.domain.exception;

/**
 * Recurso inexistente (organização, tarefa, usuário, convite, membro). Respondida como 404.
 * A mensagem é exibida ao usuário, portanto deve ser amigável e não conter detalhes internos.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
