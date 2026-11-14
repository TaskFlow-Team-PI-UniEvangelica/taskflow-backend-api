package unievangelica.taskflow.api.test.unit.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.TaskHistoryRepository;
import unievangelica.taskflow.api.domain.service.DeletedTaskHistoryPurgeJob;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeletedTaskHistoryPurgeJobTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private TaskHistoryRepository taskHistoryRepository;

    @InjectMocks
    private DeletedTaskHistoryPurgeJob job;

    private OrganizationEntity org(Long id, Integer retentionDays) {
        OrganizationEntity org = new OrganizationEntity();
        org.setId(id);
        org.setDeletedTasksRetentionDays(retentionDays);
        return org;
    }

    @Test
    @DisplayName("Deve expurgar apenas organizações com retenção, usando o corte de cada uma")
    void deveExpurgarCadaOrganizacaoComSeuCorte() {
        when(organizationRepository.findByDeletedTasksRetentionDaysGreaterThan(0))
                .thenReturn(List.of(org(1L, 7), org(2L, 30)));
        when(taskHistoryRepository.deleteExpiredDeletedHistory(anyLong(), any(LocalDateTime.class))).thenReturn(3, 0);

        job.purgeExpiredDeletedHistory();

        ArgumentCaptor<LocalDateTime> corte1 = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> corte2 = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(taskHistoryRepository).deleteExpiredDeletedHistory(eq(1L), corte1.capture());
        verify(taskHistoryRepository).deleteExpiredDeletedHistory(eq(2L), corte2.capture());
        LocalDateTime agora = LocalDateTime.now();
        assertThat(corte1.getValue()).isBetween(agora.minusDays(7).minusMinutes(1), agora.minusDays(7).plusMinutes(1));
        assertThat(corte2.getValue()).isBetween(agora.minusDays(30).minusMinutes(1), agora.minusDays(30).plusMinutes(1));
        verifyNoMoreInteractions(taskHistoryRepository);
    }

    @Test
    @DisplayName("Falha em uma organização não deve interromper o expurgo das demais")
    void falhaEmUmaOrganizacaoNaoInterrompeAsDemais() {
        when(organizationRepository.findByDeletedTasksRetentionDaysGreaterThan(0))
                .thenReturn(List.of(org(1L, 7), org(2L, 7)));
        when(taskHistoryRepository.deleteExpiredDeletedHistory(eq(1L), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("falha simulada"));
        when(taskHistoryRepository.deleteExpiredDeletedHistory(eq(2L), any(LocalDateTime.class))).thenReturn(1);

        job.purgeExpiredDeletedHistory();

        verify(taskHistoryRepository).deleteExpiredDeletedHistory(eq(2L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Sem organizações com retenção, não deve expurgar nada")
    void semOrganizacoesComRetencaoNaoExpurga() {
        when(organizationRepository.findByDeletedTasksRetentionDaysGreaterThan(0)).thenReturn(List.of());

        job.purgeExpiredDeletedHistory();

        verifyNoInteractions(taskHistoryRepository);
    }

    @Test
    @DisplayName("Job deve ser agendado pela propriedade configurável, no fuso de Brasília")
    void jobDeveSerAgendadoPorPropriedade() throws NoSuchMethodException {
        Scheduled scheduled = DeletedTaskHistoryPurgeJob.class
                .getMethod("purgeExpiredDeletedHistory")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("${app.history.purge-cron:0 0 3 * * *}");
        assertThat(scheduled.zone()).isEqualTo("America/Sao_Paulo");
    }
}
