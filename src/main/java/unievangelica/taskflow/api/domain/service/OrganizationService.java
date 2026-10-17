package unievangelica.taskflow.api.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationEntity;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationMemberEntity;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationMemberId;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationRole;
import unievangelica.taskflow.api.domain.persistence.entities.TaskEntity;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationMemberRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.dto.request.OrganizationRequestDTO;
import unievangelica.taskflow.api.dto.response.OrganizationResponseDTO;

import java.util.List;
import java.util.Objects;
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

    private OrganizationMemberEntity resolveMembership(UserEntity user, TaskEntity task) {
        if (user == null || task == null || task.getOrganization() == null) {
            return null;
        }
        Long orgId = task.getOrganization().getId();
        OrganizationMemberEntity member = organizationMemberRepository
                .findByOrganizationIdAndUserId(orgId, user.getId())
                .orElse(null);

        if (member == null && task.getOrganization().getMembers() != null) {
            member = task.getOrganization().getMembers().stream()
                    .filter(m -> m.getUser() != null && Objects.equals(m.getUser().getId(), user.getId()))
                    .findFirst()
                    .orElse(null);
        }
        return member;
    }

    private boolean isCreatorOrAssignee(UserEntity user, TaskEntity task) {
        if (user == null || user.getId() == null || task == null) {
            return false;
        }
        boolean isCreator = task.getCriador() != null
                && task.getCriador().getId() != null
                && Objects.equals(task.getCriador().getId(), user.getId());
        if (isCreator) {
            return true;
        }
        return task.getResponsaveis() != null
                && task.getResponsaveis().stream()
                    .anyMatch(r -> r != null && Objects.equals(r.getId(), user.getId()));
    }

    /**
     * Verifica se o usuário pode excluir uma tarefa específica, considerando:
     * - Cargos acima de MEMBER (OWNER, SUB_OWNER, MANAGER) sempre podem excluir qualquer tarefa.
     * - Cargo MEMBER pode excluir qualquer tarefa se membersCanDeleteTasks=true,
     *   ou pode excluir apenas tarefas criadas por ele mesmo se membersCanCreateTasks=true e membersCanDeleteOwnTasks=true.
     */
    public boolean canDeleteTask(UserEntity user, TaskEntity task) {
        OrganizationMemberEntity member = resolveMembership(user, task);
        if (member == null) {
            return false;
        }

        OrganizationRole role = member.getRole();
        OrganizationEntity org = member.getOrganization() != null ? member.getOrganization() : task.getOrganization();

        if (role == OrganizationRole.OWNER || role == OrganizationRole.SUB_OWNER || role == OrganizationRole.MANAGER) {
            return true;
        }

        if (org.isMembersCanDeleteTasks()) {
            return true;
        }

        if (org.isMembersCanCreateTasks() && org.isMembersCanDeleteOwnTasks()) {
            return task.getCriador() != null
                    && task.getCriador().getId() != null
                    && task.getCriador().getId().equals(user.getId());
        }

        return false;
    }

    /**
     * Verifica se o usuário pode editar os dados de uma tarefa específica:
     * - Cargos acima de MEMBER (OWNER, SUB_OWNER, MANAGER) sempre podem editar qualquer tarefa.
     * - Cargo MEMBER pode editar qualquer tarefa se membersCanEditAllTasks=true,
     *   ou pode editar apenas tarefas que criou ou nas quais é responsável se membersCanEditOwnOrAssignedTasks=true.
     */
    public boolean canEditTask(UserEntity user, TaskEntity task) {
        OrganizationMemberEntity member = resolveMembership(user, task);
        if (member == null) {
            return false;
        }

        OrganizationRole role = member.getRole();
        OrganizationEntity org = member.getOrganization() != null ? member.getOrganization() : task.getOrganization();

        if (role == OrganizationRole.OWNER || role == OrganizationRole.SUB_OWNER || role == OrganizationRole.MANAGER) {
            return true;
        }

        if (org.isMembersCanEditAllTasks()) {
            return true;
        }

        if (org.isMembersCanEditOwnOrAssignedTasks()) {
            return isCreatorOrAssignee(user, task);
        }

        return false;
    }

    /**
     * Verifica se o usuário pode mover o status de uma tarefa específica:
     * - Cargos acima de MEMBER (OWNER, SUB_OWNER, MANAGER) sempre podem mover qualquer tarefa.
     * - Cargo MEMBER pode mover qualquer tarefa se membersCanMoveAllTasks=true,
     *   ou pode mover apenas tarefas que criou ou nas quais é responsável se membersCanMoveOwnOrAssignedTasks=true.
     */
    public boolean canMoveTask(UserEntity user, TaskEntity task) {
        OrganizationMemberEntity member = resolveMembership(user, task);
        if (member == null) {
            return false;
        }

        OrganizationRole role = member.getRole();
        OrganizationEntity org = member.getOrganization() != null ? member.getOrganization() : task.getOrganization();

        if (role == OrganizationRole.OWNER || role == OrganizationRole.SUB_OWNER || role == OrganizationRole.MANAGER) {
            return true;
        }

        if (org.isMembersCanMoveAllTasks()) {
            return true;
        }

        if (org.isMembersCanMoveOwnOrAssignedTasks()) {
            return isCreatorOrAssignee(user, task);
        }

        return false;
    }

    private Integer normalizeRetentionDays(Integer days) {
        if (days == null || days <= 0) {
            return null; // null representa retenção permanente (sem expiração)
        }
        return days;
    }

    private void aplicarEValidarRegrasDeGovernanca(OrganizationEntity org, OrganizationRequestDTO data) {
        if (data.membersCanCreateTasks() != null) {
            org.setMembersCanCreateTasks(data.membersCanCreateTasks());
        }
        if (data.membersCanChangeDeadlines() != null) {
            org.setMembersCanChangeDeadlines(data.membersCanChangeDeadlines());
        }

        // 1. Governança de Edição (Mutuamente Exclusiva: Editar Todas vs Editar Próprias/Responsável)
        if (Boolean.TRUE.equals(data.membersCanEditAllTasks()) && Boolean.TRUE.equals(data.membersCanEditOwnOrAssignedTasks())) {
            throw new IllegalArgumentException(
                "As opções de alterar todas as tarefas e alterar apenas tarefas próprias ou nas quais é responsável são mutuamente exclusivas."
            );
        }

        boolean nextEditAll = data.membersCanEditAllTasks() != null
                ? data.membersCanEditAllTasks()
                : org.isMembersCanEditAllTasks();
        boolean nextEditOwnOrAssigned = data.membersCanEditOwnOrAssignedTasks() != null
                ? data.membersCanEditOwnOrAssignedTasks()
                : org.isMembersCanEditOwnOrAssignedTasks();

        if (Boolean.TRUE.equals(data.membersCanEditAllTasks())) {
            nextEditAll = true;
            nextEditOwnOrAssigned = false;
        } else if (Boolean.TRUE.equals(data.membersCanEditOwnOrAssignedTasks())) {
            nextEditOwnOrAssigned = true;
            nextEditAll = false;
        }

        org.setMembersCanEditAllTasks(nextEditAll);
        org.setMembersCanEditOwnOrAssignedTasks(nextEditOwnOrAssigned);

        // 2. Governança de Movimentação (Mutuamente Exclusiva: Mover Todas vs Mover Próprias/Responsável)
        if (Boolean.TRUE.equals(data.membersCanMoveAllTasks()) && Boolean.TRUE.equals(data.membersCanMoveOwnOrAssignedTasks())) {
            throw new IllegalArgumentException(
                "As opções de mover todas as tarefas e mover apenas tarefas próprias ou nas quais é responsável são mutuamente exclusivas."
            );
        }

        boolean nextMoveAll = data.membersCanMoveAllTasks() != null
                ? data.membersCanMoveAllTasks()
                : org.isMembersCanMoveAllTasks();
        boolean nextMoveOwnOrAssigned = data.membersCanMoveOwnOrAssignedTasks() != null
                ? data.membersCanMoveOwnOrAssignedTasks()
                : org.isMembersCanMoveOwnOrAssignedTasks();

        if (Boolean.TRUE.equals(data.membersCanMoveAllTasks())) {
            nextMoveAll = true;
            nextMoveOwnOrAssigned = false;
        } else if (Boolean.TRUE.equals(data.membersCanMoveOwnOrAssignedTasks())) {
            nextMoveOwnOrAssigned = true;
            nextMoveAll = false;
        }

        org.setMembersCanMoveAllTasks(nextMoveAll);
        org.setMembersCanMoveOwnOrAssignedTasks(nextMoveOwnOrAssigned);

        // 3. Governança de Retorno de Tarefas Concluídas (Depende de ao menos uma opção de movimentação estar ligada)
        boolean anyMoveEnabled = nextMoveAll || nextMoveOwnOrAssigned;
        if (!anyMoveEnabled && Boolean.TRUE.equals(data.membersCanReturnTasks())) {
            throw new IllegalArgumentException(
                "Para permitir que membros retornem tarefas concluídas, ao menos uma opção de movimentação de tarefas deve estar ligada."
            );
        }

        boolean nextReturnTasks = data.membersCanReturnTasks() != null
                ? data.membersCanReturnTasks()
                : org.isMembersCanReturnTasks();
        if (!anyMoveEnabled) {
            nextReturnTasks = false;
        }
        org.setMembersCanReturnTasks(nextReturnTasks);

        // 4. Governança de Exclusão (Mutuamente Exclusiva e Dependente de Criar Tarefas)
        boolean nextDeleteAll = data.membersCanDeleteTasks() != null
                ? data.membersCanDeleteTasks()
                : org.isMembersCanDeleteTasks();
        boolean nextDeleteOwn = data.membersCanDeleteOwnTasks() != null
                ? data.membersCanDeleteOwnTasks()
                : org.isMembersCanDeleteOwnTasks();

        if (Boolean.TRUE.equals(data.membersCanDeleteTasks()) && Boolean.TRUE.equals(data.membersCanDeleteOwnTasks())) {
            throw new IllegalArgumentException(
                "As opções de excluir todas as tarefas e excluir apenas as próprias tarefas são mutuamente exclusivas."
            );
        }

        if (Boolean.TRUE.equals(data.membersCanDeleteTasks())) {
            nextDeleteAll = true;
            nextDeleteOwn = false;
        } else if (Boolean.TRUE.equals(data.membersCanDeleteOwnTasks())) {
            if (!org.isMembersCanCreateTasks()) {
                throw new IllegalArgumentException(
                    "Para permitir que membros excluam suas próprias tarefas, a opção de criar tarefas deve estar obrigatoriamente ligada."
                );
            }
            nextDeleteOwn = true;
            nextDeleteAll = false;
        }

        if (!org.isMembersCanCreateTasks()) {
            nextDeleteOwn = false;
        }

        org.setMembersCanDeleteTasks(nextDeleteAll);
        org.setMembersCanDeleteOwnTasks(nextDeleteOwn);
    }

    @Transactional
    public OrganizationResponseDTO criarOrganizacao(OrganizationRequestDTO data, UserEntity criadorLogado) {
        OrganizationEntity novaOrg = new OrganizationEntity();
        novaOrg.setNome(data.nome());
        
        aplicarEValidarRegrasDeGovernanca(novaOrg, data);

        if (data.deletedTasksRetentionDays() != null) {
            novaOrg.setDeletedTasksRetentionDays(normalizeRetentionDays(data.deletedTasksRetentionDays()));
        }

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

        OrganizationMemberEntity member = organizationMemberRepository
                .findByOrganizationIdAndUserId(id, userLogado.getId())
                .orElseThrow(() -> new SecurityException("Você não pertence a esta organização."));

        if (data.nome() != null) org.setNome(data.nome());

        aplicarEValidarRegrasDeGovernanca(org, data);

        if (data.deletedTasksRetentionDays() != null) {
            Integer normalizedNewRetention = normalizeRetentionDays(data.deletedTasksRetentionDays());
            if (!Objects.equals(org.getDeletedTasksRetentionDays(), normalizedNewRetention)) {
                if (member.getRole() != OrganizationRole.OWNER) {
                    throw new SecurityException("Apenas o OWNER da organização pode decidir a persistência do histórico de tarefas excluídas.");
                }
                org.setDeletedTasksRetentionDays(normalizedNewRetention);
            }
        }

        OrganizationEntity orgAtualizada = organizationRepository.save(org);

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

        if (organizationMemberRepository.findByOrganizationIdAndUserId(org.getId(), novoUsuario.getId()).isPresent()) {
            throw new IllegalArgumentException("Este usuário já é membro da organização.");
        }

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

        if (organizationMemberRepository.findByOrganizationIdAndUserId(org.getId(), novoUsuario.getId()).isPresent()) {
            throw new IllegalArgumentException("Este usuário já é membro da organização.");
        }

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

        OrganizationMemberEntity requester = organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, userLogado.getId())
                .orElseThrow(() -> new IllegalArgumentException("Você não é membro desta organização."));
        if (requester.getRole() == OrganizationRole.SUB_OWNER && member.getRole() == OrganizationRole.SUB_OWNER) {
            throw new SecurityException("SUB_OWNER não pode alterar o papel de outro SUB_OWNER.");
        }
        if (requester.getRole() == OrganizationRole.MANAGER && member.getRole() != OrganizationRole.MEMBER) {
            throw new SecurityException("MANAGER só pode gerenciar usuários com papel MEMBER.");
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

    public void validarHierarquiaDeRoles(UserEntity requester, Long orgId, OrganizationRole roleAlvo) {
        if (roleAlvo == OrganizationRole.OWNER) {
            throw new SecurityException("O papel de OWNER só pode ser atribuído via transferência de posse.");
        }

        OrganizationMemberEntity requesterMember = organizationMemberRepository.findByOrganizationIdAndUserId(orgId, requester.getId())
                .orElseThrow(() -> new SecurityException("Você não pertence a esta organização."));
        OrganizationRole requesterRole = requesterMember.getRole();

        if (requesterRole == OrganizationRole.SUB_OWNER) {
            if (roleAlvo == OrganizationRole.SUB_OWNER) {
                throw new SecurityException("SUB_OWNER só pode gerenciar os papéis MANAGER e MEMBER.");
            }
        } else if (requesterRole == OrganizationRole.MANAGER) {
            if (roleAlvo != OrganizationRole.MEMBER) {
                throw new SecurityException("MANAGER só pode gerenciar o papel MEMBER.");
            }
        } else if (requesterRole == OrganizationRole.MEMBER) {
            throw new SecurityException("MEMBER não possui permissão para gerenciar papéis.");
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
                org.isMembersCanDeleteOwnTasks(),
                org.isMembersCanEditAllTasks(),
                org.isMembersCanEditOwnOrAssignedTasks(),
                org.isMembersCanMoveAllTasks(),
                org.isMembersCanMoveOwnOrAssignedTasks(),
                org.isMembersCanChangeDeadlines(),
                org.isMembersCanReturnTasks(),
                org.isMembersCanCreateTasks(),
                org.getDeletedTasksRetentionDays(),
                role.name()
        );
    }
}
