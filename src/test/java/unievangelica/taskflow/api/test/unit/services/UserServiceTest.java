package unievangelica.taskflow.api.test.unit.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unievangelica.taskflow.api.domain.exception.ConflictException;
import unievangelica.taskflow.api.domain.exception.ResourceNotFoundException;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationRole;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationMemberRepository;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.UserAvatarRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.domain.persistence.entities.UserAvatarEntity;
import unievangelica.taskflow.api.domain.service.UserService;
import unievangelica.taskflow.api.dto.request.UserRequestDTO;
import unievangelica.taskflow.api.dto.response.UserResponseDTO;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationMemberRepository memberRepository;

    @Mock
    private UserAvatarRepository avatarRepository;

    @InjectMocks
    private UserService userService;

    private UserEntity userMock;

    @BeforeEach
    void setUp() {
        userMock = new UserEntity();
        userMock.setId(1L);
        userMock.setNome("User Test");
        userMock.setKeycloakId("keycloak-id");
    }

    @Test
    @DisplayName("Deve buscar usuário por Keycloak ID existente")
    void testBuscarPorKeycloakIdExistente() {
        when(userRepository.findByKeycloakId("keycloak-id")).thenReturn(Optional.of(userMock));

        UserEntity resultado = userService.buscarPorKeycloakId("keycloak-id");

        assertNotNull(resultado);
        assertEquals("User Test", resultado.getNome());
        verify(userRepository, times(1)).findByKeycloakId("keycloak-id");
    }

    @Test
    @DisplayName("Deve lançar exceção quando o Keycloak ID não existir")
    void testBuscarPorKeycloakIdInexistenteLancaExcecao() {
        when(userRepository.findByKeycloakId("new-id")).thenReturn(Optional.empty());

        // O provisionamento JIT é feito pelo UserProvisioningFilter; o UserService apenas busca.
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> userService.buscarPorKeycloakId("new-id"));

        assertEquals("Usuário não encontrado", ex.getMessage());
        verify(userRepository, times(1)).findByKeycloakId("new-id");
    }

    @Test
    @DisplayName("Deve sincronizar nome e e-mail quando forem alterados no Keycloak")
    void testSincronizarPerfilKeycloak() {
        userMock.setNome("Nome Antigo");
        userMock.setEmail("antigo@test.com");

        userService.sincronizarPerfilKeycloak(userMock, "Novo Nome", "novo@test.com", true);

        assertEquals("Novo Nome", userMock.getNome());
        assertEquals("novo@test.com", userMock.getEmail());
        verify(userRepository, times(1)).updateUser(1L, "Novo Nome", "novo@test.com");
    }

    @Test
    @DisplayName("E-mail não verificado no Keycloak não é copiado para o banco (o nome continua sincronizado)")
    void testSincronizacaoIgnoraEmailNaoVerificado() {
        userMock.setNome("Nome Antigo");
        userMock.setEmail("antigo@test.com");

        userService.sincronizarPerfilKeycloak(userMock, "Novo Nome", "vitima@test.com", false);

        assertEquals("antigo@test.com", userMock.getEmail());
        verify(userRepository, times(1)).updateUser(1L, "Novo Nome", "antigo@test.com");
    }

    @Test
    @DisplayName("Sem mudança de nome e com e-mail não verificado, nada é gravado")
    void testSincronizacaoSemAlteracaoNaoGrava() {
        userMock.setEmail("antigo@test.com");

        userService.sincronizarPerfilKeycloak(userMock, "User Test", "vitima@test.com", false);

        verify(userRepository, never()).updateUser(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("PUT de perfil altera só o nome e preserva o e-mail")
    void testAtualizarUsuarioPreservaEmail() {
        userMock.setEmail("atual@test.com");
        when(userRepository.userFindById(1L)).thenReturn(Optional.of(userMock));

        UserResponseDTO resposta = userService.atualizarUsuario(1L, new UserRequestDTO("Nome Novo"));

        verify(userRepository).updateUser(1L, "Nome Novo", "atual@test.com");
        assertEquals("atual@test.com", resposta.email());
        assertEquals("Nome Novo", resposta.nome());
    }

    @Test
    @DisplayName("OWNER de alguma organização não pode excluir a conta (409) e nada é apagado")
    void testExclusaoDeContaBloqueadaParaOwner() {
        when(userRepository.userExistsById(1L)).thenReturn(true);
        when(memberRepository.existsByUserIdAndRole(1L, OrganizationRole.OWNER)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> userService.deletarUsuario(1L));

        assertTrue(ex.getMessage().contains("Transfira a posse"));
        verify(userRepository, never()).deleteUserById(anyLong());
    }

    @Test
    @DisplayName("Quem não é OWNER (MEMBER, SUB_OWNER etc.) exclui a própria conta normalmente")
    void testExclusaoDeContaDeNaoOwner() {
        when(userRepository.userExistsById(1L)).thenReturn(true);
        when(memberRepository.existsByUserIdAndRole(1L, OrganizationRole.OWNER)).thenReturn(false);

        userService.deletarUsuario(1L);

        verify(userRepository).deleteUserById(1L);
    }

    @Test
    @DisplayName("Exclusão de usuário inexistente responde 404 sem consultar vínculos")
    void testExclusaoDeUsuarioInexistente() {
        when(userRepository.userExistsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.deletarUsuario(99L));
        verifyNoInteractions(memberRepository);
    }

    // --- ALT-25: avatar visível só para a própria pessoa e colegas de organização ---

    private UserEntity solicitante(Long id) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setKeycloakId("kc-solicitante");
        when(userRepository.findByKeycloakId("kc-solicitante")).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    @DisplayName("Avatar: a própria pessoa obtém o seu sem consultar organizações")
    void testAvatarDoProprioUsuario() {
        solicitante(1L);
        UserAvatarEntity avatar = new UserAvatarEntity(1L, new byte[]{1}, "image/png", LocalDateTime.now());
        when(avatarRepository.findById(1L)).thenReturn(Optional.of(avatar));

        assertSame(avatar, userService.buscarAvatar(1L, "kc-solicitante").orElseThrow());
        verifyNoInteractions(memberRepository);
        verify(userRepository, never()).userExistsById(anyLong());
    }

    @Test
    @DisplayName("Avatar: colega com organização em comum obtém o avatar")
    void testAvatarDeColegaDeOrganizacao() {
        solicitante(2L);
        when(memberRepository.compartilhamOrganizacao(2L, 1L)).thenReturn(true);
        when(userRepository.userExistsById(1L)).thenReturn(true);
        when(avatarRepository.findById(1L)).thenReturn(Optional.empty());

        // Colega sem avatar: vazio (o controller responde 404 sem corpo)
        assertTrue(userService.buscarAvatar(1L, "kc-solicitante").isEmpty());
    }

    @Test
    @DisplayName("Avatar: sem organização em comum responde 'não encontrado' sem carregar o usuário alvo")
    void testAvatarSemOrganizacaoEmComum() {
        solicitante(2L);
        when(memberRepository.compartilhamOrganizacao(2L, 1L)).thenReturn(false);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> userService.buscarAvatar(1L, "kc-solicitante"));

        assertEquals("Usuário não encontrado", ex.getMessage());
        verifyNoInteractions(avatarRepository);
    }

    @Test
    @DisplayName("Avatar: usuário inexistente (mas permitido) responde 'não encontrado'")
    void testAvatarDeUsuarioInexistente() {
        solicitante(99L);
        when(avatarRepository.findById(99L)).thenReturn(Optional.empty());
        when(userRepository.userExistsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.buscarAvatar(99L, "kc-solicitante"));
    }
}
