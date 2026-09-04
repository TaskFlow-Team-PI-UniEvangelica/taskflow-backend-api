package unievangelica.taskflow.api.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import unievangelica.taskflow.api.domain.service.UserService;
import org.springframework.web.bind.annotation.*;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.service.TaskService;
import unievangelica.taskflow.api.dto.request.TaskRequestDTO;
import unievangelica.taskflow.api.dto.request.TaskStatusRequestDTO;
import unievangelica.taskflow.api.dto.response.TaskResponseDTO;

import java.util.List;

@RestController
@RequestMapping("/task")
public class TaskController {
    
    private final UserService userService;
    private final TaskService taskService;

    public TaskController(UserService userService, TaskService taskService) {
        this.userService = userService;
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> verTodasTasks(){
        List<TaskResponseDTO> tasks = taskService.listarTasks();
        return ResponseEntity.ok(tasks);
    }

    @PostMapping
    public ResponseEntity<TaskResponseDTO> criarTask(@RequestBody TaskRequestDTO data, @AuthenticationPrincipal Jwt jwt){
       UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
       TaskResponseDTO novaTask = taskService.criarTask(data, userLogado);
       return ResponseEntity.status(HttpStatus.CREATED).body(novaTask);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> updateTask(@PathVariable Long id, @RequestBody TaskRequestDTO data){
        TaskResponseDTO taskAtualizada = taskService.atualizarTask(id, data);
        return ResponseEntity.ok(taskAtualizada);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatusTask(@PathVariable Long id, @RequestBody TaskStatusRequestDTO data){
        taskService.atualizarStatusTask(id, data.status());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id){
        taskService.deletarTask(id);
        return ResponseEntity.noContent().build();
    }
}
