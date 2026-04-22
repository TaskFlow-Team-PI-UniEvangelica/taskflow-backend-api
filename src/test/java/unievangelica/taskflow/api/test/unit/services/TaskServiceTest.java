package unievangelica.taskflow.api.test.unit.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unievangelica.taskflow.api.domain.persistence.entities.TaskEntity;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.TaskRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.domain.service.TaskService;
import unievangelica.taskflow.api.dto.response.TaskResponseDTO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    // Mockando os métodos dos repositories usados no service
    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    // injetando o mock do service
    @InjectMocks
    private TaskService taskService;

    // anotação para dizer que é um teste
    @Test
    void testaListarTasksEConverterParaDTO() {
        // arrange

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

        // act
        List<TaskResponseDTO> resultado = taskService.listarTasks();

        // assert
        assertEquals(1, resultado.size());
        assertEquals("Unit Test Service Titulo", resultado.get(0).titulo());
        assertNull(resultado.get(0).descricao());
        assertEquals("pendente", resultado.get(0).status());
        assertEquals("media", resultado.get(0).prioridade());
        assertNull(resultado.get(0).prazo());
        assertEquals("UnitTestServiceUser", resultado.get(0).nomeCriador());

        // Verifica se o repositório foi chamado
        verify(taskRepository, times(1)).listAllTasks();
    }

    @Test
    void testarCriarTask() {

    }

}