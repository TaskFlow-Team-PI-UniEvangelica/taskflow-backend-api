package unievangelica.taskflow.api.test.unit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import unievangelica.taskflow.api.domain.exception.BusinessException;
import unievangelica.taskflow.api.domain.event.InviteCreatedEvent;
import unievangelica.taskflow.api.domain.persistence.entities.*;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationInviteRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationMemberRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.domain.service.OrganizationInviteService;
import unievangelica.taskflow.api.domain.service.OrganizationService;
import unievangelica.taskflow.api.dto.request.OrganizationInviteRequestDTO;
import unievangelica.taskflow.api.dto.response.InviteResponseDTO;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private ApplicationEventPublisher eventPublisher;

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

        autenticarComToken("owner@taskflow.com", true);

        lenient().when(userRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(userLogado));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** Autentica o "kc-1" com um token real cujas claims email/email_verified são as informadas. */
    private static void autenticarComToken(String email, Boolean emailVerificado) {
        Jwt.Builder builder = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject("kc-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
        if (email != null) builder.claim("email", email);
        if (emailVerificado != null) builder.claim("email_verified", emailVerificado);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(builder.build()));
    }

    private OrganizationInviteEntity convitePendentePara(String email) {
        return OrganizationInviteEntity.builder()
                .id(200L)
                .organization(organization)
                .inviter(userLogado)
                .invitedEmail(email)
                .role("SUB_OWNER")
                .status(InviteStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
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
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Deve criar convite e publicar evento para envio do e-mail após o commit")
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
        ArgumentCaptor<InviteCreatedEvent> evento = ArgumentCaptor.forClass(InviteCreatedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(evento.capture());
        assertEquals(new InviteCreatedEvent("novo@taskflow.com", "Owner User", "TaskFlow Org"), evento.getValue());
    }

    @Test
    @DisplayName("Não deve gravar convite nem publicar evento de e-mail para quem já é membro")
    void testCreateInviteParaMembroExistenteNaoPublicaEvento() {
        UserEntity existente = new UserEntity();
        existente.setId(2L);
        existente.setEmail("membro@taskflow.com");
        OrganizationMemberEntity membro = new OrganizationMemberEntity();
        membro.setOrganization(organization);
        membro.setUser(existente);
        membro.setRole(OrganizationRole.MEMBER);
        organization.setMembers(List.of(membro));

        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(organizationService.hasPermissionTo(userLogado, 10L, "MANAGE_MEMBERS")).thenReturn(true);
        when(memberRepository.existsByOrganizationIdAndUserEmailIgnoreCase(10L, "membro@taskflow.com")).thenReturn(true);

        OrganizationInviteRequestDTO request = new OrganizationInviteRequestDTO();
        request.setEmail("Membro@TaskFlow.com");
        request.setRole("MEMBER");

        assertThrows(BusinessException.class, () -> inviteService.createInvite(10L, request));
        verify(inviteRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Não deve gravar convite nem publicar evento de e-mail se já houver convite pendente válido")
    void testCreateInviteDuplicadoNaoPublicaEvento() {
        OrganizationInviteEntity pendente = OrganizationInviteEntity.builder()
                .id(50L)
                .organization(organization)
                .invitedEmail("novo@taskflow.com")
                .status(InviteStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(3))
                .build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(organizationService.hasPermissionTo(userLogado, 10L, "MANAGE_MEMBERS")).thenReturn(true);
        when(inviteRepository.findByOrganizationIdAndInvitedEmailIgnoreCaseAndStatus(10L, "novo@taskflow.com", InviteStatus.PENDING))
                .thenReturn(Optional.of(pendente));

        OrganizationInviteRequestDTO request = new OrganizationInviteRequestDTO();
        request.setEmail("novo@taskflow.com");
        request.setRole("MEMBER");

        assertThrows(BusinessException.class, () -> inviteService.createInvite(10L, request));
        verify(inviteRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
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

        assertThrows(BusinessException.class, () -> inviteService.acceptInvite(100L));
        assertEquals(InviteStatus.EXPIRED, invite.getStatus());
        verify(memberRepository, never()).save(any());
    }

    // --- ALT-23: convites autorizados pelo e-mail verificado do token, não pelo users.email ---

    @Test
    @DisplayName("Listagem de convites usa o e-mail do token, não o e-mail gravado no banco local")
    void listagemUsaEmailDoToken() {
        userLogado.setEmail("alterado-pela-api@taskflow.com");
        autenticarComToken("Owner@TaskFlow.com ", true);
        when(inviteRepository.findByInvitedEmailIgnoreCaseAndStatus("Owner@TaskFlow.com", InviteStatus.PENDING))
                .thenReturn(List.of(convitePendentePara("owner@taskflow.com")));

        List<InviteResponseDTO> convites = inviteService.getMyInvites();

        assertEquals(1, convites.size());
        verify(inviteRepository, never()).findByInvitedEmailIgnoreCaseAndStatus(eq("alterado-pela-api@taskflow.com"), any());
    }

    @Test
    @DisplayName("Sem e-mail verificado (ou sem a claim) a listagem de convites vem vazia")
    void listagemVaziaSemEmailVerificado() {
        autenticarComToken("owner@taskflow.com", false);
        assertTrue(inviteService.getMyInvites().isEmpty());

        autenticarComToken("owner@taskflow.com", null);
        assertTrue(inviteService.getMyInvites().isEmpty());

        autenticarComToken(null, true);
        assertTrue(inviteService.getMyInvites().isEmpty());

        verifyNoInteractions(inviteRepository);
    }

    @Test
    @DisplayName("Ataque: users.email trocado para o do convidado não permite aceitar o convite (403, nada gravado)")
    void aceiteRecusadoQuandoSoOBancoLocalTemOEmail() {
        OrganizationInviteEntity convite = convitePendentePara("vitima@taskflow.com");
        userLogado.setEmail("vitima@taskflow.com"); // e-mail local adulterado
        autenticarComToken("owner@taskflow.com", true); // e-mail real do token
        when(inviteRepository.findById(200L)).thenReturn(Optional.of(convite));

        SecurityException ex = assertThrows(SecurityException.class, () -> inviteService.acceptInvite(200L));

        assertEquals("Este convite não pertence ao seu usuário.", ex.getMessage());
        assertEquals(InviteStatus.PENDING, convite.getStatus());
        verify(inviteRepository, never()).save(any());
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("E-mail do convite no token, mas não verificado: aceite recusado com 403")
    void aceiteRecusadoComEmailNaoVerificado() {
        OrganizationInviteEntity convite = convitePendentePara("owner@taskflow.com");
        autenticarComToken("owner@taskflow.com", false);
        when(inviteRepository.findById(200L)).thenReturn(Optional.of(convite));

        assertThrows(SecurityException.class, () -> inviteService.acceptInvite(200L));
        assertEquals(InviteStatus.PENDING, convite.getStatus());
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Destinatário com e-mail verificado aceita o convite e entra com o papel do convite")
    void aceiteDoDestinatarioVerificado() {
        OrganizationInviteEntity convite = convitePendentePara("OWNER@taskflow.com");
        when(inviteRepository.findById(200L)).thenReturn(Optional.of(convite));
        when(memberRepository.findByOrganizationIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        inviteService.acceptInvite(200L);

        assertEquals(InviteStatus.ACCEPTED, convite.getStatus());
        ArgumentCaptor<OrganizationMemberEntity> membro = ArgumentCaptor.forClass(OrganizationMemberEntity.class);
        verify(memberRepository).save(membro.capture());
        assertEquals(userLogado, membro.getValue().getUser());
        assertEquals(OrganizationRole.SUB_OWNER, membro.getValue().getRole());
    }

    @Test
    @DisplayName("Recusa de convite de outra pessoa responde 403 e não altera o convite")
    void recusaDeConviteDeOutro() {
        OrganizationInviteEntity convite = convitePendentePara("vitima@taskflow.com");
        when(inviteRepository.findById(200L)).thenReturn(Optional.of(convite));

        assertThrows(SecurityException.class, () -> inviteService.declineInvite(200L));
        assertEquals(InviteStatus.PENDING, convite.getStatus());
        verify(inviteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Destinatário com e-mail verificado recusa o convite")
    void recusaDoDestinatario() {
        OrganizationInviteEntity convite = convitePendentePara("owner@taskflow.com");
        when(inviteRepository.findById(200L)).thenReturn(Optional.of(convite));

        inviteService.declineInvite(200L);

        assertEquals(InviteStatus.DECLINED, convite.getStatus());
        verify(inviteRepository).save(convite);
    }
}
