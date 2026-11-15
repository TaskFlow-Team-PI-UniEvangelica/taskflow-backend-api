package unievangelica.taskflow.api.test.integration.repositories;

import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import unievangelica.taskflow.api.domain.persistence.entities.*;
import unievangelica.taskflow.api.domain.persistence.repositories.*;
import unievangelica.taskflow.api.dto.response.MemberResponseDTO;
import unievangelica.taskflow.api.test.integration.support.RepositoryIntegrationTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Consultas dos repositórios executadas no PostgreSQL real (schema criado pelo Flyway).
 * Cobrem as consultas JPQL/nativas e as regras do banco (unicidade, cascatas) que os testes
 * unitários com mocks não alcançam.
 */
@RepositoryIntegrationTest
class RepositoriosIntegrationTest {

    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private OrganizationMemberRepository memberRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private TaskHistoryRepository historyRepository;
    @Autowired private OrganizationInviteRepository inviteRepository;
    @Autowired private UserRepository userRepository;

    private UserEntity ana;
    private UserEntity bruno;
    private UserEntity carla;
    private OrganizationEntity orgA;
    private OrganizationEntity orgB;

    // --- dados de teste ---

    private UserEntity usuario(String nome) {
        UserEntity u = new UserEntity();
        u.setNome(nome);
        u.setEmail(nome.toLowerCase() + "-" + UUID.randomUUID() + "@teste.local");
        u.setKeycloakId(UUID.randomUUID().toString());
        em.persist(u);
        return u;
    }

    private OrganizationEntity organizacao(String nome) {
        OrganizationEntity o = new OrganizationEntity();
        o.setNome(nome);
        em.persist(o);
        return o;
    }

    private void vincular(OrganizationEntity org, UserEntity user, OrganizationRole role) {
        OrganizationMemberEntity m = new OrganizationMemberEntity();
        m.setId(new OrganizationMemberId(org.getId(), user.getId()));
        m.setOrganization(org);
        m.setUser(user);
        m.setRole(role);
        em.persist(m);
    }

    private TaskEntity tarefa(OrganizationEntity org, UserEntity criador, int numero, UserEntity... responsaveis) {
        TaskEntity t = new TaskEntity();
        t.setTitulo("Tarefa " + numero);
        t.setStatus(TaskEntity.Status.pendente);
        t.setPrioridade(TaskEntity.Prioridade.media);
        t.setOrganization(org);
        t.setCriador(criador);
        t.setTaskNumber(numero);
        t.setResponsaveis(List.of(responsaveis));
        em.persist(t);
        return t;
    }

    private TaskHistoryEntity evento(OrganizationEntity org, TaskEntity task, UserEntity user, String acao) {
        TaskHistoryEntity h = TaskHistoryEntity.builder()
                .organization(org).task(task).user(user).actionType(acao)
                .taskTitle(task != null ? task.getTitulo() : "Excluída").build();
        em.persist(h);
        return h;
    }

    private static String causaRaiz(Throwable erro) {
        Throwable causa = erro;
        while (causa.getCause() != null) {
            causa = causa.getCause();
        }
        return causa.getMessage();
    }

    private void sincronizar() {
        em.flush();
        em.clear();
    }

    @BeforeEach
    void cenario() {
        ana = usuario("Ana");
        bruno = usuario("Bruno");
        carla = usuario("Carla");
        orgA = organizacao("Org A");
        orgB = organizacao("Org B");
        vincular(orgA, ana, OrganizationRole.OWNER);
        vincular(orgA, bruno, OrganizationRole.MEMBER);
        vincular(orgB, carla, OrganizationRole.OWNER);
        sincronizar();
    }

    @Nested
    @DisplayName("OrganizationMemberRepository")
    class Pertencimento {

        @Test
        @DisplayName("exists e findRole refletem o vínculo real")
        void existsEFindRole() {
            assertThat(memberRepository.existsByOrganizationIdAndUserId(orgA.getId(), bruno.getId())).isTrue();
            assertThat(memberRepository.existsByOrganizationIdAndUserId(orgA.getId(), carla.getId())).isFalse();
            assertThat(memberRepository.findRole(orgA.getId(), ana.getId())).contains(OrganizationRole.OWNER);
            assertThat(memberRepository.findRole(orgA.getId(), bruno.getId())).contains(OrganizationRole.MEMBER);
            assertThat(memberRepository.findRole(orgB.getId(), bruno.getId())).isEmpty();
        }

        @Test
        @DisplayName("countMembros conta só quem é membro da organização informada")
        void countMembros() {
            assertThat(memberRepository.countMembros(orgA.getId(), List.of(ana.getId(), bruno.getId()))).isEqualTo(2);
            assertThat(memberRepository.countMembros(orgA.getId(), List.of(ana.getId(), carla.getId()))).isEqualTo(1);
            assertThat(memberRepository.countMembros(orgB.getId(), List.of(ana.getId(), bruno.getId()))).isZero();
        }

        @Test
        @DisplayName("listarMembros devolve id, nome, e-mail e papel (texto) só da organização")
        void listarMembros() {
            List<MemberResponseDTO> membros = memberRepository.listarMembros(orgA.getId());

            assertThat(membros).extracting(MemberResponseDTO::id).containsExactlyInAnyOrder(ana.getId(), bruno.getId());
            assertThat(membros).filteredOn(m -> m.id().equals(ana.getId()))
                    .singleElement()
                    .satisfies(m -> {
                        assertThat(m.nome()).isEqualTo("Ana");
                        assertThat(m.email()).isEqualTo(ana.getEmail());
                        assertThat(m.roleNaOrganizacao()).isEqualTo("OWNER");
                    });
        }

        @Test
        @DisplayName("findByUserIdComOrganizacao traz a organização já carregada (sem consulta extra)")
        void joinFetchDaOrganizacao() {
            List<OrganizationMemberEntity> vinculos = memberRepository.findByUserIdComOrganizacao(ana.getId());

            assertThat(vinculos).hasSize(1);
            assertThat(Hibernate.isInitialized(vinculos.get(0).getOrganization())).isTrue();
            assertThat(vinculos.get(0).getOrganization().getNome()).isEqualTo("Org A");
        }

        @Test
        @DisplayName("compartilhamOrganizacao: verdadeiro só para quem tem organização em comum")
        void organizacaoEmComum() {
            assertThat(memberRepository.compartilhamOrganizacao(ana.getId(), bruno.getId())).isTrue();
            assertThat(memberRepository.compartilhamOrganizacao(bruno.getId(), carla.getId())).isFalse();
        }

        @Test
        @DisplayName("Membro por e-mail ignora maiúsculas/minúsculas; OWNER é detectado por papel")
        void emailEPapel() {
            assertThat(memberRepository.existsByOrganizationIdAndUserEmailIgnoreCase(orgA.getId(), bruno.getEmail().toUpperCase())).isTrue();
            assertThat(memberRepository.existsByOrganizationIdAndUserEmailIgnoreCase(orgA.getId(), carla.getEmail())).isFalse();
            assertThat(memberRepository.existsByUserIdAndRole(ana.getId(), OrganizationRole.OWNER)).isTrue();
            assertThat(memberRepository.existsByUserIdAndRole(bruno.getId(), OrganizationRole.OWNER)).isFalse();
        }
    }

    @Nested
    @DisplayName("Tarefas e numeração")
    class Tarefas {

        @Test
        @DisplayName("nextTaskNumber incrementa no banco e devolve o novo número")
        void numeracaoSequencial() {
            assertThat(organizationRepository.nextTaskNumber(orgA.getId())).isEqualTo(1);
            assertThat(organizationRepository.nextTaskNumber(orgA.getId())).isEqualTo(2);
            assertThat(organizationRepository.nextTaskNumber(orgB.getId())).isEqualTo(1);
        }

        @Test
        @DisplayName("Número de tarefa é único por organização (constraint do banco)")
        void numeroUnicoPorOrganizacao() {
            tarefa(orgA, ana, 1);
            tarefa(orgB, carla, 1);
            sincronizar();

            Exception erro = assertThrows(Exception.class, () -> tarefa(orgA, ana, 1));
            assertThat(causaRaiz(erro)).contains("uq_tasks_org_task_number");
        }

        @Test
        @DisplayName("Listagens de tarefas respeitam a organização e as organizações do usuário")
        void listagens() {
            TaskEntity daA = tarefa(orgA, ana, 1, bruno);
            TaskEntity daB = tarefa(orgB, carla, 1);
            sincronizar();

            assertThat(taskRepository.listTasksByOrganization(orgA.getId())).extracting(TaskEntity::getId).containsExactly(daA.getId());
            assertThat(taskRepository.listTasksByUserOrganizations(bruno.getId())).extracting(TaskEntity::getId).containsExactly(daA.getId());
            assertThat(taskRepository.listTasksByUserOrganizations(carla.getId())).extracting(TaskEntity::getId).containsExactly(daB.getId());
            assertThat(taskRepository.findTaskById(daA.getId())).get()
                    .satisfies(t -> assertThat(t.getResponsaveis()).extracting(UserEntity::getId).containsExactly(bruno.getId()));
        }
    }

    @Nested
    @DisplayName("Histórico e retenção de excluídas")
    class Historico {

        @Test
        @DisplayName("MEMBER: só eventos de tarefas existentes; gestor: excluídas dentro da retenção; expurgo remove só as vencidas")
        void retencao() {
            TaskEntity ativa = tarefa(orgA, ana, 1);
            evento(orgA, ativa, ana, "CREATED");
            TaskHistoryEntity recente = evento(orgA, null, ana, "DELETED");
            TaskHistoryEntity antiga = evento(orgA, null, ana, "DELETED");
            sincronizar();
            jdbc.update("UPDATE task_history SET timestamp = now() - interval '30 days' WHERE id = ?", antiga.getId());
            LocalDateTime corte = LocalDateTime.now().minusDays(7);

            assertThat(historyRepository.findExistingTasksByOrganizationIdOrderByTimestampDesc(orgA.getId()))
                    .extracting(TaskHistoryEntity::getActionType).containsExactly("CREATED");
            assertThat(historyRepository.findByOrganizationIdWithinDeletedRetentionOrderByTimestampDesc(orgA.getId(), corte))
                    .extracting(TaskHistoryEntity::getId).doesNotContain(antiga.getId()).contains(recente.getId());
            assertThat(historyRepository.findDeletedByOrganizationIdSinceOrderByTimestampDesc(orgA.getId(), corte))
                    .extracting(TaskHistoryEntity::getId).containsExactly(recente.getId());

            int removidos = historyRepository.deleteExpiredDeletedHistory(orgA.getId(), corte);

            assertThat(removidos).isEqualTo(1);
            assertThat(historyRepository.findDeletedByOrganizationIdOrderByTimestampDesc(orgA.getId()))
                    .extracting(TaskHistoryEntity::getId).containsExactly(recente.getId());
        }
    }

    @Nested
    @DisplayName("Convites e exclusão de usuário")
    class ConvitesEExclusao {

        @Test
        @DisplayName("Busca de convites pendentes ignora maiúsculas/minúsculas do e-mail")
        void convitesIgnoreCase() {
            OrganizationInviteEntity convite = OrganizationInviteEntity.builder()
                    .organization(orgA).inviter(ana).invitedEmail("convidado@teste.local").role("MEMBER")
                    .status(InviteStatus.PENDING).createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            em.persist(convite);
            sincronizar();

            assertThat(inviteRepository.findByInvitedEmailIgnoreCaseAndStatus("Convidado@Teste.LOCAL", InviteStatus.PENDING))
                    .extracting(OrganizationInviteEntity::getId).containsExactly(convite.getId());
            assertThat(inviteRepository.findByInvitedEmailIgnoreCaseAndStatus("convidado@teste.local", InviteStatus.ACCEPTED)).isEmpty();
        }

        @Test
        @DisplayName("Excluir usuário remove os vínculos e as responsabilidades (cascata) e anula o criador da tarefa")
        void cascataAoExcluirUsuario() {
            TaskEntity t = tarefa(orgA, bruno, 1, bruno);
            evento(orgA, t, bruno, "CREATED");
            sincronizar();

            userRepository.deleteUserById(bruno.getId());
            sincronizar();

            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM organization_users WHERE user_id = ?", Integer.class, bruno.getId())).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM task_responsibles WHERE user_id = ?", Integer.class, bruno.getId())).isZero();
            assertThat(jdbc.queryForObject("SELECT id_criador FROM tasks WHERE id = ?", Long.class, t.getId())).isNull();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM task_history WHERE task_id = ? AND user_id IS NULL", Integer.class, t.getId())).isEqualTo(1);
            assertThat(memberRepository.existsByOrganizationIdAndUserId(orgA.getId(), ana.getId())).isTrue();
        }

        @Test
        @DisplayName("E-mail de usuário é único no banco")
        void emailUnico() {
            UserEntity duplicado = new UserEntity();
            duplicado.setNome("Outra Ana");
            duplicado.setEmail(ana.getEmail());

            Exception erro = assertThrows(Exception.class, () -> em.persist(duplicado));
            assertThat(causaRaiz(erro)).contains("users_email_key");
        }
    }
}
