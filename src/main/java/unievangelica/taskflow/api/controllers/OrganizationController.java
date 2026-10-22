package unievangelica.taskflow.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.service.OrganizationService;
import unievangelica.taskflow.api.domain.service.UserService;
import unievangelica.taskflow.api.dto.request.OrganizationRequestDTO;
import unievangelica.taskflow.api.dto.response.OrganizationResponseDTO;

import java.util.List;

@RestController
@RequestMapping("/organization")
@Tag(name = "Organizações", description = "Endpoints para gerenciamento de organizações Multi-Tenant, governança de exclusão e retenção de auditoria")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserService userService;

    public OrganizationController(OrganizationService organizationService, UserService userService) {
        this.organizationService = organizationService;
        this.userService = userService;
    }

    @Operation(
        summary = "Criar Organização",
        description = "Cria uma nova organização com regras de governança. O usuário logado torna-se OWNER. " +
                      "As flags membersCanDeleteTasks e membersCanDeleteOwnTasks são mutuamente exclusivas, e membersCanDeleteOwnTasks exige membersCanCreateTasks=true."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Organização criada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Conflito de regras de permissão (ex: exclusão própria sem criação ligada ou ambas exclusões ligadas)")
    })
    @PostMapping
    public ResponseEntity<OrganizationResponseDTO> criarOrganizacao(
            @RequestBody OrganizationRequestDTO data,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        OrganizationResponseDTO novaOrg = organizationService.criarOrganizacao(data, userLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaOrg);
    }

    @Operation(summary = "Listar minhas organizações", description = "Retorna todas as organizações das quais o usuário logado faz parte, bem como a sua Role e as configurações vigentes em cada uma.")
    @ApiResponse(responseCode = "200", description = "Lista de organizações retornada com sucesso")
    @GetMapping("/me")
    public ResponseEntity<List<OrganizationResponseDTO>> listarMinhasOrganizacoes(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        List<OrganizationResponseDTO> organizacoes = organizationService.listarOrganizacoesDoUsuario(userLogado);
        return ResponseEntity.ok(organizacoes);
    }

    @Operation(
        summary = "Atualizar configurações da organização",
        description = "Atualiza nome e configurações de permissões dinâmicas (requer OWNER ou SUB_OWNER). " +
                      "A alteração de deletedTasksRetentionDays (retenção do histórico de tasks excluídas) é exclusiva do OWNER."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configurações atualizadas"),
            @ApiResponse(responseCode = "400", description = "Regras de exclusão mutuamente exclusivas violadas"),
            @ApiResponse(responseCode = "403", description = "Usuário não possui privilégios de OWNER/SUB_OWNER ou tentou alterar retenção sem ser OWNER"),
            @ApiResponse(responseCode = "404", description = "Organização não encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<OrganizationResponseDTO> atualizarOrganizacao(
            @Parameter(description = "ID da organização") @PathVariable Long id,
            @RequestBody OrganizationRequestDTO data,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        OrganizationResponseDTO orgAtualizada = organizationService.atualizarOrganizacao(id, data, userLogado);
        return ResponseEntity.ok(orgAtualizada);
    }

    @Operation(summary = "Listar membros da organização", description = "Retorna a lista de membros de uma organização específica.")
    @ApiResponse(responseCode = "200", description = "Lista de membros retornada")
    @GetMapping("/{id}/members")
    public ResponseEntity<List<unievangelica.taskflow.api.dto.response.MemberResponseDTO>> listarMembros(
            @Parameter(description = "ID da organização") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        List<unievangelica.taskflow.api.dto.response.MemberResponseDTO> membros = organizationService.listarMembros(id, userLogado);
        return ResponseEntity.ok(membros);
    }

    @Operation(summary = "Adicionar membro", description = "Adiciona um usuário à organização. Requer privilégio OWNER, SUB_OWNER ou MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Membro adicionado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para gerenciar membros ou atribuir o papel solicitado")
    })
    @PostMapping("/{id}/members")
    public ResponseEntity<Void> adicionarMembro(
            @Parameter(description = "ID da organização") @PathVariable Long id,
            @RequestBody unievangelica.taskflow.api.dto.request.MemberRequestDTO data,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        if (data.email() != null && !data.email().isBlank()) {
            organizationService.adicionarMembroPorEmail(id, data.email(), data.role(), userLogado);
        } else {
            organizationService.adicionarMembro(id, data.userId(), data.role(), userLogado);
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Atualizar papel do membro", description = "Altera o nível de acesso (Role) de um usuário na organização.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Papel atualizado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Tentativa de rebaixar OWNER ou violação de hierarquia pelo SUB_OWNER")
    })
    @PutMapping("/{id}/members/{userId}/role")
    public ResponseEntity<Void> atualizarPapelMembro(
            @Parameter(description = "ID da organização") @PathVariable Long id,
            @Parameter(description = "ID do usuário (membro)") @PathVariable Long userId,
            @RequestBody unievangelica.taskflow.api.dto.request.MemberRequestDTO data,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        organizationService.atualizarPapelMembro(id, userId, data.role(), userLogado);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Transferir posse da organização", description = "Transfere o cargo de OWNER para um SUB_OWNER da organização.")
    @PutMapping("/{id}/members/{userId}/transfer-ownership")
    public ResponseEntity<Void> transferirPosse(
            @Parameter(description = "ID da organização") @PathVariable Long id,
            @Parameter(description = "ID do novo OWNER") @PathVariable Long userId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        organizationService.transferirPosse(id, userId, userLogado);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remover membro", description = "Remove um usuário da organização.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Membro removido com sucesso"),
            @ApiResponse(responseCode = "403", description = "Tentativa de remover OWNER ou violação de hierarquia")
    })
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removerMembro(
            @Parameter(description = "ID da organização") @PathVariable Long id,
            @Parameter(description = "ID do usuário (membro)") @PathVariable Long userId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        organizationService.removerMembro(id, userId, userLogado);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Excluir organização", description = "Exclui a organização permanentemente. Apenas o OWNER pode realizar esta ação.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Organização excluída com sucesso"),
            @ApiResponse(responseCode = "403", description = "Apenas o OWNER pode excluir a organização")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarOrganizacao(
            @Parameter(description = "ID da organização") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UserEntity userLogado = userService.buscarPorKeycloakId(jwt.getSubject());
        organizationService.deletarOrganizacao(id, userLogado);
        return ResponseEntity.noContent().build();
    }
}
