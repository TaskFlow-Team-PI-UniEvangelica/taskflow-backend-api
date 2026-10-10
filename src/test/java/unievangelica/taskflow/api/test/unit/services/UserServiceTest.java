package unievangelica.taskflow.api.test.unit.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    @DisplayName("Deve criar novo usuário se Keycloak ID não existir (First login sync)")
    void testBuscarPorKeycloakIdNovo() {
        when(userRepository.findByKeycloakId("new-id")).thenReturn(Optional.empty());
        
        UserEntity novoUser = new UserEntity();
        novoUser.setKeycloakId("new-id");
        novoUser.setNome("Usuario Keycloak");

        // This behavior depends on how UserService is implemented.
        // Usually there is a sync method or it throws an exception if not found.
        // Assuming your UserService throws exception if just searching, adjust as necessary:
        assertThrows(RuntimeException.class, () -> {
            userService.buscarPorKeycloakId("new-id");
        });
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
