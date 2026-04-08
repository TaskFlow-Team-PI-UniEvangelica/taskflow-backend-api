package unievangelica.taskflow.api.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.dto.request.PasswordChangeRequestDTO;
import unievangelica.taskflow.api.dto.request.UserRequestDTO;
import unievangelica.taskflow.api.dto.response.UserResponseDTO;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDTO criarUsuario(UserRequestDTO data){
        if (userRepository.userExistsByEmail(data.email())){
            throw new IllegalArgumentException("Email já cadastrado, tente outro.");
        }

        UserEntity novoUsuario = new UserEntity();
        novoUsuario.setNome(data.nome());
        novoUsuario.setEmail(data.email());
        novoUsuario.setSenha(passwordEncoder.encode(data.senha())); // usando a rota de criar usuário para criptografar a senha no salvamento como a rota de auth

        if (data.cargo() != null) {
            novoUsuario.setCargo(UserEntity.Cargo.valueOf(data.cargo()));
        } else {
            novoUsuario.setCargo(UserEntity.Cargo.funcionario);
        }

        UserEntity userLocal = userRepository.save(novoUsuario);

        return converterParaDTO(userLocal);
    }

    public List<UserResponseDTO> listarUsuarios() {
        return userRepository.userFindAll().stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    // esse método usa o security filter para obter informações do usuário por meio do token e retornar a visualização para o front end
    public UserResponseDTO obterPerfilUsuario(UserEntity userLogado) {
        return converterParaDTO(userLogado);
    }

    // anotação para garantir rollback nas operações que trabalham com alterações no banco de dados
    @Transactional
    public UserResponseDTO atualizarUsuario(Long id, UserRequestDTO data) {
        UserEntity userLocal = userRepository.userFindById(id)
                .orElseThrow(()-> new IllegalArgumentException("Usuário não encontrado"));

        // usando tenário para decidir se sera salvo o novo nome ou o nome antigo
        String novoNome = data.nome() != null ? data.nome() : userLocal.getNome();
        String novoEmail = data.email() != null ? data.email() : userLocal.getEmail();

        // usando a querry do repository para atualizar os dados
        userRepository.updateUser(id, novoNome, novoEmail);

        // definindo as variaveis para atualizar o objeto local para converter no DTO
        userLocal.setNome(novoNome);
        userLocal.setEmail(novoEmail);

        return converterParaDTO(userLocal);
    }

    @Transactional
    public void autualizarSenha(Long id, PasswordChangeRequestDTO data){
        UserEntity usuario = userRepository.userFindById(id)
                .orElseThrow(()-> new IllegalArgumentException("Usuário não encontrado")); // procura usuário pelo id

        if (!passwordEncoder.matches(data.senhaAtual(), usuario.getSenha())) { // se o hash da senha atual não bater gera uma exception
            throw new IllegalArgumentException("Senha atual esta incorreta, tente novamente");
        }

        // caso os hash sejam iguais o if libera para atualizar a senha encriptar e salvar a senha nova
        String novaSenhaEncriptada = passwordEncoder.encode(data.novaSenha());
        usuario.setSenha(novaSenhaEncriptada);

        userRepository.updateUserPassword(id, novaSenhaEncriptada);
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
                entidade.getCargo().name()
        );
    }
}
