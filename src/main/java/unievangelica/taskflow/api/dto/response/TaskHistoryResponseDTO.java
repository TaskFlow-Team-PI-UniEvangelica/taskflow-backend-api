package unievangelica.taskflow.api.dto.response;

import java.time.LocalDateTime;

public record TaskHistoryResponseDTO(
        Long id,
        String actionType,
        String oldValue,
        String newValue,
        String userName,
        LocalDateTime timestamp
) {
}
