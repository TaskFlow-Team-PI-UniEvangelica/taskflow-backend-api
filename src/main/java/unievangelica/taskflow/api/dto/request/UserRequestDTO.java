package unievangelica.taskflow.api.dto.request;

/**
 * Dados editáveis do perfil pela API. O e-mail não faz parte: ele vem do Keycloak (claim
 * {@code email} verificada) e é usado para autorizar convites, por isso não pode ser alterado aqui.
 * Um campo {@code email} enviado no corpo é ignorado.
 */
public record UserRequestDTO(
        String nome,
        String senha,
        String cargo
) {
}
