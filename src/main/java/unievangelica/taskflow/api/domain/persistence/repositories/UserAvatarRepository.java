package unievangelica.taskflow.api.domain.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import unievangelica.taskflow.api.domain.persistence.entities.UserAvatarEntity;

@Repository
public interface UserAvatarRepository extends JpaRepository<UserAvatarEntity, Long> {
}
