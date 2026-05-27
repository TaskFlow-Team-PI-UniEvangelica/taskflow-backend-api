package unievangelica.taskflow.api.test.unit.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import unievangelica.taskflow.api.controllers.TaskController;
import unievangelica.taskflow.api.domain.persistence.entities.TaskEntity;
import unievangelica.taskflow.api.domain.service.TaskService;
import unievangelica.taskflow.api.dto.request.TaskRequestDTO;
import unievangelica.taskflow.api.dto.request.TaskStatusRequestDTO;
import unievangelica.taskflow.api.dto.response.TaskResponseDTO;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class TaskControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(taskController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Deve retornar lista de tarefas no GET /task")
    void testListarTasks() throws Exception {
        TaskResponseDTO responseDTO = new TaskResponseDTO(1L, "Titulo", "Desc", "pendente", "baixa", null, "Criador", List.of());
        when(taskService.listarTasks()).thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/task"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Titulo"));

        verify(taskService, times(1)).listarTasks();
    }

    @Test
    @DisplayName("Deve criar tarefa no POST /task")
    void testCriarTask() throws Exception {
        TaskRequestDTO requestDTO = new TaskRequestDTO("Titulo", "Desc", null, null, null, null);
        TaskResponseDTO responseDTO = new TaskResponseDTO(1L, "Titulo", "Desc", "pendente", "media", null, "Criador", List.of());

        when(taskService.criarTask(any(TaskRequestDTO.class), any())).thenReturn(responseDTO);

        mockMvc.perform(post("/task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value("Titulo"));

        verify(taskService, times(1)).criarTask(any(TaskRequestDTO.class), any());
    }

    @Test
    @DisplayName("Deve atualizar status da tarefa no PATCH /task/{id}/status")
    void testAtualizarStatusTask() throws Exception {
        TaskStatusRequestDTO statusRequest = new TaskStatusRequestDTO("concluida");

        doNothing().when(taskService).atualizarStatusTask(eq(1L), eq("concluida"));

        mockMvc.perform(patch("/task/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isNoContent());

        verify(taskService, times(1)).atualizarStatusTask(eq(1L), eq("concluida"));
    }

    @Test
    @DisplayName("Deve deletar tarefa no DELETE /task/{id}")
    void testDeletarTask() throws Exception {
        doNothing().when(taskService).deletarTask(1L);

        mockMvc.perform(delete("/task/1"))
                .andExpect(status().isNoContent());

        verify(taskService, times(1)).deletarTask(1L);
    }
}

