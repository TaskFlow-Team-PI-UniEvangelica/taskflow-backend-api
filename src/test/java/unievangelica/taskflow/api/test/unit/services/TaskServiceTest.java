package unievangelica.taskflow.api.test.unit.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unievangelica.taskflow.api.domain.persistence.entities.TaskEntity;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.TaskRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.domain.service.TaskService;
import unievangelica.taskflow.api.dto.request.TaskRequestDTO;
import unievangelica.taskflow.api.dto.response.TaskResponseDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    // mockando os métodos dos repositories usados no service
    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    // injetando o mock do service
    @InjectMocks
    private TaskService taskService;

    // anotação para dizer que é um teste
    @Test
    @DisplayName("Deve listar tarefas corretamente e converter para o DTO")
    void testaListarTasksEConverterParaDTO() {
        // ARRANGE

        //mock de usuário necessário para atribuir um usuário criador a task
        UserEntity criador = new UserEntity();
        criador.setNome("UnitTestServiceUser");

        // mock para adicionar uma nova task
        TaskEntity taskMock = new TaskEntity();
        taskMock.setId(1L);
        taskMock.setTitulo("Unit Test Service Titulo");
        taskMock.setStatus(TaskEntity.Status.pendente);
        taskMock.setPrioridade(TaskEntity.Prioridade.media);
        taskMock.setCriador(criador); // define criado mockado como criador da task
        taskMock.setResponsaveis(List.of()); // lista limpa sem responsáveis

        when(taskRepository.listAllTasks()).thenReturn(List.of(taskMock));

        // ACT
        List<TaskResponseDTO> resultado = taskService.listarTasks();

        // ASSERT
        assertThat(resultado).isNotNull().hasSize(1);

        TaskResponseDTO taskRetornada = resultado.get(0);

        assertThat(taskRetornada.titulo()).isEqualTo("Unit Test Service Titulo");
        assertThat(taskRetornada.descricao()).isNull();
        assertThat(taskRetornada.status()).isEqualTo("pendente");
        assertThat(taskRetornada.prioridade()).isEqualTo("media");
        assertThat(taskRetornada.prazo()).isNull();
        assertThat(taskRetornada.nomeCriador()).isEqualTo("UnitTestServiceUser");

        // Verifica se o repositório foi chamado
        verify(taskRepository, times(1)).listAllTasks();
    }

    @Test
    @DisplayName("Deve criar tarefa aplicando status e prioridade padrão")
    void testarCriarTaskSemDefinirAtributos() {
        // ARRANGE

        // mock usuário criador da tarefa
        UserEntity criador = new UserEntity();
        criador.setNome("UnitTestServiceUser");

        // mock de request do DTO de criação de usuário com apenas as informações obrigatórias e descrição
        TaskRequestDTO requestDTO = new TaskRequestDTO(
                "Unit Test Service Titulo",
                "Unit Test Service Descrição", // descricao
                null, // status
                null, // prioridade
                null, // prazo
                null  // idsResponsaveis
        );

        // retornando a entidade mockada para ser salva pelo .save do método
        TaskEntity taskMock = new TaskEntity();
        taskMock.setId(10L);
        taskMock.setTitulo("Unit Test Service Titulo");
        taskMock.setDescricao("Unit Test Service Descrição");
        taskMock.setStatus(TaskEntity.Status.pendente);
        taskMock.setPrioridade(TaskEntity.Prioridade.media);
        taskMock.setCriador(criador);
        taskMock.setResponsaveis(List.of());

        // retorna o mock após um entidade for salva
        when(taskRepository.save(any(TaskEntity.class))).thenReturn(taskMock);

        // ACT
        TaskResponseDTO resultado = taskService.criarTask(requestDTO, criador);

        // ASSERT
        assertThat(resultado.titulo()).isEqualTo("Unit Test Service Titulo");
        assertThat(resultado.status()).isEqualTo("pendente");
        assertThat(resultado.nomeCriador()).isEqualTo("UnitTestServiceUser");

        // captura a entidade que o service montou
        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);

        verify(taskRepository, times(1)).save(captor.capture());
        TaskEntity entidadeCapturada = captor.getValue();

        // verifica as regras de negócio
        assertThat(entidadeCapturada.getTitulo()).isEqualTo("Unit Test Service Titulo");
        assertThat(entidadeCapturada.getCriador()).isEqualTo(criador);

        // verifica o if do null para retornar os valores padrão
        assertThat(entidadeCapturada.getPrioridade()).isEqualTo(TaskEntity.Prioridade.media);
        assertThat(entidadeCapturada.getStatus()).isEqualTo(TaskEntity.Status.pendente);
        assertThat(entidadeCapturada.getPrazo()).isNull();

        // garante que user repository não seja chamado já que não foi passado os responsáveis
        verify(userRepository, never()).userFindAllByIds(anyList());
    }

    @Test
    @DisplayName("Deve buscar e vincular responsáveis quando a lista de IDs for informada")
    void testarCriarTaskEBuscarResponsaveis() {
        // ARRANGE

        // mocks de usuários
        UserEntity criador = new UserEntity();
        criador.setNome("UnitTestServiceUser");

        UserEntity dev1 = new UserEntity(); dev1.setNome("UnitTestServiceUser1");
        UserEntity dev2 = new UserEntity(); dev2.setNome("UnitTestServiceUser2");

        List<Long> idsResponsaveis = List.of(2L, 3L);
        TaskRequestDTO requestDTO = new TaskRequestDTO("Unit Test Service Titulo", "Unit Test Service Descrição", null, null, null, idsResponsaveis);

        // mock lê o retorno do repository
        when(userRepository.userFindAllByIds(idsResponsaveis)).thenReturn(List.of(dev1, dev2));

        // mock para garantir o retorno correto do DTO após salvar
        TaskEntity taskMock = new TaskEntity();
        taskMock.setId(10L);
        taskMock.setTitulo("Unit Test Service Titulo");
        taskMock.setStatus(TaskEntity.Status.pendente);
        taskMock.setPrioridade(TaskEntity.Prioridade.media);
        taskMock.setCriador(criador);
        taskMock.setResponsaveis(List.of(dev1, dev2)); // já devolve com a lista preenchida

        when(taskRepository.save(any(TaskEntity.class))).thenReturn(taskMock);

        // ACT
        TaskResponseDTO resultado = taskService.criarTask(requestDTO, criador);

        // ASSERT
        verify(userRepository, times(1)).userFindAllByIds(idsResponsaveis);

        // captura a entidade para verificar a vinculação
        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(taskRepository).save(captor.capture());

        // verifica a vinculação dos responsáveis na entidade antes de salvar
        assertThat(captor.getValue().getResponsaveis())
                .isNotNull()
                .hasSize(2)
                .extracting(UserEntity::getNome)
                .containsExactlyInAnyOrder("UnitTestServiceUser1", "UnitTestServiceUser2");

        // verifica se os nomes vieram do dto
        assertThat(resultado.nomesResponsaveis())
                .isNotEmpty()
                .contains("UnitTestServiceUser1", "UnitTestServiceUser2");
    }
}