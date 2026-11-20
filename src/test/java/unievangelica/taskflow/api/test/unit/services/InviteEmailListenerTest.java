package unievangelica.taskflow.api.test.unit.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import unievangelica.taskflow.api.domain.event.InviteCreatedEvent;
import unievangelica.taskflow.api.domain.service.EmailService;
import unievangelica.taskflow.api.domain.service.InviteEmailListener;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class InviteEmailListenerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private InviteEmailListener listener;

    @Test
    @DisplayName("Deve repassar os dados do convite ao EmailService")
    void deveEnviarEmailComDadosDoEvento() {
        listener.onInviteCreated(new InviteCreatedEvent("novo@taskflow.com", "Owner User", "TaskFlow Org"));

        verify(emailService).sendInviteEmail("novo@taskflow.com", "Owner User", "TaskFlow Org");
    }

    @Test
    @DisplayName("Envio deve ocorrer somente após o commit e de forma assíncrona")
    void deveSerAfterCommitEAssincrono() throws NoSuchMethodException {
        Method metodo = InviteEmailListener.class.getMethod("onInviteCreated", InviteCreatedEvent.class);

        TransactionalEventListener listenerConfig = metodo.getAnnotation(TransactionalEventListener.class);
        assertThat(listenerConfig).isNotNull();
        assertThat(listenerConfig.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(listenerConfig.fallbackExecution()).isFalse();
        assertThat(metodo.getAnnotation(Async.class)).isNotNull();
    }
}
