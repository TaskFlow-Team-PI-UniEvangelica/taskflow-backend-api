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

        // operador tenário substituindo o if/else para decidir quando subistituir os campos no update
        String novoTitulo = data.titulo() != null ? data.titulo() : taskLocal.getTitulo();
        String novaDescricao = data.descricao() != null ? data.descricao() : taskLocal.getDescricao();
        LocalDate novoPrazo = data.prazo() != null ? LocalDate.parse(data.prazo()) : taskLocal.getPrazo();
        TaskEntity.Prioridade novaPrioridade = data.prioridade() != null ? TaskEntity.Prioridade.valueOf(data.prioridade()) : taskLocal.getPrioridade();
        TaskEntity.Status novoStatus = data.status() != null ? TaskEntity.Status.valueOf(data.status()) : taskLocal.getStatus();

        // usando o jpql de update para atualizar os dados
        taskRepository.updateTask(id, novoTitulo, novaDescricao, novoPrazo, novaPrioridade, novoStatus);

        // definindo as variaveis para atualizar o objeto local para converter no DTO
        taskLocal.setTitulo(novoTitulo);
        taskLocal.setDescricao(novaDescricao);
        taskLocal.setPrazo(novoPrazo);
        taskLocal.setPrioridade(novaPrioridade);
        taskLocal.setStatus(novoStatus);

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
                entidade.getCriador().getNome(), // nome do criador
                nomesResponsaveis // nome dos responsáveis
        );
    }
}
