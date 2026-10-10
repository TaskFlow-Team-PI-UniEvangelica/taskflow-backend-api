package unievangelica.taskflow.api.domain.persistence.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Table(name = "organizations")
@Entity(name = "Organization")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class OrganizationEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "members_can_delete_tasks", nullable = false)
    private boolean membersCanDeleteTasks = false;

    @Column(name = "members_can_change_deadlines", nullable = false)
    private boolean membersCanChangeDeadlines = false;

    @Column(name = "members_can_return_tasks", nullable = false)
    private boolean membersCanReturnTasks = false;

    @Column(name = "members_can_create_tasks", nullable = false)
    private boolean membersCanCreateTasks = true;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrganizationMemberEntity> members = new ArrayList<>();
}
