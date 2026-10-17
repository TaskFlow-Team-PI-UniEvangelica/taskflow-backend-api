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
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import unievangelica.taskflow.api.controllers.TaskController;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.service.TaskService;
import unievangelica.taskflow.api.domain.service.UserService;
import unievangelica.taskflow.api.dto.request.TaskRequestDTO;
import unievangelica.taskflow.api.dto.request.TaskStatusRequestDTO;
import unievangelica.taskflow.api.dto.response.TaskHistoryResponseDTO;
import unievangelica.taskflow.api.dto.response.TaskResponseDTO;

import java.time.LocalDateTime;
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

    @Mock
    private UserService userService;

    @InjectMocks
    private TaskController taskController;

    private ObjectMapper objectMapper;
    private Jwt mockJwt;
    private UserEntity mockUser;

    @BeforeEach
    void setUp() {
        mockJwt = mock(Jwt.class);
        lenient().when(mockJwt.getSubject()).thenReturn("keycloak-id-123");

        mockUser = new UserEntity();
        mockUser.setId(1L);
        mockUser.setKeycloakId("keycloak-id-123");

        mockMvc = MockMvcBuilders.standaloneSetup(taskController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(Jwt.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return mockJwt;
                    }
                })
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Deve retornar lista de tarefas no GET /task")
    void testListarTasks() throws Exception {
        TaskResponseDTO responseDTO = new TaskResponseDTO(1L, "Titulo", "Desc", "pendente", "baixa", null, "Criador", List.of());
        when(taskService.listarTasks(any(), any())).thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/task"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].taskNumber").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Titulo"));

        verify(taskService, times(1)).listarTasks(any(), any());
    }

    @Test
    @DisplayName("Deve criar tarefa no POST /task")
    void testCriarTask() throws Exception {
        TaskRequestDTO requestDTO = new TaskRequestDTO("Titulo", "Desc", "pendente", "media", null, null, 1L);
        TaskResponseDTO responseDTO = new TaskResponseDTO(1L, "Titulo", "Desc", "pendente", "media", null, "Criador", List.of());

        when(userService.buscarPorKeycloakId("keycloak-id-123")).thenReturn(mockUser);
        when(taskService.criarTask(any(TaskRequestDTO.class), eq(mockUser))).thenReturn(responseDTO);

        mockMvc.perform(post("/task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value("Titulo"));

        verify(taskService, times(1)).criarTask(any(TaskRequestDTO.class), eq(mockUser));
    }

    @Test
    @DisplayName("Deve atualizar status da tarefa no PATCH /task/{id}/status")
    void testAtualizarStatusTask() throws Exception {
        TaskStatusRequestDTO statusRequest = new TaskStatusRequestDTO("concluida");

        when(userService.buscarPorKeycloakId("keycloak-id-123")).thenReturn(mockUser);
        doNothing().when(taskService).atualizarStatusTask(eq(1L), eq("concluida"), eq(mockUser));

        mockMvc.perform(patch("/task/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isNoContent());

        verify(taskService, times(1)).atualizarStatusTask(eq(1L), eq("concluida"), eq(mockUser));
    }

    @Test
    @DisplayName("Deve deletar tarefa no DELETE /task/{id}")
    void testDeletarTask() throws Exception {
        when(userService.buscarPorKeycloakId("keycloak-id-123")).thenReturn(mockUser);
        doNothing().when(taskService).deletarTask(eq(1L), eq(mockUser));

        mockMvc.perform(delete("/task/1"))
                .andExpect(status().isNoContent());

        verify(taskService, times(1)).deletarTask(eq(1L), eq(mockUser));
    }

    @Test
    @DisplayName("Deve buscar historico da tarefa no GET /task/{id}/history")
    void testBuscarHistoricoTask() throws Exception {
        TaskHistoryResponseDTO historyDTO = new TaskHistoryResponseDTO(
                1L, "STATUS_CHANGE", "pendente", "concluida", "Usuário Teste", LocalDateTime.now()
        );

        when(userService.buscarPorKeycloakId("keycloak-id-123")).thenReturn(mockUser);
        when(taskService.getTaskHistory(eq(1L), eq(mockUser))).thenReturn(List.of(historyDTO));

        mockMvc.perform(get("/task/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].actionType").value("STATUS_CHANGE"))
                .andExpect(jsonPath("$[0].oldValue").value("pendente"))
                .andExpect(jsonPath("$[0].newValue").value("concluida"))
                .andExpect(jsonPath("$[0].userName").value("Usuário Teste"));

        verify(taskService, times(1)).getTaskHistory(eq(1L), eq(mockUser));
    }

    @Test
    @DisplayName("Deve buscar histórico de tarefas excluídas da organização no GET /task/organization/{orgId}/deleted-history")
    void testBuscarHistoricoTasksExcluidasOrganizacao() throws Exception {
        TaskHistoryResponseDTO deletedDTO = new TaskHistoryResponseDTO(
                10L, null, 2, "Tarefa Removida", "DELETED", "Tarefa Removida", null, "Gerente", LocalDateTime.now()
        );

        when(userService.buscarPorKeycloakId("keycloak-id-123")).thenReturn(mockUser);
        when(taskService.getDeletedTasksHistory(eq(5L), eq(mockUser))).thenReturn(List.of(deletedDTO));

        mockMvc.perform(get("/task/organization/5/deleted-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskNumber").value(2))
                .andExpect(jsonPath("$[0].taskTitle").value("Tarefa Removida"))
                .andExpect(jsonPath("$[0].actionType").value("DELETED"));

        verify(taskService, times(1)).getDeletedTasksHistory(eq(5L), eq(mockUser));
    }
}
