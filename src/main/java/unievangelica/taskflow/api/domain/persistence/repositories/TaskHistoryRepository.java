package unievangelica.taskflow.api.domain.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import unievangelica.taskflow.api.domain.persistence.entities.TaskHistoryEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistoryEntity, Long> {

    @Query("SELECT h FROM TaskHistoryEntity h LEFT JOIN FETCH h.user LEFT JOIN FETCH h.task WHERE h.task.id = :taskId ORDER BY h.timestamp DESC, h.id DESC")
    List<TaskHistoryEntity> findByTaskIdOrderByTimestampDesc(@Param("taskId") Long taskId);

    @Query("SELECT h FROM TaskHistoryEntity h LEFT JOIN FETCH h.user LEFT JOIN FETCH h.task WHERE h.organization.id = :organizationId ORDER BY h.timestamp DESC, h.id DESC")
    List<TaskHistoryEntity> findByOrganizationIdOrderByTimestampDesc(@Param("organizationId") Long organizationId);

    @Query("SELECT h FROM TaskHistoryEntity h LEFT JOIN FETCH h.user LEFT JOIN FETCH h.task WHERE h.organization.id = :organizationId AND h.actionType = 'DELETED' ORDER BY h.timestamp DESC, h.id DESC")
    List<TaskHistoryEntity> findDeletedByOrganizationIdOrderByTimestampDesc(@Param("organizationId") Long organizationId);

    @Query("SELECT h FROM TaskHistoryEntity h LEFT JOIN FETCH h.user LEFT JOIN FETCH h.task WHERE h.organization.id = :organizationId AND h.actionType = 'DELETED' AND h.timestamp >= :cutoff ORDER BY h.timestamp DESC, h.id DESC")
    List<TaskHistoryEntity> findDeletedByOrganizationIdSinceOrderByTimestampDesc(
            @Param("organizationId") Long organizationId,
            @Param("cutoff") LocalDateTime cutoff
    );

    @Modifying
    @Query("DELETE FROM TaskHistoryEntity h WHERE h.organization.id = :organizationId AND h.actionType = 'DELETED' AND h.timestamp < :cutoff")
    void deleteExpiredDeletedHistory(
            @Param("organizationId") Long organizationId,
            @Param("cutoff") LocalDateTime cutoff
    );
}
