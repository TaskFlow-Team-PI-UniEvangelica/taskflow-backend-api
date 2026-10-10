package unievangelica.taskflow.api.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final EmailService emailService;

    private UserEntity getAuthenticatedUser() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String keycloakId = jwt.getSubject();
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void createInvite(Long organizationId, OrganizationInviteRequestDTO request) {
        UserEntity inviter = getAuthenticatedUser();
        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        // Verificação se o cara já é membro, etc (simplificando)
        // Criar convite
        OrganizationInviteEntity invite = OrganizationInviteEntity.builder()
                .organization(organization)
                .inviter(inviter)
                .invitedEmail(request.getEmail())
                .role(request.getRole())
                .status(InviteStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        inviteRepository.save(invite);

        // Enviar email
        emailService.sendInviteEmail(request.getEmail(), inviter.getNome(), organization.getNome());
    }

    public List<InviteResponseDTO> getMyInvites() {
        UserEntity user = getAuthenticatedUser();
        return inviteRepository.findByInvitedEmailAndStatus(user.getEmail(), InviteStatus.PENDING).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void acceptInvite(Long inviteId) {
        UserEntity user = getAuthenticatedUser();
        OrganizationInviteEntity invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new RuntimeException("Invite not found"));

        if (!invite.getInvitedEmail().equalsIgnoreCase(user.getEmail())) {
            throw new RuntimeException("Not your invite");
        }
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new RuntimeException("Invite not pending");
        }

        invite.setStatus(InviteStatus.ACCEPTED);
        inviteRepository.save(invite);

        // Add to organization members
        OrganizationMemberEntity member = new OrganizationMemberEntity();
        member.setId(new OrganizationMemberId(invite.getOrganization().getId(), user.getId()));
        member.setOrganization(invite.getOrganization());
        member.setUser(user);
        member.setRole(OrganizationRole.valueOf(invite.getRole()));

        memberRepository.save(member);
    }

    @Transactional
    public void declineInvite(Long inviteId) {
        UserEntity user = getAuthenticatedUser();
        OrganizationInviteEntity invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new RuntimeException("Invite not found"));

        if (!invite.getInvitedEmail().equalsIgnoreCase(user.getEmail())) {
            throw new RuntimeException("Not your invite");
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
