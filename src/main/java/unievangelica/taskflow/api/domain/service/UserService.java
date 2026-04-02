package unievangelica.taskflow.api.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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

    public UserResponseDTO criarUsuario(UserRequestDTO data){
        if (userRepository.existsByEmail(data.email())){
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
        return userRepository.findAll().stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    public UserResponseDTO atualizarUsuario(Long id, UserRequestDTO data) {
        UserEntity userExiste = userRepository.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("Usuário não encontrado"));

        if (data.nome() != null) {
            userExiste.setNome(data.nome());
        }

        if (data.email() != null) {
            userExiste.setEmail(data.email());
        }

        UserEntity usuarioLocal = userRepository.save(userExiste);

        return converterParaDTO(usuarioLocal);
    }

    public void autualizarSenha(Long id, PasswordChangeRequestDTO data){
        UserEntity usuario = userRepository.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("Usuário não encontrado")); // procura usuário pelo id

        if (!passwordEncoder.matches(data.senhaAtual(), usuario.getSenha())) { // se o hash da senha atual não bater gera uma exception
            throw new IllegalArgumentException("Senha atual esta incorreta, tente novamente");
        }

        // caso os hash sejam iguais o if libera para atualizar a senha encriptar e salvar a senha nova
        String novaSenhaEncriptada = passwordEncoder.encode(data.novaSenha());
        usuario.setSenha(novaSenhaEncriptada);

        userRepository.save(usuario);
    }

    public void deletarUsuario(Long id){
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Ususário não encontrado");
        }
        userRepository.deleteById(id);
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
