package unievangelica.taskflow.api.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unievangelica.taskflow.api.domain.persistence.entities.TaskEntity;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.TaskRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.dto.request.TaskRequestDTO;
import unievangelica.taskflow.api.dto.response.TaskResponseDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public List<TaskResponseDTO> listarTasks(){
        List<TaskEntity> tasksDB = taskRepository.listAllTasks();
        return tasksDB.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponseDTO criarTask(TaskRequestDTO data, UserEntity criadorLogado) {
        TaskEntity novaTarefa = new TaskEntity();
        novaTarefa.setTitulo(data.titulo());
        novaTarefa.setDescricao(data.descricao());
        novaTarefa.setCriador(criadorLogado);

        if (data.prazo() != null) {
            novaTarefa.setPrazo(LocalDate.parse(data.prazo()));
        }

        novaTarefa.setPrioridade(data.prioridade() != null ?
                TaskEntity.Prioridade.valueOf(data.prioridade()) :
                TaskEntity.Prioridade.media);

        novaTarefa.setStatus(TaskEntity.Status.pendente);

        if (data.idsResponsaveis() != null && !data.idsResponsaveis().isEmpty()) {
            List<UserEntity> responsaveis = userRepository.userFindAllByIds(data.idsResponsaveis());
            novaTarefa.setResponsaveis(responsaveis);
        }

        TaskEntity taskLocal = taskRepository.save(novaTarefa);

        return converterParaDTO(taskLocal);
    }

    @Transactional
    public TaskResponseDTO atualizarTask(Long id, TaskRequestDTO data) {
        TaskEntity taskLocal = taskRepository.findTaskById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada com ID: " + id));

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
            taskLocal.setResponsaveis(novosResponsaveis);
        }

        return converterParaDTO(taskLocal);
    }

    @Transactional
    public void atualizarStatusTask(Long id, String novoStatus) {
        if (!taskRepository.taskExist(id)) {
            throw new IllegalArgumentException("Tarefa não encontrada");
        }

        TaskEntity.Status statusEnum = TaskEntity.Status.valueOf(novoStatus.toLowerCase());
        taskRepository.updateTaskStatus(id, statusEnum);
    }

    @Transactional
    public void deletarTask(Long id) {
        if (!taskRepository.taskExist(id)) {
            throw new IllegalArgumentException("Tarefa não encontrada");
        }
        taskRepository.deleteTask(id);
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
}
