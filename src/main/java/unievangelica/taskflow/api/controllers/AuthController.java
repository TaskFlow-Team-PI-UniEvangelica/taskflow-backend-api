package unievangelica.taskflow.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;
import unievangelica.taskflow.api.domain.persistence.repositories.UserRepository;
import unievangelica.taskflow.api.dto.request.AuthRequestDTO;
import unievangelica.taskflow.api.dto.request.RegisterRequestDTO;
import unievangelica.taskflow.api.dto.response.AuthResponseDTO;
import unievangelica.taskflow.api.infra.security.TokenService;

@RestController
@RequestMapping("/auth")
public class AuthController {
    // métodos auto injetados pelo spring
    @Autowired
    private AuthenticationManager autenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    // encapsula as informações recebidas pelo DTO e autentica usando os métodos de autenticação
    @PostMapping("/login")
    public ResponseEntity login(@RequestBody AuthRequestDTO data){
        var usernamepassword = new UsernamePasswordAuthenticationToken(data.email(), data.senha());
        var auth = this.autenticationManager.authenticate(usernamepassword); // compara as credenciais do usuário com o banco de dados

        var token = tokenService.gerarToken((UserEntity) auth.getPrincipal()); // gera o token

        return ResponseEntity.ok(new AuthResponseDTO(token)); // retorna o token
    }

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody RegisterRequestDTO data){
        if(this.userRepository.userFindByEmail(data.email()) != null) { // verifica se o email já existe no banco de dados
            return ResponseEntity.badRequest().build();
        }

        // se não existir salva a entidade com senha criptografada
        String senhaCriptografada = passwordEncoder.encode(data.senha());
        UserEntity novoUser = new UserEntity(data.nome(), data.email(), senhaCriptografada, data.cargo());

        this.userRepository.save(novoUser);

        return ResponseEntity.ok().build();
    }
}
