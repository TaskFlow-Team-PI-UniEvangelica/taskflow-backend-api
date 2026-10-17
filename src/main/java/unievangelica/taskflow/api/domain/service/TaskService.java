package unievangelica.taskflow.api.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationEntity;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationMemberEntity;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationRole;
import unievangelica.taskflow.api.domain.persistence.entities.TaskEntity;
import unievangelica.taskflow.api.domain.persistence.entities.TaskHistoryEntity;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.TaskHistoryRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.TaskRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.dto.request.TaskRequestDTO;
import unievangelica.taskflow.api.dto.response.TaskResponseDTO;
import unievangelica.taskflow.api.dto.response.TaskHistoryResponseDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final OrganizationService organizationService;
    private final OrganizationRepository organizationRepository;
    private final TaskHistoryRepository taskHistoryRepository;

    public TaskService(TaskRepository taskRepository, 
                       UserRepository userRepository,
                       OrganizationService organizationService,
                       OrganizationRepository organizationRepository,
                       TaskHistoryRepository taskHistoryRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.organizationService = organizationService;
        this.organizationRepository = organizationRepository;
        this.taskHistoryRepository = taskHistoryRepository;
    }

    public List<TaskResponseDTO> listarTasks(Long organizationId, UserEntity userLogado){
        if (organizationId != null) {
            OrganizationEntity organization = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));
            if (userLogado != null && userLogado.getId() != null) {
                boolean isMember = organization.getMembers().stream()
                        .anyMatch(m -> m.getUser().getId().equals(userLogado.getId()));
                if (!isMember) {
                    throw new SecurityException("Você não pertence a esta organização.");
                }
            }
            List<TaskEntity> tasksDB = taskRepository.listTasksByOrganization(organizationId);
            return tasksDB.stream()
                    .map(this::converterParaDTO)
                    .collect(Collectors.toList());
        }

        if (userLogado != null && userLogado.getId() != null) {
            return taskRepository.listTasksByUserOrganizations(userLogado.getId()).stream()
                    .map(this::converterParaDTO)
                    .collect(Collectors.toList());
        }
        
        List<TaskEntity> tasksDB = taskRepository.listAllTasks();
        return tasksDB.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponseDTO criarTask(TaskRequestDTO data, UserEntity criadorLogado) {
        Long orgId = data.organizationId();
        if (orgId == null) {
            throw new IllegalArgumentException("O ID da organização é obrigatório para criar uma task.");
        }

        if (!organizationService.hasPermissionTo(criadorLogado, orgId, "CREATE_TASK")) {
            throw new SecurityException("Você não tem permissão para criar tasks nesta organização.");
        }

        OrganizationEntity organization = organizationRepository.findById(orgId)
            .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        int nextTaskNumber = organization.getLastTaskNumber() + 1;
        organization.setLastTaskNumber(nextTaskNumber);
        organizationRepository.save(organization);

        TaskEntity novaTarefa = new TaskEntity();
        novaTarefa.setTaskNumber(nextTaskNumber);
        novaTarefa.setTitulo(data.titulo());
        novaTarefa.setDescricao(data.descricao());
        novaTarefa.setCriador(criadorLogado);
        novaTarefa.setOrganization(organization);

        if (data.prazo() != null && !data.prazo().trim().isEmpty()) {
            novaTarefa.setPrazo(LocalDate.parse(data.prazo().trim()));
        }

        novaTarefa.setPrioridade(data.prioridade() != null && !data.prioridade().trim().isEmpty() ?
                TaskEntity.Prioridade.valueOf(data.prioridade().toLowerCase()) :
                TaskEntity.Prioridade.media);

        novaTarefa.setStatus(TaskEntity.Status.pendente);

        if (data.idsResponsaveis() != null && !data.idsResponsaveis().isEmpty()) {
            Set<Long> uniqueIds = new HashSet<>(data.idsResponsaveis());
            List<UserEntity> responsaveis = userRepository.userFindAllByIds(data.idsResponsaveis());
            if (responsaveis.size() != uniqueIds.size()) {
                throw new IllegalArgumentException("Um ou mais usuários informados como responsáveis não existem.");
            }
            
            boolean allAreMembers = responsaveis.stream().allMatch(u -> 
                organization.getMembers().stream().anyMatch(m -> m.getUser().getId().equals(u.getId()))
            );
            if (!allAreMembers) {
                throw new SecurityException("Um ou mais usuários não pertencem à organização.");
            }
            
            novaTarefa.setResponsaveis(responsaveis);
        }

        TaskEntity taskLocal = taskRepository.save(novaTarefa);
        if (taskLocal.getTaskNumber() == null) {
            taskLocal.setTaskNumber(nextTaskNumber);
        }

        salvarHistorico(taskLocal, criadorLogado, "CREATED", null, taskLocal.getTitulo());

        return converterParaDTO(taskLocal);
    }

    private boolean isAboveMemberInTaskOrg(UserEntity user, TaskEntity task) {
        if (user == null || task == null || task.getOrganization() == null || task.getOrganization().getMembers() == null) {
            return false;
        }
        return task.getOrganization().getMembers().stream()
                .filter(m -> m.getUser() != null && Objects.equals(m.getUser().getId(), user.getId()))
                .anyMatch(m -> m.getRole() == OrganizationRole.OWNER
                        || m.getRole() == OrganizationRole.SUB_OWNER
                        || m.getRole() == OrganizationRole.MANAGER);
    }

    @Transactional
    public TaskResponseDTO atualizarTask(Long id, TaskRequestDTO data, UserEntity userLogado) {
        TaskEntity taskLocal = taskRepository.findTaskById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada com ID: " + id));
        
        Long orgId = taskLocal.getOrganization().getId();

        boolean isMember = taskLocal.getOrganization().getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userLogado.getId()));
        if (!isMember) {
            throw new SecurityException("Você não pertence à organização desta tarefa.");
        }

        // Capturar estado anterior para diff de auditoria
        String oldTitle = taskLocal.getTitulo();
        String oldDescription = taskLocal.getDescricao();
        TaskEntity.Prioridade oldPriority = taskLocal.getPrioridade();
        LocalDate oldPrazo = taskLocal.getPrazo();
        TaskEntity.Status oldStatus = taskLocal.getStatus();
        List<UserEntity> oldResponsaveis = taskLocal.getResponsaveis() != null
                ? List.copyOf(taskLocal.getResponsaveis())
                : List.of();

        String currentPrazoStr = oldPrazo != null ? oldPrazo.toString() : "";
        String incomingPrazoStr = data.prazo() != null ? data.prazo().trim() : currentPrazoStr;
        boolean deadlineChanged = !incomingPrazoStr.equals(currentPrazoStr);
        boolean titleChanged = data.titulo() != null && !Objects.equals(oldTitle, data.titulo());
        boolean descriptionChanged = data.descricao() != null
                && !Objects.equals(oldDescription != null ? oldDescription : "", data.descricao());
        boolean priorityChanged = data.prioridade() != null
                && !data.prioridade().trim().isEmpty()
                && oldPriority != TaskEntity.Prioridade.valueOf(data.prioridade().toLowerCase());
        boolean assigneesChanged = false;
        if (data.idsResponsaveis() != null) {
            Set<Long> incomingIds = new HashSet<>(data.idsResponsaveis());
            Set<Long> currentIds = oldResponsaveis.stream().map(UserEntity::getId).collect(Collectors.toSet());
            assigneesChanged = !currentIds.equals(incomingIds);
        }
        boolean statusChanged = data.status() != null
                && !data.status().trim().isEmpty()
                && oldStatus != TaskEntity.Status.valueOf(data.status().toLowerCase());

        boolean contentChanged = deadlineChanged || titleChanged || descriptionChanged || priorityChanged || assigneesChanged;

        // Validar permissão de edição de conteúdo (ou submissão sem alteração de status)
        if (contentChanged || !statusChanged) {
            boolean canEdit = isAboveMemberInTaskOrg(userLogado, taskLocal)
                    || organizationService.canEditTask(userLogado, taskLocal);
            if (!canEdit) {
                throw new SecurityException("Você não tem permissão para alterar esta tarefa.");
            }
        }

        // 1. Prazo (com validação de permissão CHANGE_DEADLINE)
        if (deadlineChanged) {
            if (!organizationService.hasPermissionTo(userLogado, orgId, "CHANGE_DEADLINE")) {
                throw new SecurityException("Você não tem permissão para alterar o prazo de tasks.");
            }
            LocalDate novoPrazo = incomingPrazoStr.isEmpty() ? null : LocalDate.parse(incomingPrazoStr);
            taskLocal.setPrazo(novoPrazo);
            salvarHistorico(
                taskLocal,
                userLogado,
                "CHANGED_DEADLINE",
                oldPrazo != null ? oldPrazo.toString() : "Sem prazo",
                novoPrazo != null ? novoPrazo.toString() : "Sem prazo"
            );
        }

        // 2. Status (com validação de permissão de movimentação e RETURN_TASK)
        if (data.status() != null && !data.status().trim().isEmpty()) {
            TaskEntity.Status novoStatus = TaskEntity.Status.valueOf(data.status().toLowerCase());
            if (oldStatus != novoStatus) {
                boolean canMove = isAboveMemberInTaskOrg(userLogado, taskLocal)
                        || organizationService.canMoveTask(userLogado, taskLocal);
                if (!canMove) {
                    throw new SecurityException("Você não tem permissão para mover o status desta tarefa.");
                }
            }
            if (oldStatus == TaskEntity.Status.concluida && novoStatus != TaskEntity.Status.concluida) {
                if (!organizationService.hasPermissionTo(userLogado, orgId, "RETURN_TASK")) {
                    throw new SecurityException("Você não tem permissão para retornar tasks concluídas.");
                }
                salvarHistorico(taskLocal, userLogado, "RETURNED", oldStatus.name(), novoStatus.name());
            } else if (oldStatus != novoStatus) {
                salvarHistorico(taskLocal, userLogado, "MOVED", oldStatus.name(), novoStatus.name());
            }
            taskLocal.setStatus(novoStatus);
        }

        // 3. Responsáveis (com validação de existência e pertencimento à organização)
        if (data.idsResponsaveis() != null) {
            Set<Long> uniqueIds = new HashSet<>(data.idsResponsaveis());
            List<UserEntity> novosResponsaveis = data.idsResponsaveis().isEmpty()
                    ? List.of()
                    : userRepository.userFindAllByIds(data.idsResponsaveis());
            if (novosResponsaveis.size() != uniqueIds.size()) {
                throw new IllegalArgumentException("Um ou mais usuários informados como responsáveis não existem.");
            }
            
            boolean allAreMembers = novosResponsaveis.stream().allMatch(u -> 
                taskLocal.getOrganization().getMembers().stream().anyMatch(m -> m.getUser().getId().equals(u.getId()))
            );
            if (!allAreMembers) {
                throw new SecurityException("Um ou mais usuários não pertencem à organização.");
            }

            Set<Long> oldIds = oldResponsaveis.stream()
                    .map(UserEntity::getId)
                    .collect(Collectors.toSet());
            taskLocal.setResponsaveis(novosResponsaveis);

            if (!oldIds.equals(uniqueIds)) {
                String oldNames = oldResponsaveis.isEmpty()
                        ? "Nenhum"
                        : oldResponsaveis.stream()
                            .map(UserEntity::getNome)
                            .sorted()
                            .collect(Collectors.joining(", "));
                String newNames = novosResponsaveis.isEmpty()
                        ? "Nenhum"
                        : novosResponsaveis.stream()
                            .map(UserEntity::getNome)
                            .sorted()
                            .collect(Collectors.joining(", "));
                salvarHistorico(taskLocal, userLogado, "CHANGED_ASSIGNEES", oldNames, newNames);
            }
        }

        // 4. Título
        if (data.titulo() != null && !Objects.equals(oldTitle, data.titulo())) {
            taskLocal.setTitulo(data.titulo());
            salvarHistorico(taskLocal, userLogado, "EDITED_TITLE", oldTitle, data.titulo());
        }

        // 5. Descrição
        if (data.descricao() != null && !Objects.equals(oldDescription != null ? oldDescription : "", data.descricao())) {
            taskLocal.setDescricao(data.descricao());
            String oldDescDisplay = (oldDescription == null || oldDescription.isBlank()) ? "Sem descrição" : oldDescription;
            String newDescDisplay = data.descricao().isBlank() ? "Sem descrição" : data.descricao();
            salvarHistorico(taskLocal, userLogado, "EDITED_DESCRIPTION", oldDescDisplay, newDescDisplay);
        }

        // 6. Prioridade
        if (data.prioridade() != null && !data.prioridade().trim().isEmpty()) {
            TaskEntity.Prioridade novaPrioridade = TaskEntity.Prioridade.valueOf(data.prioridade().toLowerCase());
            if (oldPriority != novaPrioridade) {
                taskLocal.setPrioridade(novaPrioridade);
                salvarHistorico(
                    taskLocal,
                    userLogado,
                    "CHANGED_PRIORITY",
                    oldPriority != null ? oldPriority.name() : null,
                    novaPrioridade.name()
                );
            }
        }

        return converterParaDTO(taskLocal);
    }

    @Transactional
    public void atualizarStatusTask(Long id, String novoStatus, UserEntity userLogado) {
        TaskEntity taskLocal = taskRepository.findTaskById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada"));

        boolean isMember = taskLocal.getOrganization().getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userLogado.getId()));
        if (!isMember) {
            throw new SecurityException("Você não pertence à organização desta tarefa.");
        }

        TaskEntity.Status statusEnum = TaskEntity.Status.valueOf(novoStatus.toLowerCase());
        if (taskLocal.getStatus() == statusEnum) {
            return;
        }

        boolean canMove = isAboveMemberInTaskOrg(userLogado, taskLocal)
                || organizationService.canMoveTask(userLogado, taskLocal);
        if (!canMove) {
            throw new SecurityException("Você não tem permissão para mover o status desta tarefa.");
        }
        
        // Se estiver retornando para um status anterior (ex: concluida -> pendente)
        if (taskLocal.getStatus() == TaskEntity.Status.concluida && statusEnum != TaskEntity.Status.concluida) {
            if (!organizationService.hasPermissionTo(userLogado, taskLocal.getOrganization().getId(), "RETURN_TASK")) {
                throw new SecurityException("Você não tem permissão para retornar tasks concluídas.");
            }
            salvarHistorico(taskLocal, userLogado, "RETURNED", taskLocal.getStatus().name(), statusEnum.name());
        } else {
            salvarHistorico(taskLocal, userLogado, "MOVED", taskLocal.getStatus().name(), statusEnum.name());
        }

        taskRepository.updateTaskStatus(id, statusEnum);
    }

    @Transactional
    public void deletarTask(Long id, UserEntity userLogado) {
        TaskEntity taskLocal = taskRepository.findTaskById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada"));

        Long orgId = taskLocal.getOrganization().getId();
        boolean allowed = organizationService.canDeleteTask(userLogado, taskLocal)
                || organizationService.hasPermissionTo(userLogado, orgId, "DELETE_TASK");

        if (!allowed) {
            throw new SecurityException("Você não tem permissão para deletar esta task nesta organização.");
        }

        salvarHistorico(taskLocal, userLogado, "DELETED", taskLocal.getTitulo(), null);
        taskRepository.deleteTask(id);
    }

    private void salvarHistorico(TaskEntity task, UserEntity user, String action, String oldVal, String newVal) {
        TaskHistoryEntity historico = TaskHistoryEntity.builder()
            .task(task)
            .organization(task != null ? task.getOrganization() : null)
            .taskNumber(task != null ? task.getTaskNumber() : null)
            .taskTitle(task != null ? task.getTitulo() : null)
            .user(user)
            .actionType(action)
            .oldValue(oldVal)
            .newValue(newVal)
            .build();
        taskHistoryRepository.save(historico);
    }

    private TaskResponseDTO converterParaDTO(TaskEntity entidade){
        List<UserEntity> responsaveis = entidade.getResponsaveis() != null ? entidade.getResponsaveis() : List.of();
        List<Long> idsResponsaveis = responsaveis.stream()
                .map(UserEntity::getId)
                .collect(Collectors.toList());
        List<String> nomesResponsaveis = responsaveis.stream()
                .map(UserEntity::getNome)
                .collect(Collectors.toList());

        Long criadorId = entidade.getCriador() != null ? entidade.getCriador().getId() : null;
        String nomeCriador = entidade.getCriador() != null ? entidade.getCriador().getNome() : "Usuário Removido";
        Integer taskNumber = entidade.getTaskNumber() != null ? entidade.getTaskNumber() : 1;

        return new TaskResponseDTO(
                entidade.getId(),
                taskNumber,
                entidade.getTitulo(),
                entidade.getDescricao(),
                entidade.getStatus().name(),
                entidade.getPrioridade().name(),
                entidade.getPrazo(),
                criadorId,
                nomeCriador,
                idsResponsaveis,
                nomesResponsaveis
        );
    }

    private TaskHistoryResponseDTO converterHistoricoParaDTO(TaskHistoryEntity h) {
        Long historyTaskId = h.getTask() != null ? h.getTask().getId() : null;
        Integer historyTaskNumber = h.getTaskNumber() != null
                ? h.getTaskNumber()
                : (h.getTask() != null ? h.getTask().getTaskNumber() : null);
        String historyTaskTitle = h.getTaskTitle() != null
                ? h.getTaskTitle()
                : (h.getTask() != null ? h.getTask().getTitulo() : "Tarefa Excluída");
        String userName = h.getUser() != null ? h.getUser().getNome() : "Usuário Removido";

        return new TaskHistoryResponseDTO(
                h.getId(),
                historyTaskId,
                historyTaskNumber,
                historyTaskTitle,
                h.getActionType(),
                h.getOldValue(),
                h.getNewValue(),
                userName,
                h.getTimestamp()
        );
    }

    public List<TaskHistoryResponseDTO> getTaskHistory(Long taskId, UserEntity userLogado) {
        TaskEntity taskLocal = taskRepository.findTaskById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada"));

        boolean isMember = taskLocal.getOrganization().getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userLogado.getId()));
        if (!isMember) {
            throw new SecurityException("Você não tem acesso a esta organização.");
        }

        return taskHistoryRepository.findByTaskIdOrderByTimestampDesc(taskId).stream()
            .map(this::converterHistoricoParaDTO)
            .collect(Collectors.toList());
    }

    public List<TaskHistoryResponseDTO> getOrganizationHistory(Long organizationId, UserEntity userLogado) {
        OrganizationEntity organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada"));

        boolean isMember = organization.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userLogado.getId()));
        if (!isMember) {
            throw new SecurityException("Você não tem acesso a esta organização.");
        }

        return taskHistoryRepository.findByOrganizationIdOrderByTimestampDesc(organizationId).stream()
            .map(this::converterHistoricoParaDTO)
            .collect(Collectors.toList());
    }

    /**
     * Retorna o histórico de tarefas excluídas da organização.
     * Visível apenas para cargos acima de MEMBER (OWNER, SUB_OWNER, MANAGER).
     * Aplica automaticamente a política de retenção em dias configurada pelo OWNER.
     */
    @Transactional
    public List<TaskHistoryResponseDTO> getDeletedTasksHistory(Long organizationId, UserEntity userLogado) {
        OrganizationEntity organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada"));

        OrganizationMemberEntity membership = organization.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(userLogado.getId()))
                .findFirst()
                .orElseThrow(() -> new SecurityException("Você não pertence a esta organização."));

        if (membership.getRole() == OrganizationRole.MEMBER) {
            throw new SecurityException("Apenas cargos acima de Membro (Gerente, Sub-Dono e Dono) podem visualizar o histórico de tarefas excluídas.");
        }

        Integer retentionDays = organization.getDeletedTasksRetentionDays();
        if (retentionDays != null && retentionDays > 0) {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
            taskHistoryRepository.deleteExpiredDeletedHistory(organizationId, cutoff);
            return taskHistoryRepository.findDeletedByOrganizationIdSinceOrderByTimestampDesc(organizationId, cutoff)
                    .stream()
                    .map(this::converterHistoricoParaDTO)
                    .collect(Collectors.toList());
        }

        return taskHistoryRepository.findDeletedByOrganizationIdOrderByTimestampDesc(organizationId)
                .stream()
                .map(this::converterHistoricoParaDTO)
                .collect(Collectors.toList());
    }
}
