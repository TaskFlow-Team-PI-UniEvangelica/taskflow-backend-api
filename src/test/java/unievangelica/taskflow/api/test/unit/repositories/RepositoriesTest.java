package unievangelica.taskflow.api.test.unit.repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationMemberRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.TaskHistoryRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class RepositoriesTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private TaskHistoryRepository taskHistoryRepository;

    @Test
    @DisplayName("Deve mockar os repositórios corretamente em teste unitário")
    void testRepositoriesInjected() {
        assertThat(userRepository).isNotNull();
        assertThat(organizationRepository).isNotNull();
        assertThat(organizationMemberRepository).isNotNull();
        assertThat(taskHistoryRepository).isNotNull();
    }
}
