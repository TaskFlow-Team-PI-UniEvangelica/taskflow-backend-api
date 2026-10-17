package unievangelica.taskflow.api.test.unit.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationEntity;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationMemberEntity;
import unievangelica.taskflow.api.domain.persistence.entities.OrganizationRole;
import unievangelica.taskflow.api.domain.persistence.entities.TaskEntity;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationMemberRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.OrganizationRepository;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.domain.service.OrganizationService;
import unievangelica.taskflow.api.dto.request.OrganizationRequestDTO;
import unievangelica.taskflow.api.dto.response.OrganizationResponseDTO;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrganizationServiceTest {

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private UserEntity owner;
    private UserEntity member;
    private OrganizationEntity organization;

    @BeforeEach
    void setUp() {
        owner = new UserEntity();
        owner.setId(1L);
        owner.setNome("Owner User");

        member = new UserEntity();
        member.setId(2L);
        member.setNome("Normal Member");

        organization = new OrganizationEntity();
        organization.setId(10L);
        organization.setNome("My Org");
        organization.setMembersCanCreateTasks(false);
    }

    @Test
    @DisplayName("Deve criar uma nova organização e atribuir OWNER ao criador")
    void testCriarOrganizacao() {
        OrganizationRequestDTO request = new OrganizationRequestDTO("New Org", true, true, true, true);
        
        when(organizationRepository.save(any(OrganizationEntity.class))).thenAnswer(invocation -> {
            OrganizationEntity org = invocation.getArgument(0);
            org.setId(10L);
            return org;
        });

        OrganizationResponseDTO response = organizationService.criarOrganizacao(request, owner);

        assertNotNull(response);
        assertEquals("New Org", response.nome());
        assertEquals("OWNER", response.myRole());
        verify(organizationMemberRepository, times(1)).save(any(OrganizationMemberEntity.class));
    }

    @Test
    @DisplayName("Deve bloquear ativação simultânea de excluir todas as tasks e excluir apenas as próprias tasks")
    void testBloqueiaExclusaoMutuamenteExclusiva() {
        OrganizationRequestDTO invalidRequest = new OrganizationRequestDTO(
                "Org Inválida", true, true, false, false, true, 7
        );

        assertThrows(IllegalArgumentException.class, () -> {
            organizationService.criarOrganizacao(invalidRequest, owner);
        });
    }

    @Test
    @DisplayName("Deve bloquear excluir apenas próprias tasks se a opção de criar tasks estiver desligada")
    void testBloqueiaExclusaoPropriaSemCriarTaskLigado() {
        OrganizationRequestDTO invalidRequest = new OrganizationRequestDTO(
                "Org Sem Criação", false, true, false, false, false, 7
        );

        assertThrows(IllegalArgumentException.class, () -> {
            organizationService.criarOrganizacao(invalidRequest, owner);
        });
    }

    @Test
    @DisplayName("canDeleteTask: MEMBER pode excluir apenas sua própria task quando membersCanDeleteOwnTasks=true")
    void testCanDeleteTaskMemberApenasPropriaTask() {
        organization.setMembersCanCreateTasks(true);
        organization.setMembersCanDeleteOwnTasks(true);
        organization.setMembersCanDeleteTasks(false);

        OrganizationMemberEntity membership = new OrganizationMemberEntity();
        membership.setUser(member);
        membership.setOrganization(organization);
        membership.setRole(OrganizationRole.MEMBER);

        when(organizationMemberRepository.findByOrganizationIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(membership));

        TaskEntity ownTask = new TaskEntity();
        ownTask.setId(100L);
        ownTask.setOrganization(organization);
        ownTask.setCriador(member);

        TaskEntity otherUserTask = new TaskEntity();
        otherUserTask.setId(101L);
        otherUserTask.setOrganization(organization);
        otherUserTask.setCriador(owner);

        assertTrue(organizationService.canDeleteTask(member, ownTask));
        assertFalse(organizationService.canDeleteTask(member, otherUserTask));
    }

    @Test
    @DisplayName("Atualizar Organização: SUB_OWNER não pode alterar a persistência (deletedTasksRetentionDays)")
    void testSubOwnerNaoPodeAlterarRetencaoDeExcluidas() {
        organization.setDeletedTasksRetentionDays(7);

        OrganizationMemberEntity subOwnerMembership = new OrganizationMemberEntity();
        subOwnerMembership.setUser(member);
        subOwnerMembership.setOrganization(organization);
        subOwnerMembership.setRole(OrganizationRole.SUB_OWNER);

        when(organizationMemberRepository.findByOrganizationIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(subOwnerMembership));
        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(organization));

        OrganizationRequestDTO request = new OrganizationRequestDTO(
                "My Org", false, false, false, false, true, 30
        );

        assertThrows(SecurityException.class, () -> {
            organizationService.atualizarOrganizacao(10L, request, member);
        });
    }

    @Test
    @DisplayName("HasPermissionTo: OWNER tem permissão para tudo")
    void testHasPermissionToOwner() {
        OrganizationMemberEntity membership = new OrganizationMemberEntity();
        membership.setUser(owner);
        membership.setOrganization(organization);
        membership.setRole(OrganizationRole.OWNER);

        when(organizationMemberRepository.findByOrganizationIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(membership));

        boolean hasPerm = organizationService.hasPermissionTo(owner, 10L, "DELETE_TASK");
        assertTrue(hasPerm);

        hasPerm = organizationService.hasPermissionTo(owner, 10L, "UPDATE_ORG_SETTINGS");
        assertTrue(hasPerm);
    }

    @Test
    @DisplayName("HasPermissionTo: MEMBER sem permissão de criar task")
    void testHasPermissionToMemberSemPermissao() {
        OrganizationMemberEntity membership = new OrganizationMemberEntity();
        membership.setUser(member);
        membership.setOrganization(organization);
        membership.setRole(OrganizationRole.MEMBER); // org.isMembersCanCreateTasks() is false from setUp

        when(organizationMemberRepository.findByOrganizationIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(membership));

        boolean hasPerm = organizationService.hasPermissionTo(member, 10L, "CREATE_TASK");
        assertFalse(hasPerm);
    }

    @Test
    @DisplayName("HasPermissionTo: MEMBER com permissão de criar task pelas configurações")
    void testHasPermissionToMemberComPermissao() {
        organization.setMembersCanCreateTasks(true);

        OrganizationMemberEntity membership = new OrganizationMemberEntity();
        membership.setUser(member);
        membership.setOrganization(organization);
        membership.setRole(OrganizationRole.MEMBER);

        when(organizationMemberRepository.findByOrganizationIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(membership));

        boolean hasPerm = organizationService.hasPermissionTo(member, 10L, "CREATE_TASK");
        assertTrue(hasPerm);
    }

    @Test
    @DisplayName("Atualizar Organização: Falha se não tiver permissão")
    void testAtualizarOrganizacaoSemPermissao() {
        OrganizationMemberEntity membership = new OrganizationMemberEntity();
        membership.setUser(member);
        membership.setOrganization(organization);
        membership.setRole(OrganizationRole.MEMBER);

        when(organizationMemberRepository.findByOrganizationIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(membership));

        OrganizationRequestDTO request = new OrganizationRequestDTO("Updated Name", null, null, null, null);

        assertThrows(SecurityException.class, () -> {
            organizationService.atualizarOrganizacao(10L, request, member);
        });
    }

    @Test
    @DisplayName("Adicionar Membro: Falha de hierarquia se SUB_OWNER tentar adicionar OWNER")
    void testAdicionarMembroFalhaHierarquia() {
        OrganizationMemberEntity subOwnerMembership = new OrganizationMemberEntity();
        subOwnerMembership.setUser(owner);
        subOwnerMembership.setOrganization(organization);
        subOwnerMembership.setRole(OrganizationRole.SUB_OWNER);

        when(organizationMemberRepository.findByOrganizationIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(subOwnerMembership));

        assertThrows(SecurityException.class, () -> {
            organizationService.adicionarMembro(10L, 2L, "OWNER", owner);
        });
    }

    @Test
    @DisplayName("Transferir Posse: Sucesso se alvo for SUB_OWNER")
    void testTransferirPosseSucesso() {
        OrganizationMemberEntity ownerMembership = new OrganizationMemberEntity();
        ownerMembership.setUser(owner);
        ownerMembership.setRole(OrganizationRole.OWNER);

        OrganizationMemberEntity subOwnerMembership = new OrganizationMemberEntity();
        subOwnerMembership.setUser(member);
        subOwnerMembership.setRole(OrganizationRole.SUB_OWNER);

        when(organizationMemberRepository.findByOrganizationIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMembership));
        when(organizationMemberRepository.findByOrganizationIdAndUserId(10L, 2L)).thenReturn(Optional.of(subOwnerMembership));

        organizationService.transferirPosse(10L, 2L, owner);

        assertEquals(OrganizationRole.SUB_OWNER, ownerMembership.getRole());
        assertEquals(OrganizationRole.OWNER, subOwnerMembership.getRole());
        verify(organizationMemberRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("Transferir Posse: Falha se alvo nao for SUB_OWNER")
    void testTransferirPosseFalhaNaoSubOwner() {
        OrganizationMemberEntity ownerMembership = new OrganizationMemberEntity();
        ownerMembership.setUser(owner);
        ownerMembership.setRole(OrganizationRole.OWNER);

        OrganizationMemberEntity memberMembership = new OrganizationMemberEntity();
        memberMembership.setUser(member);
        memberMembership.setRole(OrganizationRole.MEMBER);

        when(organizationMemberRepository.findByOrganizationIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMembership));
        when(organizationMemberRepository.findByOrganizationIdAndUserId(10L, 2L)).thenReturn(Optional.of(memberMembership));

        assertThrows(SecurityException.class, () -> {
            organizationService.transferirPosse(10L, 2L, owner);
        });
    }

    @Test
    @DisplayName("Deletar Organização: Sucesso sendo OWNER")
    void testDeletarOrganizacaoSucesso() {
        OrganizationMemberEntity ownerMembership = new OrganizationMemberEntity();
        ownerMembership.setUser(owner);
        ownerMembership.setRole(OrganizationRole.OWNER);

        when(organizationMemberRepository.findByOrganizationIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMembership));
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));

        organizationService.deletarOrganizacao(10L, owner);

        verify(organizationRepository, times(1)).delete(organization);
    }

    @Test
    @DisplayName("Deletar Organização: Falha se nao for OWNER")
    void testDeletarOrganizacaoFalhaNaoOwner() {
        OrganizationMemberEntity memberMembership = new OrganizationMemberEntity();
        memberMembership.setUser(member);
        memberMembership.setRole(OrganizationRole.MEMBER);

        when(organizationMemberRepository.findByOrganizationIdAndUserId(10L, 2L)).thenReturn(Optional.of(memberMembership));

        assertThrows(SecurityException.class, () -> {
            organizationService.deletarOrganizacao(10L, member);
        });
    }
}
