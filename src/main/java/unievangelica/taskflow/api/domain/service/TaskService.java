package unievangelica.taskflow.api.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
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
// service responsável pelas regras de negócio
@Service
public class TaskService {
    @Autowired // essa anotação injeta diretamente uma conexão com o banco de dados usando o repository sem precisar instaciar a classe aq
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    // função que retorna todas as tasks por meio das entidades e transformando elas em responsesDTO
    public List<TaskResponseDTO> listarTasks(){
        List<TaskEntity> tasksDB = taskRepository.listAllTasks();
        return tasksDB.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    // função que cria uma task refatorada para receber vários responsáveis
    @Transactional
    public TaskResponseDTO criarTask(TaskRequestDTO data, UserEntity criadorLogado){
        // seta os valores que não necessitam de validação null
        TaskEntity novaTarefa = new TaskEntity();
        novaTarefa.setTitulo(data.titulo());
        novaTarefa.setDescricao(data.descricao());

        // criador sempre baseado no usuário logado
        novaTarefa.setCriador(criadorLogado);

        // verificações que setam os valores caso sejam null para os valores padrão
        if (data.prazo() != null) {
            novaTarefa.setPrazo(LocalDate.parse(data.prazo()));
        }

        // operador tenário responsável por definir a prioridade média caso não seja preenchida
        novaTarefa.setPrioridade(data.prioridade() != null ?
                TaskEntity.Prioridade.valueOf(data.prioridade()) :
                TaskEntity.Prioridade.media);

        // toda task nova deve nascer como pendente e ser definida posteriormente
        novaTarefa.setStatus(TaskEntity.Status.pendente);

        // se tiver responsáveis vincula eles
        if (data.idsResponsaveis() != null && !data.idsResponsaveis().isEmpty()) {
            // busca os usuários pelos ids da lista
            List<UserEntity> responsaveis = userRepository.userFindAllByIds(data.idsResponsaveis());
            novaTarefa.setResponsaveis(responsaveis);
        }

        TaskEntity taskLocal = taskRepository.save(novaTarefa);

        return converterParaDTO(taskLocal);
    }

    // função para atualizar task
    @Transactional
    public TaskResponseDTO atualizarTask(Long id, TaskRequestDTO data){
        TaskEntity taskLocal = taskRepository.findTaskById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada"));

        // operador tenário para decidir campos simples
        String novoTitulo = data.titulo() != null ? data.titulo() : taskLocal.getTitulo();
        String novaDescricao = data.descricao() != null ? data.descricao() : taskLocal.getDescricao();

        if (data.prazo() != null) {
            taskLocal.setPrazo(LocalDate.parse(data.prazo()));
        }

        if (data.prioridade() != null) {
            taskLocal.setPrioridade(TaskEntity.Prioridade.valueOf(data.prioridade()));
        }

        if (data.status() != null) {
            taskLocal.setStatus(TaskEntity.Status.valueOf(data.status()));
        }

        // atualização dos responsáveis pela task
        if (data.idsResponsaveis() != null) {
            // busca os novos usuários responsáveis com o mesmo método usado em criar task
            List<UserEntity> novosResponsaveis = userRepository.userFindAllByIds(data.idsResponsaveis());
            // sobrescreve a lista antiga pela nova
            taskLocal.setResponsaveis(novosResponsaveis);
        }

        return converterParaDTO(taskLocal);
    }

    @Transactional
    public void deletarTask(Long id){
        if (!taskRepository.taskExist(id)) {
            throw new IllegalArgumentException("Tarefa não encontrada");
        }
        taskRepository.deleteTask(id);
    }

    private TaskResponseDTO converterParaDTO(TaskEntity entidade){
        // transfoma a lista de entidades de usuários em uma lista de strings com os nomes dos responsáveis
        List<String> nomesResponsaveis = entidade.getResponsaveis().stream()
                .map(UserEntity::getNome) // extrai o nome de cada usuário
                .collect(Collectors.toList());

        return new TaskResponseDTO(
                entidade.getId(),
                entidade.getTitulo(),
                entidade.getDescricao(),
                entidade.getStatus().name(),
                entidade.getPrioridade().name(),
                entidade.getPrazo(),
                entidade.getCriador().getNome(), // nome do criador
                nomesResponsaveis // nome dos responsáveis
        );
    }
}
