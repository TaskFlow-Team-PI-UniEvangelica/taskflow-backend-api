package unievangelica.taskflow.api.test.unit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import unievangelica.taskflow.api.domain.persistence.entities.*;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationInviteRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationMemberRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.domain.service.EmailService;
import unievangelica.taskflow.api.domain.service.OrganizationInviteService;
import unievangelica.taskflow.api.domain.service.OrganizationService;
import unievangelica.taskflow.api.dto.request.OrganizationInviteRequestDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrganizationInviteServiceTest {

    @Mock
    private OrganizationInviteRepository inviteRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationMemberRepository memberRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrganizationInviteService inviteService;

    private UserEntity userLogado;
    private OrganizationEntity organization;

    @BeforeEach
    void setUp() {
        userLogado = new UserEntity();
        userLogado.setId(1L);
        userLogado.setNome("Owner User");
        userLogado.setEmail("owner@taskflow.com");
        userLogado.setKeycloakId("kc-1");

        organization = new OrganizationEntity();
        organization.setId(10L);
        organization.setNome("TaskFlow Org");
        organization.setMembers(List.of());

        Jwt jwt = mock(Jwt.class);
        lenient().when(jwt.getSubject()).thenReturn("kc-1");
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getPrincipal()).thenReturn(jwt);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        lenient().when(userRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(userLogado));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve bloquear criação de convite se o usuário não tiver permissão MANAGE_MEMBERS")
    void testCreateInviteSemPermissao() {
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(organizationService.hasPermissionTo(userLogado, 10L, "MANAGE_MEMBERS")).thenReturn(false);

        OrganizationInviteRequestDTO request = new OrganizationInviteRequestDTO();
        request.setEmail("novo@taskflow.com");
        request.setRole("MEMBER");

        assertThrows(SecurityException.class, () -> inviteService.createInvite(10L, request));
        verify(inviteRepository, never()).save(any());
        verify(emailService, never()).sendInviteEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve criar convite e disparar e-mail quando solicitante possui permissão e hierarquia")
    void testCreateInviteComSucesso() {
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(organizationService.hasPermissionTo(userLogado, 10L, "MANAGE_MEMBERS")).thenReturn(true);
        doNothing().when(organizationService).validarHierarquiaDeRoles(userLogado, 10L, OrganizationRole.MEMBER);
        when(inviteRepository.findByOrganizationIdAndInvitedEmailIgnoreCaseAndStatus(10L, "novo@taskflow.com", InviteStatus.PENDING))
                .thenReturn(Optional.empty());

        OrganizationInviteRequestDTO request = new OrganizationInviteRequestDTO();
        request.setEmail("Novo@TaskFlow.com ");
        request.setRole("MEMBER");

        inviteService.createInvite(10L, request);

        verify(inviteRepository, times(1)).save(any(OrganizationInviteEntity.class));
        verify(emailService, times(1)).sendInviteEmail("novo@taskflow.com", "Owner User", "TaskFlow Org");
    }

    @Test
    @DisplayName("Deve recusar aceite de convite já expirado e marcar status como EXPIRED")
    void testAcceptInviteExpirado() {
        OrganizationInviteEntity invite = OrganizationInviteEntity.builder()
                .id(100L)
                .organization(organization)
                .inviter(userLogado)
                .invitedEmail("owner@taskflow.com")
                .role("MEMBER")
                .status(InviteStatus.PENDING)
                .createdAt(LocalDateTime.now().minusDays(10))
                .expiresAt(LocalDateTime.now().minusDays(3))
                .build();

        when(inviteRepository.findById(100L)).thenReturn(Optional.of(invite));

        assertThrows(IllegalArgumentException.class, () -> inviteService.acceptInvite(100L));
        assertEquals(InviteStatus.EXPIRED, invite.getStatus());
        verify(memberRepository, never()).save(any());
    }
}
