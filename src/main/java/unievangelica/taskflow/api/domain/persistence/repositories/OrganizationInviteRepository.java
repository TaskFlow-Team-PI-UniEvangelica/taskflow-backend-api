package unievangelica.taskflow.api.domain.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import unievangelica.taskflow.api.domain.persistence.entities.InviteStatus;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationInviteEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationInviteRepository extends JpaRepository<OrganizationInviteEntity, Long> {
    List<OrganizationInviteEntity> findByInvitedEmailAndStatus(String email, InviteStatus status);
    List<OrganizationInviteEntity> findByInvitedEmailIgnoreCaseAndStatus(String email, InviteStatus status);
    Optional<OrganizationInviteEntity> findByIdAndInvitedEmail(Long id, String email);
    Optional<OrganizationInviteEntity> findByOrganizationIdAndInvitedEmailAndStatus(Long organizationId, String email, InviteStatus status);
    Optional<OrganizationInviteEntity> findByOrganizationIdAndInvitedEmailIgnoreCaseAndStatus(Long organizationId, String email, InviteStatus status);
    List<OrganizationInviteEntity> findByStatusAndExpiresAtBefore(InviteStatus status, LocalDateTime dateTime);
}
