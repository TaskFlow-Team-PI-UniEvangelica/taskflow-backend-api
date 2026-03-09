package unievangelica.taskflow.api.dto.request;

public record UserRequestDTO(
        String nome,
        String email,
        String senha,
        String cargo
) {
}
