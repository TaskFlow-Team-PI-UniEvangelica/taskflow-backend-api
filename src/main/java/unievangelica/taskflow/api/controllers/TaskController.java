package unievangelica.taskflow.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import unievangelica.taskflow.api.domain.service.UserService;
import org.springframework.web.bind.annotation.*;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.service.TaskService;
import unievangelica.taskflow.api.dto.request.TaskRequestDTO;
import unievangelica.taskflow.api.dto.request.TaskStatusRequestDTO;
import unievangelica.taskflow.api.dto.response.TaskResponseDTO;
import unievangelica.taskflow.api.dto.response.TaskHistoryResponseDTO;

import java.util.List;

@RestController
@RequestMapping("/task")
@Tag(name = "Tarefas", description = "Endpoints para gerenciamento de tasks com suporte a Multi-Tenant RBAC")
public class TaskController {
    
    private final UserService userService;
    private final TaskService taskService;

    public TaskController(UserService userService, TaskService taskService) {
        this.userService = userService;
        this.taskService = taskService;
    }

    @Operation(summary = "Listar todas as tarefas", description = "Retorna uma lista de tarefas, opcionalmente filtrada por organização.")
    @ApiResponse(responseCode = "200", description = "Tarefas listadas com sucesso")
    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> verTodasTasks(
            @RequestParam(required = false) Long organizationId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt){
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        List<TaskResponseDTO> tasks = taskService.listarTasks(organizationId, userLogado);
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "Criar nova tarefa", description = "Cria uma tarefa para uma organização específica, requer permissão CREATE_TASK na organização.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tarefa criada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Organização não informada no body (organizationId)"),
        @ApiResponse(responseCode = "403", description = "Usuário não possui permissão nesta organização")
    })
    @PostMapping
    public ResponseEntity<TaskResponseDTO> criarTask(
            @RequestBody TaskRequestDTO data, 
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt){
       UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
       TaskResponseDTO novaTask = taskService.criarTask(data, userLogado);
       return ResponseEntity.status(HttpStatus.CREATED).body(novaTask);
    }

    @Operation(summary = "Atualizar tarefa", description = "Atualiza os dados de uma tarefa existente. Pode exigir CHANGE_DEADLINE dependendo das alterações enviadas.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tarefa atualizada com sucesso"),
        @ApiResponse(responseCode = "403", description = "Usuário não possui permissão para editar na organização da tarefa"),
        @ApiResponse(responseCode = "404", description = "Tarefa não encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> updateTask(
            @Parameter(description = "ID da tarefa") @PathVariable Long id, 
            @RequestBody TaskRequestDTO data, 
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt){
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        TaskResponseDTO taskAtualizada = taskService.atualizarTask(id, data, userLogado);
        return ResponseEntity.ok(taskAtualizada);
    }

    @Operation(summary = "Atualizar Status de uma Tarefa", description = "Move a tarefa de status. Caso movida de Concluída para trás, requer RETURN_TASK.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Status atualizado com sucesso"),
        @ApiResponse(responseCode = "403", description = "Usuário não possui permissão para movimentar tasks na organização atual"),
        @ApiResponse(responseCode = "404", description = "Tarefa não encontrada")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatusTask(
            @Parameter(description = "ID da tarefa") @PathVariable Long id, 
            @RequestBody TaskStatusRequestDTO data, 
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt){
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        taskService.atualizarStatusTask(id, data.status(), userLogado);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Excluir Tarefa", description = "Exclui uma tarefa específica, requer permissão DELETE_TASK na organização à qual a tarefa pertence.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tarefa excluída com sucesso"),
        @ApiResponse(responseCode = "403", description = "Usuário sem permissão de exclusão (ver regras do OWNER/MANAGER)"),
        @ApiResponse(responseCode = "404", description = "Tarefa não encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @Parameter(description = "ID da tarefa") @PathVariable Long id, 
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt){
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        taskService.deletarTask(id, userLogado);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Obter histórico", description = "Obtém a linha do tempo de alterações de uma task")
    public ResponseEntity<List<TaskHistoryResponseDTO>> getTaskHistory(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        List<TaskHistoryResponseDTO> history = taskService.getTaskHistory(id, userLogado);
        return ResponseEntity.ok(history);
    }
}
