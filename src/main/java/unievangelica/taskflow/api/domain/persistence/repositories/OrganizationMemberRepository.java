package unievangelica.taskflow.api.domain.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationMemberEntity;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationMemberId;

import java.util.Optional;
import java.util.List;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMemberEntity, OrganizationMemberId> {
    Optional<OrganizationMemberEntity> findByOrganizationIdAndUserId(Long organizationId, Long userId);
    List<OrganizationMemberEntity> findByUserId(Long userId);
}
