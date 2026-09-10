package cl.duoc.cloud.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracion de seguridad de ms-auth.
 *
 * El servicio dejo de ser un Identity Provider: la identidad la administra
 * Microsoft Entra ID. Lo que queda es un BFF con dos endpoints publicos, de modo
 * que no hay validacion de token que hacer aqui.
 *
 * El registro es anonimo por necesidad: quien crea su cuenta todavia no tiene
 * ninguna y por lo tanto no puede presentar un token. Quien si controla el
 * acceso es {@link FiltroOrigenGateway}, que exige que la peticion venga del
 * API Gateway.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/auth/registro", "/api/v1/estado").permitAll()
                        // Cualquier otra ruta se rechaza: no hay mas superficie
                        // que la que se declara arriba.
                        .anyRequest().denyAll());
        return http.build();
    }
}
