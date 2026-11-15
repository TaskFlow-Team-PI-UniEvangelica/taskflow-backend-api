package unievangelica.taskflow.api.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unievangelica.taskflow.api.domain.exception.BusinessException;
import unievangelica.taskflow.api.domain.exception.ResourceNotFoundException;
import unievangelica.taskflow.api.domain.event.InviteCreatedEvent;
import unievangelica.taskflow.api.domain.persistence.entities.*;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationInviteRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationMemberRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.dto.request.OrganizationInviteRequestDTO;
import unievangelica.taskflow.api.dto.response.InviteResponseDTO;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationInviteService {

    private final OrganizationInviteRepository inviteRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationService organizationService;
    private final ApplicationEventPublisher eventPublisher;

    private static final String CONVITE_DE_OUTRO = "Este convite não pertence ao seu usuário.";

    private static Jwt getJwt() {
        return (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private UserEntity getAuthenticatedUser() {
        return userRepository.findByKeycloakId(getJwt().getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    /**
     * E-mail que autoriza convites: a claim {@code email} do token, apenas se o Keycloak a marca como
     * verificada. Não usa {@code users.email}, que reflete o banco local e não prova posse do e-mail.
     */
    private static Optional<String> emailVerificadoDoToken() {
        Jwt jwt = getJwt();
        String email = jwt.getClaimAsString("email");
        if (!Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")) || email == null || email.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(email.trim());
    }

    private static void exigirDestinatario(OrganizationInviteEntity invite) {
        boolean pertence = emailVerificadoDoToken()
                .filter(email -> email.equalsIgnoreCase(invite.getInvitedEmail()))
                .isPresent();
        if (!pertence) {
            throw new SecurityException(CONVITE_DE_OUTRO);
        }
    }

    @Transactional
    public void createInvite(Long organizationId, OrganizationInviteRequestDTO request) {
        UserEntity inviter = getAuthenticatedUser();
        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada"));

        if (!organizationService.hasPermissionTo(inviter, organizationId, "MANAGE_MEMBERS")) {
            throw new SecurityException("Você não tem permissão para convidar membros para esta organização.");
        }

        OrganizationRole targetRole = request.getRole() != null
                ? RequestValueParser.papel(request.getRole())
                : OrganizationRole.MEMBER;
        organizationService.validarHierarquiaDeRoles(inviter, organizationId, targetRole);

        String invitedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (invitedEmail.isEmpty()) {
            throw new BusinessException("O e-mail do convidado é obrigatório.");
        }

        boolean alreadyMember = organization.getMembers().stream()
                .anyMatch(m -> m.getUser().getEmail() != null && m.getUser().getEmail().equalsIgnoreCase(invitedEmail));
        if (alreadyMember) {
            throw new BusinessException("Este usuário já é membro da organização.");
        }

        Optional<OrganizationInviteEntity> existingInvite = inviteRepository
                .findByOrganizationIdAndInvitedEmailIgnoreCaseAndStatus(organizationId, invitedEmail, InviteStatus.PENDING);
        if (existingInvite.isPresent() && existingInvite.get().getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Já existe um convite pendente para este e-mail nesta organização.");
        }

        OrganizationInviteEntity invite = OrganizationInviteEntity.builder()
                .organization(organization)
                .inviter(inviter)
                .invitedEmail(invitedEmail)
                .role(targetRole.name())
                .status(InviteStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        inviteRepository.save(invite);

        // O e-mail é enviado pelo InviteEmailListener após o commit, de forma assíncrona
        eventPublisher.publishEvent(new InviteCreatedEvent(invitedEmail, inviter.getNome(), organization.getNome()));
    }

    public List<InviteResponseDTO> getMyInvites() {
        Optional<String> email = emailVerificadoDoToken();
        if (email.isEmpty()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        return inviteRepository.findByInvitedEmailIgnoreCaseAndStatus(email.get(), InviteStatus.PENDING).stream()
                .filter(inv -> inv.getExpiresAt() == null || inv.getExpiresAt().isAfter(now))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void acceptInvite(Long inviteId) {
        UserEntity user = getAuthenticatedUser();
        OrganizationInviteEntity invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado"));

        exigirDestinatario(invite);
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new BusinessException("Este convite já foi processado.");
        }
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            invite.setStatus(InviteStatus.EXPIRED);
            inviteRepository.save(invite);
            throw new BusinessException("Este convite expirou.");
        }

        invite.setStatus(InviteStatus.ACCEPTED);
        inviteRepository.save(invite);

        Long orgId = invite.getOrganization().getId();
        if (memberRepository.findByOrganizationIdAndUserId(orgId, user.getId()).isEmpty()) {
            OrganizationMemberEntity member = new OrganizationMemberEntity();
            member.setId(new OrganizationMemberId(orgId, user.getId()));
            member.setOrganization(invite.getOrganization());
            member.setUser(user);
            member.setRole(OrganizationRole.valueOf(invite.getRole()));
            memberRepository.save(member);
        }
    }

    @Transactional
    public void declineInvite(Long inviteId) {
        OrganizationInviteEntity invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado"));

        exigirDestinatario(invite);
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new BusinessException("Este convite já foi processado.");
        }

        invite.setStatus(InviteStatus.DECLINED);
        inviteRepository.save(invite);
    }

    private InviteResponseDTO mapToResponse(OrganizationInviteEntity entity) {
        return new InviteResponseDTO(
                entity.getId(),
                entity.getOrganization().getId(),
                entity.getOrganization().getNome(),
                entity.getInviter().getNome(),
                entity.getRole(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                entity.getExpiresAt()
        );
    }
}
