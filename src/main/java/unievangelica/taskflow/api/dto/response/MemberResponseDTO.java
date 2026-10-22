package unievangelica.taskflow.api.dto.response;

public record MemberResponseDTO(
    Long id,
    String nome,
    String email,
    
    String roleNaOrganizacao
) {}
