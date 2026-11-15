package unievangelica.taskflow.api.domain.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationEntity;

import java.util.List;

@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, Long> {

    // Organizações com política de retenção de excluídas ativa (null = retenção permanente)
    List<OrganizationEntity> findByDeletedTasksRetentionDaysGreaterThan(Integer days);

    /**
     * Reserva o próximo número sequencial de tarefa da organização de forma atômica (PostgreSQL).
     * O UPDATE trava a linha até o fim da transação: criações simultâneas recebem números distintos
     * e consecutivos, sem regravar as demais colunas da organização.
     * Sem @Modifying: com RETURNING a consulta devolve um resultado. Deve rodar dentro da transação de criarTask.
     */
    @Query(value = "UPDATE organizations SET last_task_number = last_task_number + 1 WHERE id = :orgId RETURNING last_task_number",
            nativeQuery = true)
    int nextTaskNumber(@Param("orgId") Long orgId);
}
