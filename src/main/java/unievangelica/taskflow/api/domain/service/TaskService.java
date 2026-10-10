package unievangelica.taskflow.api.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationEntity;
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
import java.util.List;
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
            // Verificar permissões/existência se necessário (ou se é membro)
            List<TaskEntity> tasksDB = taskRepository.listTasksByOrganization(organizationId);
            return tasksDB.stream()
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

        TaskEntity novaTarefa = new TaskEntity();
        novaTarefa.setTitulo(data.titulo());
        novaTarefa.setDescricao(data.descricao());
        novaTarefa.setCriador(criadorLogado);
        novaTarefa.setOrganization(organization);

        if (data.prazo() != null) {
            novaTarefa.setPrazo(LocalDate.parse(data.prazo()));
        }

        novaTarefa.setPrioridade(data.prioridade() != null ?
                TaskEntity.Prioridade.valueOf(data.prioridade()) :
                TaskEntity.Prioridade.media);

        novaTarefa.setStatus(TaskEntity.Status.pendente);

        if (data.idsResponsaveis() != null && !data.idsResponsaveis().isEmpty()) {
            List<UserEntity> responsaveis = userRepository.userFindAllByIds(data.idsResponsaveis());
            
            boolean allAreMembers = responsaveis.stream().allMatch(u -> 
                organization.getMembers().stream().anyMatch(m -> m.getUser().getId().equals(u.getId()))
            );
            if (!allAreMembers) {
                throw new SecurityException("Um ou mais usuários não pertencem à organização.");
            }
            
            novaTarefa.setResponsaveis(responsaveis);
        }

        TaskEntity taskLocal = taskRepository.save(novaTarefa);

        salvarHistorico(taskLocal, criadorLogado, "CREATED", null, taskLocal.getTitulo());

        return converterParaDTO(taskLocal);
    }

    @Transactional
    public TaskResponseDTO atualizarTask(Long id, TaskRequestDTO data, UserEntity userLogado) {
        TaskEntity taskLocal = taskRepository.findTaskById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada com ID: " + id));
        
        Long orgId = taskLocal.getOrganization().getId();

        if (data.prazo() != null && !data.prazo().equals(taskLocal.getPrazo() != null ? taskLocal.getPrazo().toString() : "")) {
            if (!organizationService.hasPermissionTo(userLogado, orgId, "CHANGE_DEADLINE")) {
                throw new SecurityException("Você não tem permissão para alterar o prazo de tasks.");
            }
        }

        String oldTitle = taskLocal.getTitulo();

        taskLocal.setTitulo(data.titulo() != null ? data.titulo() : taskLocal.getTitulo());
        taskLocal.setDescricao(data.descricao() != null ? data.descricao() : taskLocal.getDescricao());

        if (data.prazo() != null) {
            taskLocal.setPrazo(LocalDate.parse(data.prazo()));
        }

        if (data.prioridade() != null) {
            taskLocal.setPrioridade(TaskEntity.Prioridade.valueOf(data.prioridade()));
        }

        if (data.status() != null) {
            taskLocal.setStatus(TaskEntity.Status.valueOf(data.status()));
        }

        if (data.idsResponsaveis() != null) {
            List<UserEntity> novosResponsaveis = userRepository.userFindAllByIds(data.idsResponsaveis());
            
            boolean allAreMembers = novosResponsaveis.stream().allMatch(u -> 
                taskLocal.getOrganization().getMembers().stream().anyMatch(m -> m.getUser().getId().equals(u.getId()))
            );
            if (!allAreMembers) {
                throw new SecurityException("Um ou mais usuários não pertencem à organização.");
            }
            
            taskLocal.setResponsaveis(novosResponsaveis);
        }

        salvarHistorico(taskLocal, userLogado, "EDITED", oldTitle, taskLocal.getTitulo());

        return converterParaDTO(taskLocal);
    }

    @Transactional
    public void atualizarStatusTask(Long id, String novoStatus, UserEntity userLogado) {
        TaskEntity taskLocal = taskRepository.findTaskById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada"));

        TaskEntity.Status statusEnum = TaskEntity.Status.valueOf(novoStatus.toLowerCase());
        
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

        if (!organizationService.hasPermissionTo(userLogado, taskLocal.getOrganization().getId(), "DELETE_TASK")) {
            throw new SecurityException("Você não tem permissão para deletar tasks nesta organização.");
        }

        taskRepository.deleteTask(id);
    }

    private void salvarHistorico(TaskEntity task, UserEntity user, String action, String oldVal, String newVal) {
        TaskHistoryEntity historico = TaskHistoryEntity.builder()
            .task(task)
            .user(user)
            .actionType(action)
            .oldValue(oldVal)
            .newValue(newVal)
            .build();
        taskHistoryRepository.save(historico);
    }

    private TaskResponseDTO converterParaDTO(TaskEntity entidade){
        List<String> nomesResponsaveis = entidade.getResponsaveis().stream()
                .map(UserEntity::getNome)
                .collect(Collectors.toList());

        return new TaskResponseDTO(
                entidade.getId(),
                entidade.getTitulo(),
                entidade.getDescricao(),
                entidade.getStatus().name(),
                entidade.getPrioridade().name(),
                entidade.getPrazo(),
                entidade.getCriador().getNome(),
                nomesResponsaveis
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
            .map(h -> new TaskHistoryResponseDTO(
                h.getId(),
                h.getActionType(),
                h.getOldValue(),
                h.getNewValue(),
                h.getUser().getNome(),
                h.getTimestamp()
            )).collect(Collectors.toList());
    }
}
