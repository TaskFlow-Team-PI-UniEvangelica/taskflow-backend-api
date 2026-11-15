package unievangelica.taskflow.api.domain.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationMemberEntity;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationMemberId;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationRole;

import java.util.Optional;
import java.util.List;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMemberEntity, OrganizationMemberId> {
    Optional<OrganizationMemberEntity> findByOrganizationIdAndUserId(Long organizationId, Long userId);
    List<OrganizationMemberEntity> findByUserId(Long userId);
    boolean existsByUserIdAndRole(Long userId, OrganizationRole role);

    /** Indica se os dois usuários são membros de ao menos uma organização em comum. */
    @Query("""
            SELECT CASE WHEN COUNT(m1) > 0 THEN true ELSE false END
            FROM OrganizationMemberEntity m1, OrganizationMemberEntity m2
            WHERE m1.organization.id = m2.organization.id
              AND m1.user.id = :usuarioA
              AND m2.user.id = :usuarioB
            """)
    boolean compartilhamOrganizacao(@Param("usuarioA") Long usuarioA, @Param("usuarioB") Long usuarioB);
}
