package unievangelica.taskflow.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Objeto de requisição para adicionar um membro ou alterar seu papel na organização")
public record MemberRequestDTO(
        @Schema(description = "ID do usuário a ser adicionado ou alterado", example = "10")
        Long userId,

        @Schema(description = "Email do usuário a ser adicionado", example = "usuario@email.com")
        String email,

        @Schema(description = "Papel (Role) que o membro terá na organização (MEMBER, MANAGER, SUB_OWNER)", example = "MANAGER")
        String role
) {}
