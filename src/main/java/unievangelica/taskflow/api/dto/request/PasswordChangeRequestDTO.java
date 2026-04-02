package unievangelica.taskflow.api.dto.request;

public record PasswordChangeRequestDTO(
        String senhaAtual,
        String novaSenha
) {
}
