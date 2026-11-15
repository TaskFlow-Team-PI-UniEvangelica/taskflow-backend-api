package unievangelica.taskflow.api.test.unit.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unievangelica.taskflow.api.domain.exception.ResourceNotFoundException;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.domain.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

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

        userService.sincronizarPerfilKeycloak(userMock, "Novo Nome", "novo@test.com");

        assertEquals("Novo Nome", userMock.getNome());
        assertEquals("novo@test.com", userMock.getEmail());
        verify(userRepository, times(1)).updateUser(1L, "Novo Nome", "novo@test.com");
    }

    @Test
    @DisplayName("Deve listar todos os usuários")
    void testListarTodos() {
        when(userRepository.userFindAll()).thenReturn(List.of(userMock));

        List<unievangelica.taskflow.api.dto.response.UserResponseDTO> resultados = userService.listarUsuarios();

        assertFalse(resultados.isEmpty());
        assertEquals(1, resultados.size());
        verify(userRepository, times(1)).userFindAll();
    }
}
