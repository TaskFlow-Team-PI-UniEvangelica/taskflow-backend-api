package unievangelica.taskflow.api.test.unit.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationEntity;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationMemberEntity;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationMemberId;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationRole;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrganizationMemberEntityTest {

    @Test
    @DisplayName("Deve testar entidade e ID composto")
    void testGettersAndSetters() {
        OrganizationMemberId id = new OrganizationMemberId(10L, 20L);
        assertEquals(10L, id.getOrganizationId());
        assertEquals(20L, id.getUserId());

        OrganizationMemberEntity member = new OrganizationMemberEntity();
        member.setId(id);
        member.setRole(OrganizationRole.OWNER);

        OrganizationEntity org = new OrganizationEntity();
        org.setId(10L);
        member.setOrganization(org);

        UserEntity user = new UserEntity();
        user.setId(20L);
        member.setUser(user);

        assertEquals(id, member.getId());
        assertEquals(OrganizationRole.OWNER, member.getRole());
        assertEquals(10L, member.getOrganization().getId());
        assertEquals(20L, member.getUser().getId());
    }
}
