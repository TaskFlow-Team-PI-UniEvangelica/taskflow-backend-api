package unievangelica.taskflow.api.infra.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration // diz que essa classe é uma classe de configuração
@EnableWebSecurity // habilita as configs do spring security dentro da classe
public class SpringSecurityConfig {
    @Autowired
    private SecurityFilter securityfilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ativa o cors nessa bagaça para usar o método novo la embaixo
                .csrf(csrf -> csrf.disable()) // desabilita o csrf para usar os tokens jwt
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // transforma a sessão em stateless para guardar e validar tokens
                .authorizeHttpRequests(authorize -> authorize
                        // rotas com os respectivos usuários que possuem acesso a elas
                        .requestMatchers("/error").permitAll() // liberando acesso a rota de erro
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/user/me/password").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/task/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/user/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/user/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/user/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/user/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(securityfilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // configurção do cors
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // define a url do frontend
        configuration.setAllowedOrigins(List.of("http://127.0.0.1:5173", "http://localhost:5173", "http://localhost"));

        // libera os métodos http q estou usando
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));

        // permite que o frontend envie os headers dos jsons e tokens
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // permite enviar as credenciais
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // aplicando as configs do cors para todas as rotas
        return source;
    }

    @Bean
    public AuthenticationManager autenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // método auxilizar para transformar a senha do usuário em hash
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}