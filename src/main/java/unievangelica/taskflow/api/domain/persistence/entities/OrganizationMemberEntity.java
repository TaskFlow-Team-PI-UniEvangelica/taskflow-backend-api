package unievangelica.taskflow.api.domain.persistence.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "organization_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMemberEntity {

    @EmbeddedId
    private OrganizationMemberId id = new OrganizationMemberId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("organizationId")
    @JoinColumn(name = "organization_id")
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private OrganizationRole role = OrganizationRole.MEMBER;
}
