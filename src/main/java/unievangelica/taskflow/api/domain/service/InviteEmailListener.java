package unievangelica.taskflow.api.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import unievangelica.taskflow.api.domain.event.InviteCreatedEvent;

/**
 * Envia o e-mail de convite fora da requisição:
 * - AFTER_COMMIT: só depois que o convite foi efetivamente gravado (sem e-mail de convite inexistente);
 * - @Async: a latência do SMTP não entra na resposta HTTP nem segura a conexão com o banco.
 * Falhas de envio são registradas pelo EmailService; o convite continua visível no app.
 */
@Component
@RequiredArgsConstructor
public class InviteEmailListener {

    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInviteCreated(InviteCreatedEvent event) {
        emailService.sendInviteEmail(event.invitedEmail(), event.inviterName(), event.organizationName());
    }
}
