package unievangelica.taskflow.api.test.unit.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationEntity;
import unievangelica.taskflow.api.domain.persistence.entities.TaskEntity;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.TaskHistoryRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.TaskRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.domain.service.OrganizationService;
import unievangelica.taskflow.api.domain.service.TaskService;
import unievangelica.taskflow.api.dto.request.TaskRequestDTO;
import unievangelica.taskflow.api.dto.response.TaskResponseDTO;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private TaskHistoryRepository taskHistoryRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    @DisplayName("Deve listar tarefas corretamente e converter para o DTO")
    void testaListarTasksEConverterParaDTO() {
        UserEntity criador = new UserEntity();
        criador.setNome("UnitTestServiceUser");

        TaskEntity taskMock = new TaskEntity();
        taskMock.setId(1L);
        taskMock.setTitulo("Unit Test Service Titulo");
        taskMock.setStatus(TaskEntity.Status.pendente);
        taskMock.setPrioridade(TaskEntity.Prioridade.media);
        taskMock.setCriador(criador);
        taskMock.setResponsaveis(List.of());

        when(taskRepository.listAllTasks()).thenReturn(List.of(taskMock));

        List<TaskResponseDTO> resultado = taskService.listarTasks(null, new unievangelica.taskflow.api.domain.persistence.entities.UserEntity());

        assertThat(resultado).isNotNull().hasSize(1);
        TaskResponseDTO taskRetornada = resultado.get(0);
        assertThat(taskRetornada.titulo()).isEqualTo("Unit Test Service Titulo");
        assertThat(taskRetornada.status()).isEqualTo("pendente");
        assertThat(taskRetornada.prioridade()).isEqualTo("media");
        assertThat(taskRetornada.nomeCriador()).isEqualTo("UnitTestServiceUser");

        verify(taskRepository, times(1)).listAllTasks();
    }

    @Test
    @DisplayName("Deve criar tarefa aplicando status e prioridade padrão, validando RBAC")
    void testarCriarTaskSemDefinirAtributos() {
        UserEntity criador = new UserEntity();
        criador.setNome("UnitTestServiceUser");
        
        OrganizationEntity orgMock = new OrganizationEntity();
        orgMock.setId(100L);

        TaskRequestDTO requestDTO = new TaskRequestDTO(
                "Unit Test Service Titulo",
                "Unit Test Service Descrição",
                null,
                null,
                null,
                null,
                100L
        );

        when(organizationService.hasPermissionTo(criador, 100L, "CREATE_TASK")).thenReturn(true);
        when(organizationRepository.findById(100L)).thenReturn(Optional.of(orgMock));

        TaskEntity taskMock = new TaskEntity();
        taskMock.setId(10L);
        taskMock.setTitulo("Unit Test Service Titulo");
        taskMock.setDescricao("Unit Test Service Descrição");
        taskMock.setStatus(TaskEntity.Status.pendente);
        taskMock.setPrioridade(TaskEntity.Prioridade.media);
        taskMock.setCriador(criador);
        taskMock.setOrganization(orgMock);
        taskMock.setResponsaveis(List.of());

        when(taskRepository.save(any(TaskEntity.class))).thenReturn(taskMock);

        TaskResponseDTO resultado = taskService.criarTask(requestDTO, criador);

        assertThat(resultado.titulo()).isEqualTo("Unit Test Service Titulo");
        assertThat(resultado.status()).isEqualTo("pendente");

        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(taskRepository, times(1)).save(captor.capture());
        TaskEntity entidadeCapturada = captor.getValue();

        assertThat(entidadeCapturada.getTitulo()).isEqualTo("Unit Test Service Titulo");
        assertThat(entidadeCapturada.getCriador()).isEqualTo(criador);
        assertThat(entidadeCapturada.getOrganization()).isEqualTo(orgMock);
        assertThat(entidadeCapturada.getPrioridade()).isEqualTo(TaskEntity.Prioridade.media);
        assertThat(entidadeCapturada.getStatus()).isEqualTo(TaskEntity.Status.pendente);

        verify(userRepository, never()).userFindAllByIds(anyList());
        verify(taskHistoryRepository, times(1)).save(any());
    }
}