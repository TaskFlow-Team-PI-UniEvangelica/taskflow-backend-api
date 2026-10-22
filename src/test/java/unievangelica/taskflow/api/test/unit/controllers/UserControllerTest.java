package unievangelica.taskflow.api.test.unit.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import unievangelica.taskflow.api.controllers.UserController;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.service.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private Jwt mockJwt;
    private UserEntity mockUser;

    @BeforeEach
    void setUp() {
        mockJwt = mock(Jwt.class);
        lenient().when(mockJwt.getSubject()).thenReturn("keycloak-id-123");

        mockUser = new UserEntity();
        mockUser.setId(1L);
        mockUser.setNome("User Test");
        mockUser.setKeycloakId("keycloak-id-123");

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(Jwt.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return mockJwt;
                    }
                })
                .build();
    }

    @Test
    @DisplayName("Deve retornar informações do usuário logado (GET /user/me)")
    void testGetMyUser() throws Exception {
        when(userService.buscarPorKeycloakId("keycloak-id-123")).thenReturn(mockUser);
        
        // userService.obterPerfilUsuario is called inside perfilDeUsuario
        unievangelica.taskflow.api.dto.response.UserResponseDTO profileDTO = new unievangelica.taskflow.api.dto.response.UserResponseDTO(1L, "User Test", null, "MEMBER");
        when(userService.obterPerfilUsuario(mockUser)).thenReturn(profileDTO);

        mockMvc.perform(get("/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("User Test"));

        verify(userService, times(1)).buscarPorKeycloakId("keycloak-id-123");
    }

    @Test
    @DisplayName("Deve listar todos os usuários (GET /user)")
    void testListarTodos() throws Exception {
        unievangelica.taskflow.api.dto.response.UserResponseDTO responseDTO = new unievangelica.taskflow.api.dto.response.UserResponseDTO(1L, "User Test", "test@test.com", "MEMBER");
        when(userService.listarUsuarios()).thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("User Test"));

        verify(userService, times(1)).listarUsuarios();
    }
}
