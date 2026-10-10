package unievangelica.taskflow.api.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationEntity;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationMemberEntity;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationMemberId;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationRole;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationMemberRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.dto.request.OrganizationRequestDTO;
import unievangelica.taskflow.api.dto.response.OrganizationResponseDTO;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrganizationService {

    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public OrganizationService(OrganizationMemberRepository organizationMemberRepository, 
                               OrganizationRepository organizationRepository,
                               UserRepository userRepository) {
        this.organizationMemberRepository = organizationMemberRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    public boolean hasPermissionTo(UserEntity user, Long organizationId, String action) {
        OrganizationMemberEntity member = organizationMemberRepository
            .findByOrganizationIdAndUserId(organizationId, user.getId())
            .orElse(null);

        if (member == null) return false;

        OrganizationRole role = member.getRole();
        OrganizationEntity org = member.getOrganization();

        if (role == OrganizationRole.OWNER || role == OrganizationRole.SUB_OWNER) {
            return true;
        }

        switch (action) {
            case "DELETE_TASK":
                return (role == OrganizationRole.MANAGER) || org.isMembersCanDeleteTasks();
            case "CHANGE_DEADLINE":
                return (role == OrganizationRole.MANAGER) || org.isMembersCanChangeDeadlines();
            case "RETURN_TASK":
                return (role == OrganizationRole.MANAGER) || org.isMembersCanReturnTasks();
            case "CREATE_TASK":
                return (role == OrganizationRole.MANAGER) || org.isMembersCanCreateTasks();
            case "UPDATE_ORG_SETTINGS":
                return false; // Only OWNER or SUB_OWNER (handled above) can update settings
            case "MANAGE_MEMBERS":
                return (role == OrganizationRole.MANAGER);
            default:
                return false;
        }
    }

    @Transactional
    public OrganizationResponseDTO criarOrganizacao(OrganizationRequestDTO data, UserEntity criadorLogado) {
        OrganizationEntity novaOrg = new OrganizationEntity();
        novaOrg.setNome(data.nome());
        
        if (data.membersCanDeleteTasks() != null) novaOrg.setMembersCanDeleteTasks(data.membersCanDeleteTasks());
        if (data.membersCanChangeDeadlines() != null) novaOrg.setMembersCanChangeDeadlines(data.membersCanChangeDeadlines());
        if (data.membersCanReturnTasks() != null) novaOrg.setMembersCanReturnTasks(data.membersCanReturnTasks());
        if (data.membersCanCreateTasks() != null) novaOrg.setMembersCanCreateTasks(data.membersCanCreateTasks());

        OrganizationEntity orgSalva = organizationRepository.save(novaOrg);

        // Adiciona o criador como OWNER
        OrganizationMemberEntity member = new OrganizationMemberEntity();
        member.setId(new OrganizationMemberId(orgSalva.getId(), criadorLogado.getId()));
        member.setOrganization(orgSalva);
        member.setUser(criadorLogado);
        member.setRole(OrganizationRole.OWNER);

        organizationMemberRepository.save(member);

        return converterParaDTO(orgSalva, OrganizationRole.OWNER);
    }

    @Transactional
    public OrganizationResponseDTO atualizarOrganizacao(Long id, OrganizationRequestDTO data, UserEntity userLogado) {
        if (!hasPermissionTo(userLogado, id, "UPDATE_ORG_SETTINGS")) {
            throw new SecurityException("Você não tem permissão para alterar as configurações desta organização. Requer nível OWNER ou SUB_OWNER.");
        }

        OrganizationEntity org = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada"));

        if (data.nome() != null) org.setNome(data.nome());
        if (data.membersCanDeleteTasks() != null) org.setMembersCanDeleteTasks(data.membersCanDeleteTasks());
        if (data.membersCanChangeDeadlines() != null) org.setMembersCanChangeDeadlines(data.membersCanChangeDeadlines());
        if (data.membersCanReturnTasks() != null) org.setMembersCanReturnTasks(data.membersCanReturnTasks());
        if (data.membersCanCreateTasks() != null) org.setMembersCanCreateTasks(data.membersCanCreateTasks());

        OrganizationEntity orgAtualizada = organizationRepository.save(org);

        OrganizationMemberEntity member = organizationMemberRepository.findByOrganizationIdAndUserId(id, userLogado.getId()).get();
        return converterParaDTO(orgAtualizada, member.getRole());
    }

    public List<OrganizationResponseDTO> listarOrganizacoesDoUsuario(UserEntity userLogado) {
        List<OrganizationMemberEntity> memberships = organizationMemberRepository.findByUserId(userLogado.getId());
        
        return memberships.stream()
                .map(m -> converterParaDTO(m.getOrganization(), m.getRole()))
                .collect(Collectors.toList());
    }

    public java.util.List<unievangelica.taskflow.api.dto.response.MemberResponseDTO> listarMembros(Long organizationId, UserEntity userLogado) {
        OrganizationEntity org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada"));
        
        // Verificar se o usuario atual eh membro da organizacao
        boolean isMember = org.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userLogado.getId()));
        if (!isMember) {
            throw new SecurityException("Você não tem acesso a esta organização.");
        }

        return org.getMembers().stream()
                .map(m -> new unievangelica.taskflow.api.dto.response.MemberResponseDTO(
                        m.getUser().getId(),
                        m.getUser().getNome(),
                        m.getUser().getEmail(),
                        m.getRole().name()
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void adicionarMembro(Long organizationId, Long userId, String roleStr, UserEntity userLogado) {
        if (!hasPermissionTo(userLogado, organizationId, "MANAGE_MEMBERS")) {
            throw new SecurityException("Você não tem permissão para gerenciar membros.");
        }

        OrganizationRole novoRole = OrganizationRole.valueOf(roleStr.toUpperCase());
        validarHierarquiaDeRoles(userLogado, organizationId, novoRole);

        OrganizationEntity org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada"));
        UserEntity novoUsuario = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        OrganizationMemberEntity member = new OrganizationMemberEntity();
        member.setId(new OrganizationMemberId(org.getId(), novoUsuario.getId()));
        member.setOrganization(org);
        member.setUser(novoUsuario);
        member.setRole(novoRole);

        organizationMemberRepository.save(member);
    }

    @Transactional
    public void adicionarMembroPorEmail(Long organizationId, String email, String roleStr, UserEntity userLogado) {
        if (!hasPermissionTo(userLogado, organizationId, "MANAGE_MEMBERS")) {
            throw new SecurityException("Você não tem permissão para gerenciar membros.");
        }

        OrganizationRole novoRole = OrganizationRole.valueOf(roleStr.toUpperCase());
        validarHierarquiaDeRoles(userLogado, organizationId, novoRole);

        OrganizationEntity org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada"));
        UserEntity novoUsuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado com o email informado"));

        OrganizationMemberEntity member = new OrganizationMemberEntity();
        member.setId(new OrganizationMemberId(org.getId(), novoUsuario.getId()));
        member.setOrganization(org);
        member.setUser(novoUsuario);
        member.setRole(novoRole);

        organizationMemberRepository.save(member);
    }

    @Transactional
    public void atualizarPapelMembro(Long organizationId, Long userId, String roleStr, UserEntity userLogado) {
        if (!hasPermissionTo(userLogado, organizationId, "MANAGE_MEMBERS")) {
            throw new SecurityException("Você não tem permissão para gerenciar membros.");
        }

        OrganizationRole novoRole = OrganizationRole.valueOf(roleStr.toUpperCase());
        validarHierarquiaDeRoles(userLogado, organizationId, novoRole);

        OrganizationMemberEntity member = organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Membro não encontrado nesta organização"));

        // Impedir que alguém altere o papel do próprio OWNER original ou rebaixe-o
        if (member.getRole() == OrganizationRole.OWNER) {
            throw new SecurityException("Não é permitido alterar o papel do OWNER principal.");
        }

        member.setRole(novoRole);
        organizationMemberRepository.save(member);
    }

    @Transactional
    public void transferirPosse(Long organizationId, Long novoOwnerId, UserEntity userLogado) {
        OrganizationMemberEntity requester = organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, userLogado.getId())
                .orElseThrow(() -> new IllegalArgumentException("Você não é membro desta organização."));

        if (requester.getRole() != OrganizationRole.OWNER) {
            throw new SecurityException("Apenas o OWNER pode transferir a posse da organização.");
        }

        OrganizationMemberEntity novoOwner = organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, novoOwnerId)
                .orElseThrow(() -> new IllegalArgumentException("Membro destino não encontrado nesta organização."));

        if (novoOwner.getUser().getId().equals(userLogado.getId())) {
            throw new IllegalArgumentException("Você já é o OWNER desta organização.");
        }

        if (novoOwner.getRole() != OrganizationRole.SUB_OWNER) {
            throw new SecurityException("A posse da organização só pode ser transferida para um SUB_OWNER.");
        }

        // O OWNER atual vira SUB_OWNER, e o alvo vira OWNER
        requester.setRole(OrganizationRole.SUB_OWNER);
        novoOwner.setRole(OrganizationRole.OWNER);

        organizationMemberRepository.save(requester);
        organizationMemberRepository.save(novoOwner);
    }

    @Transactional
    public void removerMembro(Long organizationId, Long userId, UserEntity userLogado) {
        boolean isSelfRemoval = userLogado.getId().equals(userId);

        if (!isSelfRemoval && !hasPermissionTo(userLogado, organizationId, "MANAGE_MEMBERS")) {
            throw new SecurityException("Você não tem permissão para remover membros.");
        }

        OrganizationMemberEntity memberTarget = organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Membro não encontrado nesta organização"));

        if (memberTarget.getRole() == OrganizationRole.OWNER) {
            throw new SecurityException("Não é possível remover o OWNER da organização.");
        }

        if (!isSelfRemoval) {
            OrganizationMemberEntity requester = organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, userLogado.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Você não é membro desta organização."));

            if (requester.getRole() == OrganizationRole.SUB_OWNER && memberTarget.getRole() == OrganizationRole.SUB_OWNER) {
                throw new SecurityException("SUB_OWNER não pode remover outro SUB_OWNER.");
            }
            if (requester.getRole() == OrganizationRole.MANAGER && memberTarget.getRole() != OrganizationRole.MEMBER) {
                throw new SecurityException("MANAGER só pode remover usuários com papel MEMBER.");
            }
        }

        organizationMemberRepository.delete(memberTarget);
    }

    private void validarHierarquiaDeRoles(UserEntity requester, Long orgId, OrganizationRole roleAlvo) {
        OrganizationMemberEntity requesterMember = organizationMemberRepository.findByOrganizationIdAndUserId(orgId, requester.getId()).get();
        OrganizationRole requesterRole = requesterMember.getRole();

        if (requesterRole == OrganizationRole.SUB_OWNER) {
            if (roleAlvo == OrganizationRole.OWNER || roleAlvo == OrganizationRole.SUB_OWNER) {
                throw new SecurityException("SUB_OWNER só pode gerenciar os papéis MANAGER e MEMBER.");
            }
        } else if (requesterRole == OrganizationRole.MANAGER) {
            if (roleAlvo != OrganizationRole.MEMBER) {
                throw new SecurityException("MANAGER só pode gerenciar o papel MEMBER.");
            }
        }
    }

    @Transactional
    public void deletarOrganizacao(Long organizationId, UserEntity userLogado) {
        OrganizationMemberEntity requester = organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, userLogado.getId())
                .orElseThrow(() -> new IllegalArgumentException("Você não é membro desta organização."));

        if (requester.getRole() != OrganizationRole.OWNER) {
            throw new SecurityException("Apenas o OWNER principal pode excluir a organização.");
        }

        OrganizationEntity org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada"));

        organizationRepository.delete(org);
    }

    private OrganizationResponseDTO converterParaDTO(OrganizationEntity org, OrganizationRole role) {
        return new OrganizationResponseDTO(
                org.getId(),
                org.getNome(),
                org.getCriadoEm(),
                org.isMembersCanDeleteTasks(),
                org.isMembersCanChangeDeadlines(),
                org.isMembersCanReturnTasks(),
                org.isMembersCanCreateTasks(),
                role.name()
        );
    }
}
