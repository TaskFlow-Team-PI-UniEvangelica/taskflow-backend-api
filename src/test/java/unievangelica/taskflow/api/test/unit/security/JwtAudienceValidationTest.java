package unievangelica.taskflow.api.test.unit.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica, com a auto-configuração real do Resource Server do Spring Boot e a propriedade
 * {@code audiences} lida do application.properties do projeto, que só tokens com "taskflow-api" no
 * {@code aud} são aceitos (tokens de outros clients do realm, como admin-cli e account, são recusados).
 */
public class JwtAudienceValidationTest {

    private static final String ISSUER = "http://localhost:9090/realms/taskflow-realm";
    private static final String PROP_AUDIENCES = "spring.security.oauth2.resourceserver.jwt.audiences";

    @TempDir
    static Path dir;

    private static NimbusJwtEncoder encoder;
    private static Path chavePublica;
    private static String audiencesDoProjeto;

    @BeforeAll
    static void prepararChaveEPropriedade() throws Exception {
        KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
        gerador.initialize(2048);
        KeyPair par = gerador.generateKeyPair();
        RSAKey rsa = new RSAKey.Builder((RSAPublicKey) par.getPublic())
                .privateKey((RSAPrivateKey) par.getPrivate()).build();
        encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsa)));

        chavePublica = dir.resolve("publica.pem");
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(par.getPublic().getEncoded());
        Files.writeString(chavePublica, "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----\n");

        Properties props = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
        audiencesDoProjeto = props.getProperty(PROP_AUDIENCES);
    }

    /** Segurança mínima só para o contexto subir; o que se testa é o JwtDecoder auto-configurado. */
    @Configuration
    @EnableWebSecurity
    static class SegurancaMinima {
        @Bean
        SecurityFilterChain cadeia(HttpSecurity http) throws Exception {
            return http.build();
        }
    }

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withUserConfiguration(SegurancaMinima.class)
            .withConfiguration(AutoConfigurations.of(OAuth2ResourceServerAutoConfiguration.class));

    private static String token(Object aud) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject("sub-teste")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
        if (aud != null) {
            claims.claim("aud", aud);
        }
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    private WebApplicationContextRunner comConfiguracaoDoProjeto() {
        return runner.withPropertyValues(
                // Sem issuer-uri: com ele o Boot faria a descoberta do emissor pela rede (Keycloak real)
                "spring.security.oauth2.resourceserver.jwt.public-key-location=file:" + chavePublica,
                PROP_AUDIENCES + "=" + audiencesDoProjeto);
    }

    @Test
    @DisplayName("application.properties exige a audiência taskflow-api (configurável por JWT_AUDIENCE)")
    void propriedadeDeAudienciaConfigurada() {
        assertThat(audiencesDoProjeto).isEqualTo("${JWT_AUDIENCE:taskflow-api}");
    }

    @Test
    @DisplayName("Token do frontend (aud com taskflow-api) é aceito")
    void tokenComAudienciaDaApiEAceito() {
        comConfiguracaoDoProjeto().run(ctx -> {
            JwtDecoder decoder = ctx.getBean(JwtDecoder.class);
            assertThat(decoder.decode(token(List.of("taskflow-api", "account"))).getAudience())
                    .contains("taskflow-api");
        });
    }

    @Test
    @DisplayName("Token de outro client (aud=account ou sem aud, como o admin-cli) é recusado")
    void tokenDeOutroClientERecusado() {
        comConfiguracaoDoProjeto().run(ctx -> {
            JwtDecoder decoder = ctx.getBean(JwtDecoder.class);
            JwtValidationException semApi = assertThrows(JwtValidationException.class,
                    () -> decoder.decode(token("account")));
            assertThat(semApi.getErrors()).anyMatch(e -> e.getDescription().contains("aud"));
            assertThrows(JwtValidationException.class, () -> decoder.decode(token(null)));
        });
    }
}
