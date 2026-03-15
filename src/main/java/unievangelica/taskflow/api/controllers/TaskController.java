package unievangelica.taskflow.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import unievangelica.taskflow.api.domain.service.TaskService;
import unievangelica.taskflow.api.dto.request.TaskRequestDTO;
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

    @PostMapping
    public ResponseEntity<TaskResponseDTO> criarTask(@RequestBody TaskRequestDTO data){
       TaskResponseDTO novaTask = taskService.criarTask(data);
       return ResponseEntity.status(HttpStatus.CREATED).body(novaTask);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> UpdateTask(@PathVariable Long id, @RequestBody TaskRequestDTO data){
        TaskResponseDTO taskAtualizada = taskService.atualizarTask(id, data);
        return ResponseEntity.ok(taskAtualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> DeleteTask(@PathVariable Long id){
        taskService.deletarTask(id);
        return ResponseEntity.noContent().build();
    }

}
