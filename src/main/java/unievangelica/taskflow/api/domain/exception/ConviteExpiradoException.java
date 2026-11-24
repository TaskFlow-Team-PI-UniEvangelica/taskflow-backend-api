package unievangelica.taskflow.api.domain.exception;

/**
 * Tentativa de aceitar um convite vencido. Respondida como 400 (é uma BusinessException), mas
 * não desfaz a transação: a marcação do convite como EXPIRED precisa ser gravada.
 */
public class ConviteExpiradoException extends BusinessException {

    public ConviteExpiradoException() {
        super("Este convite expirou.");
    }
}
