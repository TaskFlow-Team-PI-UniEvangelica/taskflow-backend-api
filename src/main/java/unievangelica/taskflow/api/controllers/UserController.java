package unievangelica.taskflow.api.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.service.UserService;
import unievangelica.taskflow.api.dto.request.UserRequestDTO;
import unievangelica.taskflow.api.dto.response.UserResponseDTO;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/user")
public class UserController {
    
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>>listarTodosUsuarios(){
        List<UserResponseDTO> users = userService.listarUsuarios();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> perfilDeUsuario(@AuthenticationPrincipal Jwt jwt) {
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        
        // Sincronizacao Just-In-Time (Espelha o nome do Keycloak para o Banco de Dados)
        String nomeNoToken = jwt.getClaimAsString("name");
        if (nomeNoToken != null && !nomeNoToken.trim().isEmpty() && !nomeNoToken.equals(userLogado.getNome())) {
            userService.sincronizarNome(userLogado, nomeNoToken);
        }

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

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadAvatar(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file) {
        userService.updateAvatar(jwt.getSubject(), file);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/{id}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long id) {
        UserEntity user = userService.buscarPorId(id);
        if (user.getAvatar() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, user.getAvatarType() != null ? user.getAvatarType() : "image/jpeg")
                .body(user.getAvatar());
    }

    @DeleteMapping(value = "/me/avatar")
    public ResponseEntity<Void> deleteAvatar(@AuthenticationPrincipal Jwt jwt) {
        userService.deleteAvatar(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

}
