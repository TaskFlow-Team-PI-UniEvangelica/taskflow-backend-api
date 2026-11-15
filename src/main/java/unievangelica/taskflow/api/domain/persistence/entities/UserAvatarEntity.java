package unievangelica.taskflow.api.domain.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Imagem de perfil do usuário, em tabela própria (ALT-31): só é lida quando o avatar é pedido,
 * e não a cada consulta de usuário. Removida em cascata com o usuário.
 */
@Entity
@Table(name = "user_avatars")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAvatarEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "content", nullable = false)
    private byte[] content;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
