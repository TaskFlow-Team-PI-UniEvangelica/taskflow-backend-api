package unievangelica.taskflow.api.domain.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import unievangelica.taskflow.api.domain.persistence.entities.TaskEntity;

import java.util.List;
// repository responsável por fazer as consultas dentro do banco de dados
@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    // o springboot consegue identificar como seria a consulta dos métodos pelo nome
}
