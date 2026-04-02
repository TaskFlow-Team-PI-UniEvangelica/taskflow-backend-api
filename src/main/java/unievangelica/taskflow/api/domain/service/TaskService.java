package unievangelica.taskflow.api.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import unievangelica.taskflow.api.domain.persistence.entities.TaskEntity;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.TaskRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.dto.request.TaskRequestDTO;
import unievangelica.taskflow.api.dto.response.TaskResponseDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
// service responsável pelas regras de negócio
@Service
public class TaskService {
    @Autowired // essa anotação injeta diretamente uma conexão com o banco de dados usando o repository sem precisar instaciar a classe aq
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    // função que retorna todas as tasks por meio das entidades e transformando elas em responsesDTO
    public List<TaskResponseDTO> listarTasks(){
        List<TaskEntity> tasksDB = taskRepository.findAll();
        return tasksDB.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    // função que cria uma task
    public TaskResponseDTO criarTask(TaskRequestDTO data){
        UserEntity criador = userRepository.findById(data.idCriador())
                .orElseThrow(() -> new IllegalArgumentException("Erro: Criador não encontrado"));

        // seta os valores que não necessitam de validação null
        TaskEntity novaTarefa = new TaskEntity();
        novaTarefa.setTitulo(data.titulo());
        novaTarefa.setDescricao(data.descricao());

        // verificações que setam os valores caso sejam null para os valores padrão
        if (data.prazo() != null) {
            novaTarefa.setPrazo(LocalDate.parse(data.prazo()));
        }

        if (data.prioridade() != null) {
            novaTarefa.setPrioridade(TaskEntity.Prioridade.valueOf(data.prioridade()));
        } else {
            novaTarefa.setPrioridade(TaskEntity.Prioridade.media);
        }

        novaTarefa.setStatus(TaskEntity.Status.pendente);
        novaTarefa.setCriador(criador);

        TaskEntity taskLocal = taskRepository.save(novaTarefa);

        return converterParaDTO(taskLocal);
    }

    // função para atualizar task
    public TaskResponseDTO atualizarTask(Long id, TaskRequestDTO data){
        TaskEntity taskExiste = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada"));

        if (data.titulo() != null) {
            taskExiste.setTitulo(data.titulo());
        }

        if (data.descricao() != null) {
            taskExiste.setDescricao(data.descricao());
        }

        if (data.prazo() != null) {
            taskExiste.setPrazo(LocalDate.parse(data.prazo()));
        }

        if (data.prioridade() != null) {
            taskExiste.setPrioridade(TaskEntity.Prioridade.valueOf(data.prioridade()));
        }

        if (data.status() != null) {
            taskExiste.setStatus(TaskEntity.Status.valueOf(data.status()));
        }

        TaskEntity taskLocal = taskRepository.save(taskExiste);

        return converterParaDTO(taskLocal);
    }

    public void deletarTask(Long id){
        if (!taskRepository.existsById(id)) {
            throw new IllegalArgumentException("Tarefa não encontrada");
        }
        taskRepository.deleteById(id);
    }

    private TaskResponseDTO converterParaDTO(TaskEntity entidade){
        return new TaskResponseDTO(
                entidade.getId(),
                entidade.getTitulo(),
                entidade.getDescricao(),
                entidade.getStatus().name(),
                entidade.getPrioridade().name(),
                entidade.getCriador().getNome()
        );
    }
}
