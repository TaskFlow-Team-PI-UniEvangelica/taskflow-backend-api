package unievangelica.taskflow.api.test.unit.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unievangelica.taskflow.api.domain.exception.BusinessException;
import unievangelica.taskflow.api.domain.exception.ResourceNotFoundException;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationEntity;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationMemberEntity;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationRole;
import unievangelica.taskflow.api.domain.persistence.entities.TaskEntity;
import unievangelica.taskflow.api.domain.persistence.entities.TaskHistoryEntity;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationMemberRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.TaskHistoryRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.TaskRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.domain.service.OrganizationService;
import unievangelica.taskflow.api.domain.service.TaskService;
import unievangelica.taskflow.api.dto.request.TaskRequestDTO;
import unievangelica.taskflow.api.dto.response.TaskHistoryResponseDTO;
import unievangelica.taskflow.api.dto.response.TaskResponseDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private TaskHistoryRepository taskHistoryRepository;

    @Mock
    private OrganizationMemberRepository memberRepository;

    @InjectMocks
    private TaskService taskService;

    /**
     * "Banco" de vínculos simulado: o TaskService checa pertencimento pelo repositório (exists, findRole,
     * countMembros). As respostas vêm das organizações registradas pelos próprios testes.
     */
    private final Map<Long, OrganizationEntity> organizacoes = new HashMap<>();

    private OrganizationEntity registrar(OrganizationEntity org) {
        organizacoes.put(org.getId(), org);
        return org;
    }

    private Optional<OrganizationMemberEntity> vinculo(Long orgId, Long userId) {
        OrganizationEntity org = organizacoes.get(orgId);
        if (org == null || org.getMembers() == null) {
            return Optional.empty();
        }
        return org.getMembers().stream()
                .filter(m -> m.getUser() != null && userId.equals(m.getUser().getId()))
                .findFirst();
    }

    @BeforeEach
    void simularVinculos() {
        lenient().when(memberRepository.existsByOrganizationIdAndUserId(anyLong(), anyLong()))
                .thenAnswer(inv -> vinculo(inv.getArgument(0), inv.getArgument(1)).isPresent());
        lenient().when(memberRepository.findRole(anyLong(), anyLong()))
                .thenAnswer(inv -> vinculo(inv.getArgument(0), inv.getArgument(1)).map(OrganizationMemberEntity::getRole));
        lenient().when(memberRepository.countMembros(anyLong(), anyCollection()))
                .thenAnswer(inv -> {
                    Long orgId = inv.getArgument(0);
                    Collection<Long> ids = inv.getArgument(1);
                    return ids.stream().filter(id -> vinculo(orgId, id).isPresent()).count();
                });
    }

    private OrganizationEntity buildOrgWithMember(Long orgId, UserEntity memberUser, OrganizationRole role) {
        OrganizationEntity org = new OrganizationEntity();
        org.setId(orgId);
        OrganizationMemberEntity membership = new OrganizationMemberEntity();
        membership.setOrganization(org);
        membership.setUser(memberUser);
        membership.setRole(role);
        org.setMembers(List.of(membership));
        return registrar(org);
    }

    @Test
    @DisplayName("Deve listar tarefas corretamente e converter para o DTO")
    void testaListarTasksEConverterParaDTO() {
        UserEntity criador = new UserEntity();
        criador.setId(5L);
        criador.setNome("UnitTestServiceUser");

        TaskEntity taskMock = new TaskEntity();
        taskMock.setId(1L);
        taskMock.setTaskNumber(3);
        taskMock.setTitulo("Unit Test Service Titulo");
        taskMock.setStatus(TaskEntity.Status.pendente);
        taskMock.setPrioridade(TaskEntity.Prioridade.media);
        taskMock.setCriador(criador);
        taskMock.setResponsaveis(List.of());

        UserEntity usuarioLogado = new UserEntity();
        usuarioLogado.setId(5L);

        when(taskRepository.listTasksByUserOrganizations(5L)).thenReturn(List.of(taskMock));

        List<TaskResponseDTO> resultado = taskService.listarTasks(null, usuarioLogado);

        assertThat(resultado).isNotNull().hasSize(1);
        TaskResponseDTO taskRetornada = resultado.get(0);
        assertThat(taskRetornada.taskNumber()).isEqualTo(3);
        assertThat(taskRetornada.criadorId()).isEqualTo(5L);
        assertThat(taskRetornada.titulo()).isEqualTo("Unit Test Service Titulo");
        assertThat(taskRetornada.status()).isEqualTo("pendente");
        assertThat(taskRetornada.prioridade()).isEqualTo("media");
        assertThat(taskRetornada.nomeCriador()).isEqualTo("UnitTestServiceUser");

        verify(taskRepository, times(1)).listTasksByUserOrganizations(5L);
    }

    @Test
    @DisplayName("Deve negar listagem geral de tarefas quando o usuário não está identificado")
    void testaListarTasksSemUsuarioIdentificadoNegaAcesso() {
        assertThrows(SecurityException.class, () -> taskService.listarTasks(null, null));
        assertThrows(SecurityException.class, () -> taskService.listarTasks(null, new UserEntity()));

        verifyNoInteractions(taskRepository);
    }

    @Test
    @DisplayName("Deve negar listagem de tarefas da organização quando o usuário não está identificado")
    void testaListarTasksPorOrganizacaoSemUsuarioIdentificadoNegaAcesso() {
        assertThrows(SecurityException.class, () -> taskService.listarTasks(100L, null));
        assertThrows(SecurityException.class, () -> taskService.listarTasks(100L, new UserEntity()));

        verifyNoInteractions(taskRepository);
        verify(organizationRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Deve bloquear listagem de tarefas de uma organização da qual o usuário não é membro")
    void testaListarTasksPorOrganizacaoSemSerMembro() {
        UserEntity intruso = new UserEntity();
        intruso.setId(99L);

        UserEntity membroReal = new UserEntity();
        membroReal.setId(1L);
        buildOrgWithMember(100L, membroReal, OrganizationRole.OWNER);

        when(organizationRepository.existsById(100L)).thenReturn(true);

        assertThrows(SecurityException.class, () -> taskService.listarTasks(100L, intruso));
        verify(taskRepository, never()).listTasksByOrganization(anyLong());
    }

    @Test
    @DisplayName("Listagem de tarefas de organização inexistente responde 'não encontrada' antes da checagem de vínculo")
    void testaListarTasksDeOrganizacaoInexistente() {
        when(organizationRepository.existsById(404L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> taskService.listarTasks(404L, usuario(1L)));
        verify(memberRepository, never()).existsByOrganizationIdAndUserId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Pertencimento é checado por consulta direta, sem carregar a lista de membros da organização")
    void testaChecagemDePertencimentoSemListaDeMembros() {
        UserEntity membro = usuario(2L);
        OrganizationEntity org = new OrganizationEntity();
        org.setId(10L);
        org.setMembers(null); // a lista de membros não é usada

        TaskEntity task = new TaskEntity();
        task.setId(61L);
        task.setStatus(TaskEntity.Status.pendente);
        task.setOrganization(org);

        when(taskRepository.findTaskById(61L)).thenReturn(Optional.of(task));
        when(memberRepository.findRole(10L, 2L)).thenReturn(Optional.of(OrganizationRole.MANAGER));

        taskService.atualizarStatusTask(61L, "em_andamento", membro);

        verify(taskRepository).updateTaskStatus(61L, TaskEntity.Status.em_andamento);
        // Vínculo e papel vêm de uma única consulta
        verify(memberRepository, times(1)).findRole(10L, 2L);
        verify(memberRepository, never()).existsByOrganizationIdAndUserId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Criação com responsável que existe mas não é membro da organização é recusada (contagem em uma consulta)")
    void testarCriarTaskComResponsavelDeOutraOrganizacao() {
        UserEntity criador = usuario(1L);
        UserEntity deFora = usuario(50L);
        OrganizationEntity org = buildOrgWithMember(100L, criador, OrganizationRole.OWNER);

        when(organizationService.hasPermissionTo(criador, 100L, "CREATE_TASK")).thenReturn(true);
        when(organizationRepository.findById(100L)).thenReturn(Optional.of(org));
        when(userRepository.userFindAllByIds(List.of(1L, 50L))).thenReturn(List.of(criador, deFora));

        TaskRequestDTO dto = new TaskRequestDTO("Titulo", null, null, null, null, List.of(1L, 50L), 100L);

        SecurityException ex = assertThrows(SecurityException.class, () -> taskService.criarTask(dto, criador));
        assertThat(ex.getMessage()).isEqualTo("Um ou mais usuários não pertencem à organização.");
        verify(memberRepository).countMembros(eq(100L), argThat(ids -> ids.containsAll(List.of(1L, 50L)) && ids.size() == 2));
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve criar tarefa com taskNumber reservado atomicamente no banco e gravar histórico CREATED")
    void testarCriarTaskSemDefinirAtributos() {
        UserEntity criador = new UserEntity();
        criador.setId(1L);
        criador.setNome("UnitTestServiceUser");
        
        OrganizationEntity orgMock = new OrganizationEntity();
        orgMock.setId(100L);
        orgMock.setLastTaskNumber(4);

        TaskRequestDTO requestDTO = new TaskRequestDTO(
                "Unit Test Service Titulo",
                "Unit Test Service Descrição",
                null,
                null,
                null,
                null,
                100L
        );

        when(organizationService.hasPermissionTo(criador, 100L, "CREATE_TASK")).thenReturn(true);
        when(organizationRepository.findById(100L)).thenReturn(Optional.of(orgMock));
        when(organizationRepository.nextTaskNumber(100L)).thenReturn(5);

        TaskEntity taskMock = new TaskEntity();
        taskMock.setId(10L);
        taskMock.setTaskNumber(5);
        taskMock.setTitulo("Unit Test Service Titulo");
        taskMock.setDescricao("Unit Test Service Descrição");
        taskMock.setStatus(TaskEntity.Status.pendente);
        taskMock.setPrioridade(TaskEntity.Prioridade.media);
        taskMock.setCriador(criador);
        taskMock.setOrganization(orgMock);
        taskMock.setResponsaveis(List.of());

        ArgumentCaptor<TaskEntity> taskCaptor = ArgumentCaptor.forClass(TaskEntity.class);
        when(taskRepository.save(taskCaptor.capture())).thenReturn(taskMock);

        TaskResponseDTO resultado = taskService.criarTask(requestDTO, criador);

        assertThat(resultado.titulo()).isEqualTo("Unit Test Service Titulo");
        assertThat(resultado.taskNumber()).isEqualTo(5);
        // O número vem do incremento atômico no banco; a organização não é regravada (evita sobrescrever configurações)
        assertThat(taskCaptor.getValue().getTaskNumber()).isEqualTo(5);
        verify(organizationRepository, times(1)).nextTaskNumber(100L);
        verify(organizationRepository, never()).save(any());
        assertThat(orgMock.getLastTaskNumber()).isEqualTo(4);

        ArgumentCaptor<TaskHistoryEntity> historyCaptor = ArgumentCaptor.forClass(TaskHistoryEntity.class);
        verify(taskHistoryRepository, times(1)).save(historyCaptor.capture());
        TaskHistoryEntity hist = historyCaptor.getValue();
        assertThat(hist.getActionType()).isEqualTo("CREATED");
        assertThat(hist.getTaskNumber()).isEqualTo(5);
        assertThat(hist.getNewValue()).isEqualTo("Unit Test Service Titulo");
        assertThat(hist.getOrganization()).isEqualTo(orgMock);
    }

    @Test
    @DisplayName("Deve bloquear edição de tarefa se tentar reabrir tarefa concluída sem permissão RETURN_TASK")
    void testarAtualizarTaskBloqueiaBypassReturnTask() {
        UserEntity member = new UserEntity();
        member.setId(2L);
        member.setNome("Member");

        OrganizationEntity org = buildOrgWithMember(10L, member, OrganizationRole.MEMBER);

        TaskEntity taskConcluida = new TaskEntity();
        taskConcluida.setId(50L);
        taskConcluida.setTitulo("Task Concluída");
        taskConcluida.setStatus(TaskEntity.Status.concluida);
        taskConcluida.setPrioridade(TaskEntity.Prioridade.media);
        taskConcluida.setOrganization(org);

        when(taskRepository.findTaskById(50L)).thenReturn(Optional.of(taskConcluida));
        when(organizationService.canEditTask(member, taskConcluida)).thenReturn(true);
        when(organizationService.canMoveTask(member, taskConcluida)).thenReturn(true);
        when(organizationService.hasPermissionTo(member, 10L, "RETURN_TASK")).thenReturn(false);

        TaskRequestDTO dto = new TaskRequestDTO("Task Concluída", "Desc", "pendente", "media", null, null, 10L);

        assertThrows(SecurityException.class, () -> taskService.atualizarTask(50L, dto, member));
    }

    @Test
    @DisplayName("Deve permitir limpar prazo de uma tarefa e registrar apenas CHANGED_DEADLINE quando somente o prazo mudar")
    void testarAtualizarTaskLimparPrazo() {
        UserEntity manager = new UserEntity();
        manager.setId(1L);
        manager.setNome("Manager");

        OrganizationEntity org = buildOrgWithMember(10L, manager, OrganizationRole.MANAGER);

        TaskEntity taskComPrazo = new TaskEntity();
        taskComPrazo.setId(50L);
        taskComPrazo.setTitulo("Task com Prazo");
        taskComPrazo.setDescricao("Desc");
        taskComPrazo.setStatus(TaskEntity.Status.pendente);
        taskComPrazo.setPrioridade(TaskEntity.Prioridade.alta);
        taskComPrazo.setPrazo(LocalDate.of(2026, 12, 31));
        taskComPrazo.setOrganization(org);
        taskComPrazo.setCriador(manager);
        taskComPrazo.setResponsaveis(List.of(manager));

        when(taskRepository.findTaskById(50L)).thenReturn(Optional.of(taskComPrazo));
        when(organizationService.hasPermissionTo(manager, 10L, "CHANGE_DEADLINE")).thenReturn(true);

        TaskRequestDTO dto = new TaskRequestDTO("Task com Prazo", "Desc", "pendente", "alta", "", null, 10L);

        TaskResponseDTO response = taskService.atualizarTask(50L, dto, manager);

        assertThat(response.prazo()).isNull();
        assertThat(response.idsResponsaveis()).containsExactly(1L);

        ArgumentCaptor<TaskHistoryEntity> histCaptor = ArgumentCaptor.forClass(TaskHistoryEntity.class);
        verify(taskHistoryRepository, times(1)).save(histCaptor.capture());
        assertThat(histCaptor.getValue().getActionType()).isEqualTo("CHANGED_DEADLINE");
        assertThat(histCaptor.getValue().getOldValue()).isEqualTo("2026-12-31");
        assertThat(histCaptor.getValue().getNewValue()).isEqualTo("Sem prazo");
    }

    @Test
    @DisplayName("Deve registrar eventos granulares no histórico ao alterar título, prioridade e descrição")
    void testarAtualizarTaskHistoricoGranular() {
        UserEntity owner = new UserEntity();
        owner.setId(1L);
        owner.setNome("Owner");

        OrganizationEntity org = buildOrgWithMember(10L, owner, OrganizationRole.OWNER);

        TaskEntity task = new TaskEntity();
        task.setId(77L);
        task.setTitulo("Título Antigo");
        task.setDescricao("Descrição Antiga");
        task.setStatus(TaskEntity.Status.pendente);
        task.setPrioridade(TaskEntity.Prioridade.baixa);
        task.setOrganization(org);
        task.setCriador(owner);
        task.setResponsaveis(List.of());

        when(taskRepository.findTaskById(77L)).thenReturn(Optional.of(task));

        TaskRequestDTO dto = new TaskRequestDTO(
                "Título Novo",
                "Descrição Nova",
                "pendente",
                "alta",
                null,
                null,
                10L
        );

        taskService.atualizarTask(77L, dto, owner);

        ArgumentCaptor<TaskHistoryEntity> captor = ArgumentCaptor.forClass(TaskHistoryEntity.class);
        verify(taskHistoryRepository, times(3)).save(captor.capture());

        List<String> actions = captor.getAllValues().stream()
                .map(TaskHistoryEntity::getActionType)
                .toList();
        assertThat(actions).containsExactlyInAnyOrder("EDITED_TITLE", "EDITED_DESCRIPTION", "CHANGED_PRIORITY");
    }

    @Test
    @DisplayName("Deve registrar DELETED com taskNumber no histórico antes de excluir a tarefa")
    void testarDeletarTaskRegistraHistoricoDeleted() {
        UserEntity owner = new UserEntity();
        owner.setId(1L);
        owner.setNome("Owner");

        OrganizationEntity org = buildOrgWithMember(10L, owner, OrganizationRole.OWNER);

        TaskEntity task = new TaskEntity();
        task.setId(99L);
        task.setTaskNumber(7);
        task.setTitulo("Tarefa a Excluir");
        task.setOrganization(org);

        when(taskRepository.findTaskById(99L)).thenReturn(Optional.of(task));
        when(organizationService.canDeleteTask(owner, task)).thenReturn(true);

        taskService.deletarTask(99L, owner);

        ArgumentCaptor<TaskHistoryEntity> captor = ArgumentCaptor.forClass(TaskHistoryEntity.class);
        verify(taskHistoryRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo("DELETED");
        assertThat(captor.getValue().getTaskNumber()).isEqualTo(7);
        assertThat(captor.getValue().getOldValue()).isEqualTo("Tarefa a Excluir");
        assertThat(captor.getValue().getTaskTitle()).isEqualTo("Tarefa a Excluir");
        verify(taskRepository, times(1)).deleteTask(99L);
    }

    @Test
    @DisplayName("Deve buscar histórico de tarefas excluídas para MANAGER aplicando retenção, sem expurgar, e bloquear para MEMBER")
    void testarGetDeletedTasksHistoryComRetencaoERbac() {
        UserEntity manager = new UserEntity();
        manager.setId(1L);
        manager.setNome("Gerente");

        UserEntity member = new UserEntity();
        member.setId(2L);
        member.setNome("Membro");

        OrganizationEntity org = new OrganizationEntity();
        org.setId(10L);
        org.setDeletedTasksRetentionDays(7);

        OrganizationMemberEntity m1 = new OrganizationMemberEntity();
        m1.setOrganization(org);
        m1.setUser(manager);
        m1.setRole(OrganizationRole.MANAGER);

        OrganizationMemberEntity m2 = new OrganizationMemberEntity();
        m2.setOrganization(org);
        m2.setUser(member);
        m2.setRole(OrganizationRole.MEMBER);

        org.setMembers(List.of(m1, m2));
        registrar(org);

        TaskHistoryEntity deletedItem = TaskHistoryEntity.builder()
                .id(501L)
                .organization(org)
                .taskNumber(4)
                .taskTitle("Task Excluída #4")
                .user(manager)
                .actionType("DELETED")
                .oldValue("Task Excluída #4")
                .timestamp(LocalDateTime.now())
                .build();

        when(organizationRepository.findById(10L)).thenReturn(Optional.of(org));
        when(taskHistoryRepository.findDeletedByOrganizationIdSinceOrderByTimestampDesc(eq(10L), any(LocalDateTime.class)))
                .thenReturn(List.of(deletedItem));

        // MEMBER deve ser bloqueado
        assertThrows(SecurityException.class, () -> taskService.getDeletedTasksHistory(10L, member));

        // MANAGER tem acesso; a consulta filtra pela retenção e não expurga (o expurgo é do job agendado)
        List<TaskHistoryResponseDTO> result = taskService.getDeletedTasksHistory(10L, manager);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).taskNumber()).isEqualTo(4);
        assertThat(result.get(0).taskTitle()).isEqualTo("Task Excluída #4");
        verify(taskHistoryRepository, never()).deleteExpiredDeletedHistory(anyLong(), any());
    }

    @Test
    @DisplayName("Não deve reservar taskNumber quando a criação falha na validação")
    void testarCriarTaskInvalidaNaoReservaNumero() {
        UserEntity criador = new UserEntity();
        criador.setId(1L);
        OrganizationEntity org = new OrganizationEntity();
        org.setId(100L);
        org.setMembers(List.of());

        TaskRequestDTO comResponsavelInexistente = new TaskRequestDTO(
                "Titulo", null, null, null, null, List.of(77L), 100L);

        when(organizationService.hasPermissionTo(criador, 100L, "CREATE_TASK")).thenReturn(true);
        when(organizationRepository.findById(100L)).thenReturn(Optional.of(org));
        when(userRepository.userFindAllByIds(List.of(77L))).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> taskService.criarTask(comResponsavelInexistente, criador));

        when(organizationService.hasPermissionTo(criador, 100L, "CREATE_TASK")).thenReturn(false);
        assertThrows(SecurityException.class, () -> taskService.criarTask(comResponsavelInexistente, criador));

        verify(organizationRepository, never()).nextTaskNumber(anyLong());
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve validar título obrigatório e limite de 255 caracteres antes de reservar taskNumber")
    void testarCriarTaskValidaTituloAntesDeReservarNumero() {
        UserEntity criador = usuario(1L);
        OrganizationEntity org = new OrganizationEntity();
        org.setId(100L);

        when(organizationService.hasPermissionTo(criador, 100L, "CREATE_TASK")).thenReturn(true);
        when(organizationRepository.findById(100L)).thenReturn(Optional.of(org));

        TaskRequestDTO tituloVazio = new TaskRequestDTO("   ", null, null, null, null, null, 100L);
        assertThrows(BusinessException.class, () -> taskService.criarTask(tituloVazio, criador));

        TaskRequestDTO tituloLongo = new TaskRequestDTO("a".repeat(256), null, null, null, null, null, 100L);
        assertThrows(BusinessException.class, () -> taskService.criarTask(tituloLongo, criador));

        verify(organizationRepository, never()).nextTaskNumber(anyLong());
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando nextTaskNumber retornar null (organização removida em paralelo)")
    void testarCriarTaskQuandoNextTaskNumberRetornaNull() {
        UserEntity criador = usuario(1L);
        OrganizationEntity org = new OrganizationEntity();
        org.setId(100L);

        when(organizationService.hasPermissionTo(criador, 100L, "CREATE_TASK")).thenReturn(true);
        when(organizationRepository.findById(100L)).thenReturn(Optional.of(org));
        when(organizationRepository.nextTaskNumber(100L)).thenReturn(null);

        TaskRequestDTO req = new TaskRequestDTO("Titulo válido", null, null, null, null, null, 100L);
        assertThrows(ResourceNotFoundException.class, () -> taskService.criarTask(req, criador));
        verify(taskRepository, never()).save(any());
    }

    // --- Histórico da organização (RNE-006: excluídas só para cargos acima de MEMBER) ---

    private OrganizationEntity orgComMembro(Long orgId, Integer retentionDays, UserEntity user, OrganizationRole role) {
        OrganizationEntity org = new OrganizationEntity();
        org.setId(orgId);
        org.setDeletedTasksRetentionDays(retentionDays);
        OrganizationMemberEntity membro = new OrganizationMemberEntity();
        membro.setOrganization(org);
        membro.setUser(user);
        membro.setRole(role);
        org.setMembers(List.of(membro));
        return registrar(org);
    }

    private UserEntity usuario(Long id) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setNome("Usuário " + id);
        return u;
    }

    @Test
    @DisplayName("MEMBER deve receber no histórico da organização apenas eventos de tarefas existentes")
    void testarHistoricoOrganizacaoMemberSemTarefasExcluidas() {
        UserEntity member = usuario(2L);
        OrganizationEntity org = orgComMembro(10L, 7, member, OrganizationRole.MEMBER);
        TaskEntity existente = new TaskEntity();
        existente.setId(100L);
        existente.setTitulo("Tarefa ativa");
        TaskHistoryEntity evento = TaskHistoryEntity.builder()
                .id(1L).organization(org).task(existente).taskTitle("Tarefa ativa")
                .user(member).actionType("CREATED").timestamp(LocalDateTime.now()).build();

        when(organizationRepository.findById(10L)).thenReturn(Optional.of(org));
        when(taskHistoryRepository.findExistingTasksByOrganizationIdOrderByTimestampDesc(10L)).thenReturn(List.of(evento));

        List<TaskHistoryResponseDTO> result = taskService.getOrganizationHistory(10L, member);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).taskId()).isEqualTo(100L);
        verify(taskHistoryRepository, never()).findByOrganizationIdOrderByTimestampDesc(anyLong());
        verify(taskHistoryRepository, never()).findByOrganizationIdWithinDeletedRetentionOrderByTimestampDesc(anyLong(), any());
    }

    @Test
    @DisplayName("Cargos acima de MEMBER devem receber o histórico completo respeitando a retenção de excluídas")
    void testarHistoricoOrganizacaoGestorComRetencao() {
        for (OrganizationRole role : List.of(OrganizationRole.OWNER, OrganizationRole.SUB_OWNER, OrganizationRole.MANAGER)) {
            reset(organizationRepository, taskHistoryRepository);
            UserEntity gestor = usuario(1L);
            OrganizationEntity org = orgComMembro(10L, 7, gestor, role);
            TaskHistoryEntity excluida = TaskHistoryEntity.builder()
                    .id(2L).organization(org).taskTitle("Tarefa excluída")
                    .user(gestor).actionType("DELETED").timestamp(LocalDateTime.now()).build();

            when(organizationRepository.findById(10L)).thenReturn(Optional.of(org));
            when(taskHistoryRepository.findByOrganizationIdWithinDeletedRetentionOrderByTimestampDesc(eq(10L), any(LocalDateTime.class)))
                    .thenReturn(List.of(excluida));

            List<TaskHistoryResponseDTO> result = taskService.getOrganizationHistory(10L, gestor);

            assertThat(result).as(role.name()).hasSize(1);
            assertThat(result.get(0).actionType()).isEqualTo("DELETED");
            ArgumentCaptor<LocalDateTime> corte = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(taskHistoryRepository).findByOrganizationIdWithinDeletedRetentionOrderByTimestampDesc(eq(10L), corte.capture());
            assertThat(corte.getValue()).isBetween(LocalDateTime.now().minusDays(7).minusMinutes(1), LocalDateTime.now().minusDays(7).plusMinutes(1));
            verify(taskHistoryRepository, never()).findExistingTasksByOrganizationIdOrderByTimestampDesc(anyLong());
            // A consulta do histórico não expurga registros (expurgo é responsabilidade de outro fluxo)
            verify(taskHistoryRepository, never()).deleteExpiredDeletedHistory(anyLong(), any());
        }
    }

    @Test
    @DisplayName("OWNER sem retenção configurada deve receber o histórico completo")
    void testarHistoricoOrganizacaoOwnerSemRetencao() {
        UserEntity owner = usuario(1L);
        OrganizationEntity org = orgComMembro(10L, null, owner, OrganizationRole.OWNER);

        when(organizationRepository.findById(10L)).thenReturn(Optional.of(org));
        when(taskHistoryRepository.findByOrganizationIdOrderByTimestampDesc(10L)).thenReturn(List.of());

        taskService.getOrganizationHistory(10L, owner);

        verify(taskHistoryRepository).findByOrganizationIdOrderByTimestampDesc(10L);
        verify(taskHistoryRepository, never()).findByOrganizationIdWithinDeletedRetentionOrderByTimestampDesc(anyLong(), any());
    }

    @Test
    @DisplayName("Usuário que não pertence à organização não deve acessar o histórico")
    void testarHistoricoOrganizacaoNaoMembroNegado() {
        OrganizationEntity org = orgComMembro(10L, 7, usuario(1L), OrganizationRole.OWNER);
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(org));

        assertThrows(SecurityException.class, () -> taskService.getOrganizationHistory(10L, usuario(99L)));
        verifyNoInteractions(taskHistoryRepository);
    }

    @Test
    @DisplayName("Deve bloquear movimentação de status no Kanban por usuário que não pertence à organização")
    void testarAtualizarStatusTaskUsuarioNaoMembro() {
        UserEntity estranho = new UserEntity();
        estranho.setId(99L);

        UserEntity dono = new UserEntity();
        dono.setId(1L);
        OrganizationEntity org = buildOrgWithMember(10L, dono, OrganizationRole.OWNER);

        TaskEntity task = new TaskEntity();
        task.setId(5L);
        task.setStatus(TaskEntity.Status.pendente);
        task.setOrganization(org);

        when(taskRepository.findTaskById(5L)).thenReturn(Optional.of(task));

        assertThrows(SecurityException.class, () -> taskService.atualizarStatusTask(5L, "em_andamento", estranho));
        verify(taskRepository, never()).updateTaskStatus(anyLong(), any());
    }

    @Test
    @DisplayName("Deve bloquear edição de tarefa por MEMBER quando canEditTask retornar false")
    void testarAtualizarTaskBloqueiaMemberSemPermissaoEdicao() {
        UserEntity member = new UserEntity();
        member.setId(2L);
        member.setNome("Member");

        OrganizationEntity org = buildOrgWithMember(10L, member, OrganizationRole.MEMBER);

        TaskEntity taskAlheia = new TaskEntity();
        taskAlheia.setId(60L);
        taskAlheia.setTitulo("Task de Outro");
        taskAlheia.setDescricao("Desc");
        taskAlheia.setStatus(TaskEntity.Status.pendente);
        taskAlheia.setPrioridade(TaskEntity.Prioridade.media);
        taskAlheia.setOrganization(org);

        when(taskRepository.findTaskById(60L)).thenReturn(Optional.of(taskAlheia));
        when(organizationService.canEditTask(member, taskAlheia)).thenReturn(false);

        TaskRequestDTO dto = new TaskRequestDTO("Tentando Editar", "Desc", "pendente", "media", null, null, 10L);

        assertThrows(SecurityException.class, () -> taskService.atualizarTask(60L, dto, member));
    }

    @Test
    @DisplayName("Deve bloquear movimentação no Kanban por MEMBER quando canMoveTask retornar false e permitir quando true")
    void testarAtualizarStatusTaskRespeitaCanMoveTaskParaMember() {
        UserEntity member = new UserEntity();
        member.setId(2L);
        member.setNome("Member");

        OrganizationEntity org = buildOrgWithMember(10L, member, OrganizationRole.MEMBER);

        TaskEntity task = new TaskEntity();
        task.setId(61L);
        task.setTaskNumber(2);
        task.setTitulo("Task Kanban");
        task.setStatus(TaskEntity.Status.pendente);
        task.setOrganization(org);

        when(taskRepository.findTaskById(61L)).thenReturn(Optional.of(task));

        // 1. Bloqueado quando canMoveTask = false
        when(organizationService.canMoveTask(member, task)).thenReturn(false);
        assertThrows(SecurityException.class, () -> taskService.atualizarStatusTask(61L, "em_andamento", member));
        verify(taskRepository, never()).updateTaskStatus(anyLong(), any());

        // 2. Permitido quando canMoveTask = true
        when(organizationService.canMoveTask(member, task)).thenReturn(true);
        taskService.atualizarStatusTask(61L, "em_andamento", member);
        verify(taskRepository, times(1)).updateTaskStatus(61L, TaskEntity.Status.em_andamento);
    }
}