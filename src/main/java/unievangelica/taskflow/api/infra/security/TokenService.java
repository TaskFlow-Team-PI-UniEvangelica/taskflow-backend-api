package unievangelica.taskflow.api.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import unievangelica.taskflow.api.domain.persistence.entities.UserEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    @Value("${api.security.token.secret}") // assinatura do token definida como variavel de ambiente
    private String secret;

    // método para gerar o token
    public String gerarToken(UserEntity user){
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret); // criptografia do token
            String token = JWT.create()
                    .withIssuer("API TaskFlow") // emissor do token
                    .withSubject(user.getEmail()) // quem é o dono do token
                    .withExpiresAt(tempoDeExpiracao()) // tempo de expiração do token
                    .sign(algorithm);
            return token;
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token", exception);
        }
    }

    // verifica se o token é do mesmo emissor e do mesmo dono linkado com o email para validar se ainda é valido
    public String validarToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("API TaskFlow")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) { // caso não seja válido retorna token invalido
            return "";
        }
    }

    // método que define o tempo de expiração para 30 minutos
    private Instant tempoDeExpiracao(){
        return LocalDateTime.now().plusMinutes(30).toInstant(ZoneOffset.of("-03:00"));
    }
}
