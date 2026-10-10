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
import unievangelica.taskflow.api.controllers.OrganizationController;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.service.OrganizationService;
import unievangelica.taskflow.api.domain.service.UserService;
import unievangelica.taskflow.api.dto.request.MemberRequestDTO;
import unievangelica.taskflow.api.dto.request.OrganizationRequestDTO;
import unievangelica.taskflow.api.dto.response.OrganizationResponseDTO;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class OrganizationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private OrganizationController organizationController;

    private ObjectMapper objectMapper;
    private Jwt mockJwt;
    private UserEntity mockUser;

    @BeforeEach
    void setUp() {
        mockJwt = mock(Jwt.class);
        lenient().when(mockJwt.getSubject()).thenReturn("keycloak-id-123");

        mockUser = new UserEntity();
        mockUser.setId(1L);
        mockUser.setKeycloakId("keycloak-id-123");

        mockMvc = MockMvcBuilders.standaloneSetup(organizationController)
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
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Deve criar organização com sucesso (POST /organization)")
    void testCriarOrganizacao() throws Exception {
        OrganizationRequestDTO request = new OrganizationRequestDTO("Minha Org", true, true, true, true);
        OrganizationResponseDTO response = new OrganizationResponseDTO(1L, "Minha Org", null, true, true, true, true, "OWNER");

        when(userService.buscarPorKeycloakId("keycloak-id-123")).thenReturn(mockUser);
        when(organizationService.criarOrganizacao(any(OrganizationRequestDTO.class), eq(mockUser))).thenReturn(response);

        mockMvc.perform(post("/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Minha Org"))
                .andExpect(jsonPath("$.myRole").value("OWNER"));

        verify(organizationService, times(1)).criarOrganizacao(any(OrganizationRequestDTO.class), eq(mockUser));
    }

    @Test
    @DisplayName("Deve listar organizações do usuário logado (GET /organization/me)")
    void testListarMinhasOrganizacoes() throws Exception {
        OrganizationResponseDTO response = new OrganizationResponseDTO(1L, "Minha Org", null, true, true, true, true, "OWNER");

        when(userService.buscarPorKeycloakId("keycloak-id-123")).thenReturn(mockUser);
        when(organizationService.listarOrganizacoesDoUsuario(eq(mockUser))).thenReturn(List.of(response));

        mockMvc.perform(get("/organization/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nome").value("Minha Org"));

        verify(organizationService, times(1)).listarOrganizacoesDoUsuario(eq(mockUser));
    }

    @Test
    @DisplayName("Deve atualizar configurações da organização (PUT /organization/{id})")
    void testAtualizarOrganizacao() throws Exception {
        OrganizationRequestDTO request = new OrganizationRequestDTO("Org Atualizada", false, false, false, false);
        OrganizationResponseDTO response = new OrganizationResponseDTO(1L, "Org Atualizada", null, false, false, false, false, "OWNER");

        when(userService.buscarPorKeycloakId("keycloak-id-123")).thenReturn(mockUser);
        when(organizationService.atualizarOrganizacao(eq(1L), any(OrganizationRequestDTO.class), eq(mockUser))).thenReturn(response);

        mockMvc.perform(put("/organization/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Org Atualizada"));

        verify(organizationService, times(1)).atualizarOrganizacao(eq(1L), any(OrganizationRequestDTO.class), eq(mockUser));
    }

    @Test
    @DisplayName("Deve adicionar membro (POST /organization/{id}/members)")
    void testAdicionarMembro() throws Exception {
        MemberRequestDTO request = new MemberRequestDTO(2L, null, "MANAGER");

        when(userService.buscarPorKeycloakId("keycloak-id-123")).thenReturn(mockUser);
        doNothing().when(organizationService).adicionarMembro(eq(1L), eq(2L), eq("MANAGER"), eq(mockUser));

        mockMvc.perform(post("/organization/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(organizationService, times(1)).adicionarMembro(eq(1L), eq(2L), eq("MANAGER"), eq(mockUser));
    }

    @Test
    @DisplayName("Deve atualizar papel de membro (PUT /organization/{id}/members/{userId}/role)")
    void testAtualizarPapelMembro() throws Exception {
        MemberRequestDTO request = new MemberRequestDTO(2L, null, "SUB_OWNER"); // DTO has userId but path has it too

        when(userService.buscarPorKeycloakId("keycloak-id-123")).thenReturn(mockUser);
        doNothing().when(organizationService).atualizarPapelMembro(eq(1L), eq(2L), eq("SUB_OWNER"), eq(mockUser));

        mockMvc.perform(put("/organization/1/members/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(organizationService, times(1)).atualizarPapelMembro(eq(1L), eq(2L), eq("SUB_OWNER"), eq(mockUser));
    }

    @Test
    @DisplayName("Deve remover membro (DELETE /organization/{id}/members/{userId})")
    void testRemoverMembro() throws Exception {
        when(userService.buscarPorKeycloakId("keycloak-id-123")).thenReturn(mockUser);
        doNothing().when(organizationService).removerMembro(eq(1L), eq(2L), eq(mockUser));

        mockMvc.perform(delete("/organization/1/members/2"))
                .andExpect(status().isNoContent());

        verify(organizationService, times(1)).removerMembro(eq(1L), eq(2L), eq(mockUser));
    }

    @Test
    @DisplayName("Deve transferir posse (PUT /organization/{id}/members/{userId}/transfer-ownership)")
    void testTransferirPosse() throws Exception {
        when(userService.buscarPorKeycloakId("keycloak-id-123")).thenReturn(mockUser);
        doNothing().when(organizationService).transferirPosse(eq(1L), eq(2L), eq(mockUser));

        mockMvc.perform(put("/organization/1/members/2/transfer-ownership"))
                .andExpect(status().isNoContent());

        verify(organizationService, times(1)).transferirPosse(eq(1L), eq(2L), eq(mockUser));
    }

    @Test
    @DisplayName("Deve excluir organização (DELETE /organization/{id})")
    void testDeletarOrganizacao() throws Exception {
        when(userService.buscarPorKeycloakId("keycloak-id-123")).thenReturn(mockUser);
        doNothing().when(organizationService).deletarOrganizacao(eq(1L), eq(mockUser));

        mockMvc.perform(delete("/organization/1"))
                .andExpect(status().isNoContent());

        verify(organizationService, times(1)).deletarOrganizacao(eq(1L), eq(mockUser));
    }
}
