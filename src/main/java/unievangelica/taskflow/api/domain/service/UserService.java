package unievangelica.taskflow.api.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.dto.request.UserRequestDTO;
import unievangelica.taskflow.api.dto.response.UserResponseDTO;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity buscarPorKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    }

    @Transactional
    public UserResponseDTO criarUsuario(UserRequestDTO data){
        if (userRepository.userExistsByEmail(data.email())){
            throw new IllegalArgumentException("Email já cadastrado, tente outro.");
        }

        UserEntity novoUsuario = new UserEntity();
        novoUsuario.setNome(data.nome());
        novoUsuario.setEmail(data.email());

        UserEntity userLocal = userRepository.save(novoUsuario);

        return converterParaDTO(userLocal);
    }

    public List<UserResponseDTO> listarUsuarios() {
        return userRepository.userFindAll().stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    public UserResponseDTO obterPerfilUsuario(UserEntity userLogado) {
        return converterParaDTO(userLogado);
    }

    @Transactional
    public UserResponseDTO atualizarUsuario(Long id, UserRequestDTO data) {
        UserEntity userLocal = userRepository.userFindById(id)
                .orElseThrow(()-> new IllegalArgumentException("Usuário não encontrado"));

        String novoNome = data.nome() != null ? data.nome() : userLocal.getNome();
        String novoEmail = data.email() != null ? data.email() : userLocal.getEmail();

        userRepository.updateUser(id, novoNome, novoEmail);

        userLocal.setNome(novoNome);
        userLocal.setEmail(novoEmail);

        return converterParaDTO(userLocal);
    }

    @Transactional
    public void deletarUsuario(Long id){
        if (!userRepository.userExistsById(id)) {
            throw new IllegalArgumentException("Ususário não encontrado");
        }
        userRepository.deleteUserById(id);
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
