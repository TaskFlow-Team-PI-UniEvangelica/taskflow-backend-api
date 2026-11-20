package unievangelica.taskflow.api.infra.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita as tarefas agendadas (@Scheduled) da aplicação.
 * Mantida separada da classe principal para não acoplar o agendamento a contextos de teste.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
