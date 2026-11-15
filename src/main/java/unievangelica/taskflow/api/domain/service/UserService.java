package unievangelica.taskflow.api.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unievangelica.taskflow.api.domain.exception.BusinessException;
import unievangelica.taskflow.api.domain.exception.ResourceNotFoundException;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.dto.request.UserRequestDTO;
import unievangelica.taskflow.api.dto.response.UserResponseDTO;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity buscarPorKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    public UserResponseDTO obterPerfilUsuario(UserEntity userLogado) {
        return converterParaDTO(userLogado);
    }

    @Transactional
    public UserResponseDTO atualizarUsuario(Long id, UserRequestDTO data) {
        UserEntity userLocal = userRepository.userFindById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Usuário não encontrado"));

        // Só o nome é editável; o e-mail é mantido (vem do Keycloak e autoriza convites)
        String novoNome = data.nome() != null ? data.nome() : userLocal.getNome();

        userRepository.updateUser(id, novoNome, userLocal.getEmail());

        userLocal.setNome(novoNome);

        return converterParaDTO(userLocal);
    }

    @Transactional
    public void deletarUsuario(Long id){
        if (!userRepository.userExistsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado");
        }
        userRepository.deleteUserById(id);
    }

    
    public UserEntity buscarPorId(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }


    @Transactional
    public void updateAvatar(String keycloakId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Arquivo de imagem vazio ou inválido.");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao processar imagem", e);
        }
        // O tipo é identificado pelo conteúdo (magic bytes), não pelo Content-Type enviado pelo cliente
        String detectedType = ImageTypeDetector.detect(content)
                .orElseThrow(() -> new BusinessException("Formato de imagem não suportado. Use JPEG, PNG, WEBP ou GIF."));
        userRepository.updateAvatar(keycloakId, content, detectedType);
    }

    
    @Transactional
    public void deleteAvatar(String keycloakId) {
        userRepository.removeAvatar(keycloakId);
    }

    
    @Transactional
    public void sincronizarNome(UserEntity user, String novoNome) {
        sincronizarPerfilKeycloak(user, novoNome, null, false);
    }

    /**
     * Espelha nome e e-mail do token no banco local. O e-mail só é copiado quando o Keycloak o
     * marca como verificado: um e-mail trocado e ainda não confirmado não pode virar o e-mail local,
     * porque é ele que autoriza convites.
     */
    @Transactional
    public void sincronizarPerfilKeycloak(UserEntity user, String novoNome, String novoEmail, boolean emailVerificado) {
        if (!emailVerificado) {
            novoEmail = null;
        }
        boolean alterado = false;
        String nomeFinal = user.getNome();
        String emailFinal = user.getEmail();

        if (novoNome != null && !novoNome.trim().isEmpty() && !novoNome.equals(user.getNome())) {
            nomeFinal = novoNome.trim();
            user.setNome(nomeFinal);
            alterado = true;
        }

        if (novoEmail != null && !novoEmail.trim().isEmpty() && !novoEmail.equalsIgnoreCase(user.getEmail())) {
            emailFinal = novoEmail.trim().toLowerCase();
            user.setEmail(emailFinal);
            alterado = true;
        }

        if (alterado) {
            userRepository.updateUser(user.getId(), nomeFinal, emailFinal);
        }
    }

    private UserResponseDTO converterParaDTO(UserEntity entidade) {
        return new UserResponseDTO(
                entidade.getId(),
                entidade.getNome(),
                entidade.getEmail(),
                "MEMBER"
        );
    }
}
