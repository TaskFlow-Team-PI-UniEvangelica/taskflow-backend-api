package unievangelica.taskflow.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Resposta contendo os dados de uma Organização e suas regras")
public record OrganizationResponseDTO(
        Long id,
        String nome,
        LocalDateTime criadoEm,
        Boolean membersCanDeleteTasks,
        Boolean membersCanChangeDeadlines,
        Boolean membersCanReturnTasks,
        Boolean membersCanCreateTasks,
        String myRole // Papel (Role) que o usuário logado tem nesta organização
) {}
