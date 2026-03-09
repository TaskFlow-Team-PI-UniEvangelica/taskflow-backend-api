package unievangelica.taskflow.api.dto.response;

public record UserResponseDTO(
        Long id,
        String nome,
        String email,
        String cargo
) {
}
