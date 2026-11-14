package unievangelica.taskflow.api.domain.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationEntity;

import java.util.List;

@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, Long> {

    // Organizações com política de retenção de excluídas ativa (null = retenção permanente)
    List<OrganizationEntity> findByDeletedTasksRetentionDaysGreaterThan(Integer days);
}
