package unievangelica.taskflow.api.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.TaskHistoryRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Expurgo automático do histórico de tarefas excluídas (RNE-006): remove os eventos DELETED
 * mais antigos que a retenção configurada por cada organização (deletedTasksRetentionDays).
 * Organizações com retenção nula (permanente) não são afetadas.
 *
 * Horário configurável por "app.history.purge-cron" (padrão: todos os dias às 03:00, horário de Brasília).
 * A operação é idempotente: execuções repetidas ou concorrentes produzem o mesmo resultado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeletedTaskHistoryPurgeJob {

    private final OrganizationRepository organizationRepository;
    private final TaskHistoryRepository taskHistoryRepository;

    @Scheduled(cron = "${app.history.purge-cron:0 0 3 * * *}", zone = "America/Sao_Paulo")
    public void purgeExpiredDeletedHistory() {
        List<OrganizationEntity> organizations = organizationRepository.findByDeletedTasksRetentionDaysGreaterThan(0);
        LocalDateTime now = LocalDateTime.now();
        int totalRemovidos = 0;
        int falhas = 0;

        for (OrganizationEntity organization : organizations) {
            try {
                LocalDateTime cutoff = now.minusDays(organization.getDeletedTasksRetentionDays());
                totalRemovidos += taskHistoryRepository.deleteExpiredDeletedHistory(organization.getId(), cutoff);
            } catch (RuntimeException e) {
                // A falha em uma organização não interrompe o expurgo das demais.
                falhas++;
                log.error("Falha ao expurgar o histórico de excluídas da organização {}: {}", organization.getId(), e.getMessage(), e);
            }
        }

        log.info("Expurgo do histórico de excluídas concluído: {} registro(s) removido(s) em {} organização(ões) com retenção; {} falha(s).",
                totalRemovidos, organizations.size(), falhas);
    }
}
