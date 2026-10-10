package unievangelica.taskflow.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Objeto de requisição para criar ou atualizar uma Organização")
public record OrganizationRequestDTO(
        @Schema(description = "Nome da organização", example = "Equipe Alpha")
        String nome,

        @Schema(description = "Permissão: membros podem deletar tasks?", example = "false")
        Boolean membersCanDeleteTasks,

        @Schema(description = "Permissão: membros podem alterar datas limite?", example = "false")
        Boolean membersCanChangeDeadlines,

        @Schema(description = "Permissão: membros podem retornar tasks para status anteriores?", example = "false")
        Boolean membersCanReturnTasks,

        @Schema(description = "Permissão: membros podem criar novas tasks?", example = "true")
        Boolean membersCanCreateTasks
) {}
