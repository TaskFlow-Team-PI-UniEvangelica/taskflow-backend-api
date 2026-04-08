package unievangelica.taskflow.api.dto.response;

import java.time.LocalDate;
import java.util.List;

// armazena quais campos devem ser retornados em um método de retorno por exemplo no controller
public record TaskResponseDTO(
        Long id,
        String titulo,
        String descricao,
        String status,
        String prioridade,
        LocalDate prazo,
        String nomeCriador,
        List<String> nomesResponsaveis
) {
}