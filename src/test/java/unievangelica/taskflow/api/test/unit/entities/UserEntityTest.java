package unievangelica.taskflow.api.test.unit.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserEntityTest {

    @Test
    @DisplayName("Deve testar getters e setters da UserEntity")
    void testGettersAndSetters() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setNome("Test");
        user.setKeycloakId("keycloak-id-123");

        assertEquals(1L, user.getId());
        assertEquals("Test", user.getNome());
        assertEquals("keycloak-id-123", user.getKeycloakId());
    }
}
