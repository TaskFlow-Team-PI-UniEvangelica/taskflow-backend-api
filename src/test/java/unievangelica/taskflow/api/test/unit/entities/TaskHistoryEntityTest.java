package unievangelica.taskflow.api.test.unit.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import unievangelica.taskflow.api.domain.persistence.entities.TaskEntity;
import unievangelica.taskflow.api.domain.persistence.entities.TaskHistoryEntity;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TaskHistoryEntityTest {

    @Test
    @DisplayName("Deve testar getters e setters de TaskHistoryEntity")
    void testGettersAndSetters() {
        TaskHistoryEntity history = new TaskHistoryEntity();
        history.setId(1L);
        history.setActionType("CREATE");
        
        TaskEntity task = new TaskEntity();
        task.setId(10L);
        history.setTask(task);

        UserEntity user = new UserEntity();
        user.setId(20L);
        history.setUser(user);

        assertEquals(1L, history.getId());
        assertEquals("CREATE", history.getActionType());
        assertEquals(10L, history.getTask().getId());
        assertEquals(20L, history.getUser().getId());
    }
}
