package unievangelica.taskflow.api.test.unit.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import unievangelica.taskflow.api.domain.persistence.entities.TaskEntity;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TaskEntityTest {

    @Test
    @DisplayName("Deve instanciar uma TaskEntity com sucesso e atribuir os valores")
    void deveInstanciarTaskEntityCorretamente() {
        UserEntity criador = new UserEntity();
        criador.setId(1L);
        criador.setNome("Test User");

        UserEntity resp1 = new UserEntity();
        resp1.setId(2L);

        TaskEntity task = new TaskEntity();
        task.setId(10L);
        task.setTitulo("Título da Tarefa");
        task.setDescricao("Descrição da Tarefa");
        task.setStatus(TaskEntity.Status.pendente);
        task.setPrioridade(TaskEntity.Prioridade.alta);
        task.setPrazo(LocalDate.of(2025, 12, 31));
        task.setCriador(criador);
        task.setResponsaveis(List.of(resp1));

        assertThat(task.getId()).isEqualTo(10L);
        assertThat(task.getTitulo()).isEqualTo("Título da Tarefa");
        assertThat(task.getDescricao()).isEqualTo("Descrição da Tarefa");
        assertThat(task.getStatus()).isEqualTo(TaskEntity.Status.pendente);
        assertThat(task.getPrioridade()).isEqualTo(TaskEntity.Prioridade.alta);
        assertThat(task.getPrazo()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(task.getCriador().getNome()).isEqualTo("Test User");
        assertThat(task.getResponsaveis()).hasSize(1);
    }
}
