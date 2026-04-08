package unievangelica.taskflow.api.domain.persistence.entities;
// não preciso comentar aqui mais o esqueleto da entidade é basicamente o mesmo do user
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Table( name = "tasks") // anotação pra apontar qual a tabela
@Entity( name = "Task") // anotação nomeando a entidade
@Getter // método de geração de getters do lombok
@Setter // método de geração de getters do lombok
@AllArgsConstructor  // cria um construtor com os argumentos dos campos da tabela
@NoArgsConstructor // cria um construtor vazio usando o lombok para puxar os dados
@EqualsAndHashCode(of = "id") // forçar o java a considerar dois objetos com mesmo id como iguais
public class TaskEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) // verificações de colunas para garantir que o JPA trate isso como not null
    private String titulo;

    private String descricao;

    // tratamento dos campos enum criados nas migrations
    @Enumerated(EnumType.STRING) // passa o tipo do enum como parametro para salvar string nesse caso
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) // diz pro banco de dados tratar como enum
    @Column(columnDefinition = "status_tarefa") // dizendo qual a coluna possui o enum e deve se comportar como tal
    private Status status;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "prioridade_tarefa")
    private Prioridade prioridade;

    private LocalDate prazo;

    @ManyToOne // falando o tipo de relacionamento da chave estrangeira no caso many to one
    @JoinColumn(name = "id_criador")
    private UserEntity criador;

    // novo relacionamento N to N para os responsáveis das tasks
    @ManyToMany
    @JoinTable(
            name = "task_responsibles",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )

    private List<UserEntity> responsaveis = new ArrayList<>();

    // lista com todos os valores presentes no enum
    public enum Status {
        pendente,
        em_andamento,
        concluida
    }

    public enum Prioridade {
        baixa,
        media,
        alta
    }
}
