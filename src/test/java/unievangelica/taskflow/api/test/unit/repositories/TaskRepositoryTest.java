package unievangelica.taskflow.api.test.unit.repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unievangelica.taskflow.api.domain.persistence.repositories.TaskRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class TaskRepositoryTest {

    @Mock
    private TaskRepository taskRepository;

    @Test
    @DisplayName("Deve mockar o repositório corretamente em teste unitário")
    void testRepositoryInjected() {
        assertThat(taskRepository).isNotNull();
    }
}

