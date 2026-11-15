package unievangelica.taskflow.api.domain.event;

/**
 * Publicado quando um convite de organização é criado. O e-mail correspondente é enviado
 * pelo InviteEmailListener somente após o commit da transação que gravou o convite.
 */
public record InviteCreatedEvent(String invitedEmail, String inviterName, String organizationName) {
}
