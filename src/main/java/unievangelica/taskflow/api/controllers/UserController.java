package unievangelica.taskflow.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Usuários", description = "Endpoints para gerenciamento de perfis e usuários")
public class UserController {
    
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Listar todos os usuários do sistema")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> listarTodosUsuarios(){
        List<UserResponseDTO> users = userService.listarUsuarios();
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Obter perfil do usuário logado", description = "Baseado no JWT enviado no cabeçalho")
    @ApiResponse(responseCode = "200", description = "Perfil retornado com sucesso")
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> perfilDeUsuario(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        
        // Sincronizacao Just-In-Time (Espelha o nome do Keycloak para o Banco de Dados)
        String nomeNoToken = jwt.getClaimAsString("name");
        if (nomeNoToken != null && !nomeNoToken.trim().isEmpty() && !nomeNoToken.equals(userLogado.getNome())) {
            userService.sincronizarNome(userLogado, nomeNoToken);
        }

        UserResponseDTO profile = userService.obterPerfilUsuario(userLogado);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Criar novo usuário manualmente", description = "Cria um usuário no banco da aplicação (idealmente o Keycloak gerencia isso)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping
    public ResponseEntity<UserResponseDTO> criarUsuario(@RequestBody UserRequestDTO data){
        UserResponseDTO novoUsuario = userService.criarUsuario(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoUsuario);
    }

    @Operation(summary = "Atualizar usuário por ID")
    @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> atualizarUsuario(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do usuário a ser atualizado") @PathVariable Long id, 
            @RequestBody UserRequestDTO data){
        if (jwt != null) {
            UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
            if (!userLogado.getId().equals(id)) {
                throw new SecurityException("Você só pode atualizar o seu próprio perfil.");
            }
        }
        UserResponseDTO usuarioAtualizado = userService.atualizarUsuario(id, data);
        return ResponseEntity.ok(usuarioAtualizado);
    }

    @Operation(summary = "Deletar usuário por ID")
    @ApiResponse(responseCode = "204", description = "Usuário deletado sem conteúdo de retorno")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do usuário") @PathVariable Long id){
        if (jwt != null) {
            UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
            if (!userLogado.getId().equals(id)) {
                throw new SecurityException("Você só pode excluir a sua própria conta.");
            }
        }
        userService.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload de Avatar", description = "Faz upload da imagem de perfil do usuário logado")
    @ApiResponse(responseCode = "200", description = "Upload realizado com sucesso")
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadAvatar(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, 
                                             @Parameter(description = "Arquivo de imagem") @RequestParam("file") MultipartFile file) {
        userService.updateAvatar(jwt.getSubject(), file);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Obter Avatar de um usuário", description = "Retorna a imagem raw do avatar do usuário")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Imagem retornada com sucesso", content = @Content(mediaType = "image/jpeg")),
        @ApiResponse(responseCode = "404", description = "Avatar não encontrado")
    })
    @GetMapping(value = "/{id}/avatar")
    public ResponseEntity<byte[]> getAvatar(@Parameter(description = "ID do usuário") @PathVariable Long id) {
        UserEntity user = userService.buscarPorId(id);
        if (user.getAvatar() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, user.getAvatarType() != null ? user.getAvatarType() : "image/jpeg")
                .header("X-Content-Type-Options", "nosniff")
                .body(user.getAvatar());
    }

    @Operation(summary = "Excluir Avatar", description = "Deleta a imagem de perfil do usuário logado")
    @ApiResponse(responseCode = "204", description = "Avatar excluído com sucesso")
    @DeleteMapping(value = "/me/avatar")
    public ResponseEntity<Void> deleteAvatar(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        userService.deleteAvatar(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
