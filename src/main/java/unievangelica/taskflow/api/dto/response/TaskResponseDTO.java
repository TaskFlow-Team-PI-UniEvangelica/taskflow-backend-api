package unievangelica.taskflow.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

// armazena quais campos devem ser retornados em um método de retorno por exemplo no controller
@Schema(description = "Dados de resposta de uma Tarefa dentro de uma Organização")
public record TaskResponseDTO(
        @Schema(description = "ID interno global da tarefa no banco de dados", example = "42")
        Long id,

        @Schema(description = "Número sequencial imutável da tarefa dentro da organização (1, 2, 3...)", example = "1")
        Integer taskNumber,

        @Schema(description = "Título da tarefa", example = "Implementar autenticação OIDC")
        String titulo,

        @Schema(description = "Descrição detalhada da tarefa")
        String descricao,

        @Schema(description = "Status atual da tarefa (pendente, em_andamento, concluida)", example = "pendente")
        String status,

        @Schema(description = "Prioridade da tarefa (baixa, media, alta)", example = "alta")
        String prioridade,

        @Schema(description = "Prazo de entrega (opcional)", example = "2026-12-31")
        LocalDate prazo,

        @Schema(description = "ID do usuário criador da tarefa", example = "1")
        Long criadorId,

        @Schema(description = "Nome do usuário criador da tarefa", example = "João Silva")
        String nomeCriador,

        @Schema(description = "Lista de IDs dos membros responsáveis pela tarefa")
        List<Long> idsResponsaveis,

        @Schema(description = "Lista de nomes dos membros responsáveis pela tarefa")
        List<String> nomesResponsaveis
) {
    public TaskResponseDTO(
            Long id,
            String titulo,
            String descricao,
            String status,
            String prioridade,
            LocalDate prazo,
            String nomeCriador,
            List<Long> idsResponsaveis,
            List<String> nomesResponsaveis
    ) {
        this(id, 1, titulo, descricao, status, prioridade, prazo, null, nomeCriador, idsResponsaveis, nomesResponsaveis);
    }

    public TaskResponseDTO(
            Long id,
            String titulo,
            String descricao,
            String status,
            String prioridade,
            LocalDate prazo,
            String nomeCriador,
            List<String> nomesResponsaveis
    ) {
        this(id, 1, titulo, descricao, status, prioridade, prazo, null, nomeCriador, List.of(), nomesResponsaveis);
    }
}