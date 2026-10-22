package unievangelica.taskflow.api.domain.persistence.entities;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OrganizationMemberId implements Serializable {
    private Long organizationId;
    private Long userId;
}
