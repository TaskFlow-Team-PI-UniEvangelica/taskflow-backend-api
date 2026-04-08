package unievangelica.taskflow.api.dto.request;

import java.util.List;

// armazena quais campos devem ser requisitados para alteração
public record TaskRequestDTO(
        String titulo,
        String descricao,
        String status,
        String prioridade,
        String prazo,
        List<Long> idsResponsaveis
) {
}
