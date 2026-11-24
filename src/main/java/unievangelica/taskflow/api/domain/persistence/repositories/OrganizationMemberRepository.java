package unievangelica.taskflow.api.domain.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationMemberEntity;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationMemberId;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationRole;
import unievangelica.taskflow.api.dto.response.MemberResponseDTO;

import java.util.Collection;
import java.util.Optional;
import java.util.List;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMemberEntity, OrganizationMemberId> {
    Optional<OrganizationMemberEntity> findByOrganizationIdAndUserId(Long organizationId, Long userId);
    List<OrganizationMemberEntity> findByUserId(Long userId);
    boolean existsByUserIdAndRole(Long userId, OrganizationRole role);

    // --- Checagens de pertencimento por consulta direta (sem carregar a lista de membros) ---

    boolean existsByOrganizationIdAndUserId(Long organizationId, Long userId);

    boolean existsByOrganizationIdAndUserEmailIgnoreCase(Long organizationId, String email);

    @Query("SELECT m.role FROM OrganizationMemberEntity m WHERE m.organization.id = :organizationId AND m.user.id = :userId")
    Optional<OrganizationRole> findRole(@Param("organizationId") Long organizationId, @Param("userId") Long userId);

    /** Quantos dos usuários informados são membros da organização (para validar responsáveis numa só consulta). */
    @Query("SELECT COUNT(m) FROM OrganizationMemberEntity m WHERE m.organization.id = :organizationId AND m.user.id IN :userIds")
    long countMembros(@Param("organizationId") Long organizationId, @Param("userIds") Collection<Long> userIds);

    /** Membros da organização já no formato de resposta, em uma consulta e sem carregar o avatar. */
    @Query("""
            SELECT new unievangelica.taskflow.api.dto.response.MemberResponseDTO(u.id, u.nome, u.email, CAST(m.role AS String))
            FROM OrganizationMemberEntity m JOIN m.user u
            WHERE m.organization.id = :organizationId
            """)
    List<MemberResponseDTO> listarMembros(@Param("organizationId") Long organizationId);

    /** Vínculos do usuário com a organização já carregada (evita uma consulta por organização). */
    @Query("SELECT m FROM OrganizationMemberEntity m JOIN FETCH m.organization WHERE m.user.id = :userId")
    List<OrganizationMemberEntity> findByUserIdComOrganizacao(@Param("userId") Long userId);

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
