package unievangelica.taskflow.api.test.unit.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationEntity;
import unievangelica.taskflow.api.domain.persistence.entities.TaskEntity;
import unievangelica.taskflow.api.domain.persistence.entities.TaskHistoryEntity;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TaskHistoryEntityTest {

    @Test
    @DisplayName("Deve testar getters e setters de TaskHistoryEntity incluindo organization e taskTitle")
    void testGettersAndSetters() {
        TaskHistoryEntity history = new TaskHistoryEntity();
        history.setId(1L);
        history.setActionType("CREATED");
        history.setTaskTitle("Tarefa Inicial");
        
        TaskEntity task = new TaskEntity();
        task.setId(10L);
        history.setTask(task);

        OrganizationEntity org = new OrganizationEntity();
        org.setId(30L);
        history.setOrganization(org);

        UserEntity user = new UserEntity();
        user.setId(20L);
        history.setUser(user);

        assertEquals(1L, history.getId());
        assertEquals("CREATED", history.getActionType());
        assertEquals("Tarefa Inicial", history.getTaskTitle());
        assertEquals(10L, history.getTask().getId());
        assertEquals(30L, history.getOrganization().getId());
        assertEquals(20L, history.getUser().getId());
    }
}
