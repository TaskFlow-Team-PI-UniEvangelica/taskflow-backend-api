package unievangelica.taskflow.api.domain.persistence.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import unievangelica.taskflow.api.domain.persistence.entities.InviteStatus;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationInviteEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationInviteRepository extends JpaRepository<OrganizationInviteEntity, Long> {
    @Query("SELECT i FROM OrganizationInviteEntity i JOIN FETCH i.organization JOIN FETCH i.inviter "
            + "WHERE UPPER(i.invitedEmail) = UPPER(:email) AND i.status = :status")
    List<OrganizationInviteEntity> findByInvitedEmailIgnoreCaseAndStatus(
            @Param("email") String email,
            @Param("status") InviteStatus status);

    List<OrganizationInviteEntity> findByOrganizationIdAndInvitedEmailIgnoreCaseAndStatus(Long organizationId, String email, InviteStatus status);
    List<OrganizationInviteEntity> findByStatusAndExpiresAtBefore(InviteStatus status, LocalDateTime dateTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM OrganizationInviteEntity i WHERE i.id = :id")
    Optional<OrganizationInviteEntity> findByIdForUpdate(@Param("id") Long id);
}
