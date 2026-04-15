package unievangelica.taskflow.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.service.TaskService;
import unievangelica.taskflow.api.dto.request.TaskRequestDTO;
import unievangelica.taskflow.api.dto.request.TaskStatusRequestDTO;
import unievangelica.taskflow.api.dto.response.TaskResponseDTO;

import java.util.List;

@RestController
@RequestMapping("/task") // Cria o Endpoint
public class TaskController {
    @Autowired
    private TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> verTodasTasks(){
        List<TaskResponseDTO> tasks = taskService.listarTasks();
        return ResponseEntity.ok(tasks);
    }

    // permitir a rota a receber usuário logado ou seja puxar o id do criador pelo token do usuário
    @PostMapping
    public ResponseEntity<TaskResponseDTO> criarTask(@RequestBody TaskRequestDTO data, @AuthenticationPrincipal UserEntity userLogado){
       TaskResponseDTO novaTask = taskService.criarTask(data, userLogado);
       return ResponseEntity.status(HttpStatus.CREATED).body(novaTask);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> updateTask(@PathVariable Long id, @RequestBody TaskRequestDTO data){
        TaskResponseDTO taskAtualizada = taskService.atualizarTask(id, data);
        return ResponseEntity.ok(taskAtualizada);
    }

    // rota patch para atualizar status das task
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
