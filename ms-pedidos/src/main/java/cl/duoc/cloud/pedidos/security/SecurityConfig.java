package cl.duoc.cloud.pedidos.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resource Server OAuth 2.0 con validacion multi emisor.
 *
 * El servicio no guarda contrasenias ni sabe autenticar: solo verifica la firma
 * del JWT que recibe contra las llaves publicas del emisor que lo firmo. La
 * lista de emisores confiables es configuracion, no codigo, para poder sumar
 * Cognito o Entra ID sin recompilar.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final List<String> emisoresConfiables;
    private final Map<String, AuthenticationManager> gestores = new ConcurrentHashMap<>();

    public SecurityConfig(@Value("${seguridad.emisores}") List<String> emisoresConfiables) {
        this.emisoresConfiables = emisoresConfiables.stream()
                .map(String::trim)
                .filter(e -> !e.isEmpty())
                .toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/api/v1/public").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationManagerResolver(resolverDeEmisores()));
        return http.build();
    }

    /**
     * Elige el validador segun el claim "iss" del token entrante. Un emisor que
     * no este en la lista blanca se rechaza con 401 antes de tocar la red.
     */
    @Bean
    public AuthenticationManagerResolver<HttpServletRequest> resolverDeEmisores() {
        return new JwtIssuerAuthenticationManagerResolver(emisor -> {
            if (!emisoresConfiables.contains(emisor)) {
                throw new InvalidBearerTokenException("Emisor no confiable: " + emisor);
            }
            return gestores.computeIfAbsent(emisor, this::crearGestor);
        });
    }

    private AuthenticationManager crearGestor(String emisor) {
        // Descubre el jwks_uri leyendo /.well-known/openid-configuration del emisor
        JwtDecoder decodificador = JwtDecoders.fromIssuerLocation(emisor);
        JwtAuthenticationProvider proveedor = new JwtAuthenticationProvider(decodificador);
        proveedor.setJwtAuthenticationConverter(convertidorDeAutoridades());
        return proveedor::authenticate;
    }

    /**
     * Normaliza los permisos de los tres emisores a un mismo vocabulario:
     * scopes como SCOPE_x y roles o grupos como ROLE_x.
     */
    private Converter<Jwt, AbstractAuthenticationToken> convertidorDeAutoridades() {
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> autoridades = new ArrayList<>();

            // "scope" lo usan el IdP propio y Cognito; "scp" lo usa Entra ID
            String scopes = jwt.hasClaim("scope") ? jwt.getClaimAsString("scope")
                    : jwt.getClaimAsString("scp");
            if (scopes != null) {
                for (String s : scopes.split(" ")) {
                    if (!s.isBlank()) {
                        autoridades.add(new SimpleGrantedAuthority("SCOPE_" + s));
                    }
                }
            }

            // "roles" en el IdP propio y en Entra ID; "cognito:groups" en Cognito
            for (String claim : List.of("roles", "cognito:groups")) {
                List<String> valores = jwt.getClaimAsStringList(claim);
                if (valores != null) {
                    valores.stream()
                            .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                            .map(SimpleGrantedAuthority::new)
                            .forEach(autoridades::add);
                }
            }
            return autoridades;
        });
        return convertidor;
    }
}
