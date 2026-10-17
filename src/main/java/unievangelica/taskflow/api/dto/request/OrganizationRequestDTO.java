package unievangelica.taskflow.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Objeto de requisição para criar ou atualizar uma Organização e suas regras de governança")
public record OrganizationRequestDTO(
        @Schema(description = "Nome da organização", example = "Equipe Alpha")
        String nome,

        @Schema(description = "Permissão: membros podem deletar todas as tasks da organização? (Mutuamente exclusivo com membersCanDeleteOwnTasks)", example = "false")
        Boolean membersCanDeleteTasks,

        @Schema(description = "Permissão: membros podem deletar apenas suas próprias tasks? (Exige membersCanCreateTasks=true e membersCanDeleteTasks=false)", example = "false")
        Boolean membersCanDeleteOwnTasks,

        @Schema(description = "Permissão: membros podem alterar datas limite?", example = "false")
        Boolean membersCanChangeDeadlines,

        @Schema(description = "Permissão: membros podem retornar tasks para status anteriores?", example = "false")
        Boolean membersCanReturnTasks,

        @Schema(description = "Permissão: membros podem criar novas tasks?", example = "true")
        Boolean membersCanCreateTasks,

        @Schema(description = "Dias de retenção do histórico de tasks excluídas (configurável apenas pelo OWNER; null ou 0 = permanente)", example = "7")
        Integer deletedTasksRetentionDays
) {
    public OrganizationRequestDTO(
            String nome,
            Boolean membersCanDeleteTasks,
            Boolean membersCanChangeDeadlines,
            Boolean membersCanReturnTasks,
            Boolean membersCanCreateTasks
    ) {
        this(nome, membersCanDeleteTasks, false, membersCanChangeDeadlines, membersCanReturnTasks, membersCanCreateTasks, null);
    }
}
