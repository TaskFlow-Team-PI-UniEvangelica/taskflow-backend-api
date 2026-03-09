package unievangelica.taskflow.api.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.dto.request.UserRequestDTO;
import unievangelica.taskflow.api.dto.response.UserResponseDTO;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public UserResponseDTO criarUsuario(UserRequestDTO data){
        if (userRepository.existsByEmail(data.email())){
            throw new IllegalArgumentException("Email já cadastrado, tente outro.");
        }

        UserEntity novoUsuario = new UserEntity();
        novoUsuario.setNome(data.nome());
        novoUsuario.setEmail(data.email());
        novoUsuario.setSenha(data.senha());

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

    private UserResponseDTO converterParaDTO(UserEntity entidade) {
        return new UserResponseDTO(
                entidade.getId(),
                entidade.getNome(),
                entidade.getEmail(),
                entidade.getCargo().name()
        );
    }
}
