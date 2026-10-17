package unievangelica.taskflow.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Resposta contendo os dados de uma Organização, suas regras de permissão e retenção de auditoria")
public record OrganizationResponseDTO(
        @Schema(description = "ID único da organização", example = "1")
        Long id,

        @Schema(description = "Nome da organização", example = "Equipe Alpha")
        String nome,

        @Schema(description = "Data e hora de criação da organização")
        LocalDateTime criadoEm,

        @Schema(description = "Indica se membros podem excluir qualquer tarefa da organização")
        Boolean membersCanDeleteTasks,

        @Schema(description = "Indica se membros podem excluir apenas tarefas criadas por eles mesmos")
        Boolean membersCanDeleteOwnTasks,

        @Schema(description = "Indica se membros podem alterar o prazo de entrega das tarefas")
        Boolean membersCanChangeDeadlines,

        @Schema(description = "Indica se membros podem reabrir tarefas já concluídas")
        Boolean membersCanReturnTasks,

        @Schema(description = "Indica se membros podem criar novas tarefas")
        Boolean membersCanCreateTasks,

        @Schema(description = "Quantidade de dias que o histórico de tarefas excluídas permanece disponível (definido pelo OWNER)", example = "7")
        Integer deletedTasksRetentionDays,

        @Schema(description = "Papel (Role) que o usuário logado possui nesta organização", example = "OWNER")
        String myRole
) {
    public OrganizationResponseDTO(
            Long id,
            String nome,
            LocalDateTime criadoEm,
            Boolean membersCanDeleteTasks,
            Boolean membersCanChangeDeadlines,
            Boolean membersCanReturnTasks,
            Boolean membersCanCreateTasks,
            String myRole
    ) {
        this(
            id,
            nome,
            criadoEm,
            membersCanDeleteTasks,
            false,
            membersCanChangeDeadlines,
            membersCanReturnTasks,
            membersCanCreateTasks,
            7,
            myRole
        );
    }
}
