package unievangelica.taskflow.api.dto.response;

import java.time.LocalDateTime;

public record InviteResponseDTO(
        Long id,
        Long organizationId,
        String organizationName,
        String inviterName,
        String role,
        String status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {}
