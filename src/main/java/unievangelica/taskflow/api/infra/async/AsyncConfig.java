package unievangelica.taskflow.api.infra.async;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita a execução assíncrona (@Async), usada no envio de e-mails de convite.
 * Usa o executor padrão configurado pelo Spring Boot (applicationTaskExecutor, ajustável por spring.task.execution.*).
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
