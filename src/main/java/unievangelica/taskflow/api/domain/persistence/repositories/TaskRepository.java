package unievangelica.taskflow.api.domain.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import unievangelica.taskflow.api.domain.persistence.entities.TaskEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// repository responsável por fazer as consultas dentro do banco de dados
// o springboot consegue identificar como seria a consulta dos métodos pelo nome por meio do JPA assim fazendo as consultas automaticamente
// porem estamos utilizando consultas JPQL para acessar o banco de dados em métodos como update select e delete
@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    @Query("SELECT DISTINCT t FROM Task t " +
            "JOIN FETCH t.criador " +
            "LEFT JOIN FETCH t.responsaveis")
    List<TaskEntity> listAllTasks();

    @Query("SELECT t FROM Task t " +
            "JOIN FETCH t.criador " +
            "LEFT JOIN FETCH t.responsaveis " +
            "WHERE t.id = :id")
    Optional<TaskEntity> findTaskById(@Param("id") Long id);

    @Query("SELECT COUNT(t) > 0 FROM Task t WHERE t.id = :id")
    boolean taskExist(@Param("id") Long id);

    // updates e deletes usando modify sem uso por enquanto o update
    @Modifying
    @Query("UPDATE Task t SET t.titulo = :titulo, t.descricao = :descricao, t.prazo = :prazo, t.prioridade = :prioridade, t.status = :status WHERE t.id = :id")
    void updateTask(
            @Param("id") Long id,
            @Param("titulo") String titulo,
            @Param("descricao") String descricao,
            @Param("prazo") LocalDate prazo,
            @Param("prioridade") TaskEntity.Prioridade prioridade,
            @Param("status") TaskEntity.Status status
    );

    @Modifying
    @Query("DELETE FROM Task t WHERE t.id = :id")
    void deleteTask(@Param("id") Long id);
}
