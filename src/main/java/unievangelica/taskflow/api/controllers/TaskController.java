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
@Tag(name = "Tarefas", description = "Endpoints para gerenciamento de tasks com numeração sequencial por organização, RBAC Multi-Tenant e auditoria de exclusão")
public class TaskController {
    
    private final UserService userService;
    private final TaskService taskService;

    public TaskController(UserService userService, TaskService taskService) {
        this.userService = userService;
        this.taskService = taskService;
    }

    @Operation(summary = "Listar todas as tarefas", description = "Retorna uma lista de tarefas da organização informada (com taskNumber sequencial por organização).")
    @ApiResponse(responseCode = "200", description = "Tarefas listadas com sucesso")
    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> verTodasTasks(
            @RequestParam(required = false) Long organizationId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt){
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        List<TaskResponseDTO> tasks = taskService.listarTasks(organizationId, userLogado);
        return ResponseEntity.ok(tasks);
    }

    @Operation(
        summary = "Criar nova tarefa",
        description = "Cria uma tarefa para uma organização específica (requer permissão CREATE_TASK) e atribui automaticamente o próximo número sequencial (taskNumber) da organização."
    )
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

    @Operation(summary = "Atualizar tarefa", description = "Atualiza os dados de uma tarefa existente mantendo seu taskNumber original. Pode exigir CHANGE_DEADLINE ou RETURN_TASK dependendo das alterações enviadas.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tarefa atualizada com sucesso"),
        @ApiResponse(responseCode = "403", description = "Usuário não possui permissão para editar prazo ou reabrir tarefa na organização"),
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

    @Operation(
        summary = "Excluir Tarefa",
        description = "Exclui uma tarefa específica e registra o evento DELETED no histórico da organização. " +
                      "Cargos OWNER, SUB_OWNER e MANAGER podem excluir qualquer tarefa. " +
                      "O cargo MEMBER pode excluir qualquer tarefa se membersCanDeleteTasks=true, ou apenas tarefas criadas por ele mesmo se membersCanCreateTasks=true e membersCanDeleteOwnTasks=true."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tarefa excluída com sucesso"),
        @ApiResponse(responseCode = "403", description = "Usuário sem permissão de exclusão para esta tarefa"),
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
    @Operation(summary = "Obter histórico da tarefa", description = "Obtém a linha do tempo de alterações de uma task específica")
    public ResponseEntity<List<TaskHistoryResponseDTO>> getTaskHistory(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        List<TaskHistoryResponseDTO> history = taskService.getTaskHistory(id, userLogado);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/organization/{organizationId}/history")
    @Operation(summary = "Obter histórico da organização", description = "Obtém a linha do tempo completa de auditoria das tasks de uma organização (incluindo tarefas excluídas)")
    public ResponseEntity<List<TaskHistoryResponseDTO>> getOrganizationHistory(
            @PathVariable Long organizationId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        List<TaskHistoryResponseDTO> history = taskService.getOrganizationHistory(organizationId, userLogado);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/organization/{organizationId}/deleted-history")
    @Operation(
        summary = "Obter histórico de tarefas excluídas da organização",
        description = "Lista todas as tarefas excluídas da organização dentro do período de retenção definido pelo OWNER (deletedTasksRetentionDays). " +
                      "Endpoint restrito aos cargos acima de Membro (OWNER, SUB_OWNER e MANAGER)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Histórico de tarefas excluídas retornado com sucesso"),
        @ApiResponse(responseCode = "403", description = "Acesso negado (usuário é apenas MEMBER ou não pertence à organização)"),
        @ApiResponse(responseCode = "404", description = "Organização não encontrada")
    })
    public ResponseEntity<List<TaskHistoryResponseDTO>> getDeletedTasksHistory(
            @Parameter(description = "ID da organização") @PathVariable Long organizationId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        List<TaskHistoryResponseDTO> deletedHistory = taskService.getDeletedTasksHistory(organizationId, userLogado);
        return ResponseEntity.ok(deletedHistory);
    }
}
