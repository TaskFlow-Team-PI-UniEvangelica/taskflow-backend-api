package unievangelica.taskflow.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.service.UserService;
import unievangelica.taskflow.api.dto.request.UserRequestDTO;
import unievangelica.taskflow.api.dto.response.UserResponseDTO;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>>listarTodosUsuarios(){
        List<UserResponseDTO> users = userService.listarUsuarios();
        return ResponseEntity.ok(users);
    }

    // rota para listar o usuário logado
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> perfilDeUsuario(@AuthenticationPrincipal UserEntity userLogado) {
        UserResponseDTO profile = userService.obterPerfilUsuario(userLogado);
        return ResponseEntity.ok(profile);
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> criarUsuario(@RequestBody UserRequestDTO data){
        UserResponseDTO novoUsuario = userService.criarUsuario(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoUsuario);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> atualizarUsuario(@PathVariable Long id, @RequestBody UserRequestDTO data){
        UserResponseDTO usuarioAtualizado = userService.atualizarUsuario(id, data);
        return ResponseEntity.ok(usuarioAtualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id){
        userService.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}
