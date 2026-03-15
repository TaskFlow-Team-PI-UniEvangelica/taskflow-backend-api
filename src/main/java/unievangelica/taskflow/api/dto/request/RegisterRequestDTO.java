package unievangelica.taskflow.api.dto.request;

import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;

public record RegisterRequestDTO(
        String nome,
        String email,
        String senha,
        UserEntity.Cargo cargo
) {
}
