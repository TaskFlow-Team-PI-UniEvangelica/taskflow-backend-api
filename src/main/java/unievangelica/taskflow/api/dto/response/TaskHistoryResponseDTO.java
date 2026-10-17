package unievangelica.taskflow.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Registro de auditoria e histórico de alterações ou exclusão de uma tarefa")
public record TaskHistoryResponseDTO(
        @Schema(description = "ID do registro de histórico", example = "100")
        Long id,

        @Schema(description = "ID da tarefa (pode ser null caso a tarefa já tenha sido excluída)", example = "42")
        Long taskId,

        @Schema(description = "Número sequencial da tarefa dentro da organização (preservado mesmo após exclusão)", example = "1")
        Integer taskNumber,

        @Schema(description = "Título da tarefa no momento do registro (preservado mesmo após exclusão)", example = "Implementar login")
        String taskTitle,

        @Schema(description = "Tipo da ação registrada (CREATED, MOVED, RETURNED, EDITED_TITLE, EDITED_DESCRIPTION, CHANGED_PRIORITY, CHANGED_DEADLINE, CHANGED_ASSIGNEES, DELETED)", example = "DELETED")
        String actionType,

        @Schema(description = "Valor anterior do campo alterado")
        String oldValue,

        @Schema(description = "Novo valor do campo alterado")
        String newValue,

        @Schema(description = "Nome do usuário que executou a ação", example = "Maria Souza")
        String userName,

        @Schema(description = "Data e hora exata da ação")
        LocalDateTime timestamp
) {
    public TaskHistoryResponseDTO(
            Long id,
            Long taskId,
            String taskTitle,
            String actionType,
            String oldValue,
            String newValue,
            String userName,
            LocalDateTime timestamp
    ) {
        this(id, taskId, 1, taskTitle, actionType, oldValue, newValue, userName, timestamp);
    }

    public TaskHistoryResponseDTO(
            Long id,
            String actionType,
            String oldValue,
            String newValue,
            String userName,
            LocalDateTime timestamp
    ) {
        this(id, null, null, null, actionType, oldValue, newValue, userName, timestamp);
    }
}
