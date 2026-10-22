package unievangelica.taskflow.api.test.unit.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationEntity;

import static org.junit.jupiter.api.Assertions.*;

public class OrganizationEntityTest {

    @Test
    @DisplayName("Deve testar getters e setters da OrganizationEntity")
    void testGettersAndSetters() {
        OrganizationEntity org = new OrganizationEntity();
        org.setId(1L);
        org.setNome("Test Org");
        org.setMembersCanCreateTasks(false);

        assertEquals(1L, org.getId());
        assertEquals("Test Org", org.getNome());
        assertFalse(org.isMembersCanCreateTasks());
    }
}
