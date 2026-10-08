package cl.duoc.cloud.carrito.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * El claim {@code aud} no tiene un unico formato: Entra ID v2 lo emite como
 * cadena y Cognito como lista. Una version anterior del validador casteaba el
 * claim crudo a {@code List}, lo que reventaba con un ClassCastException y
 * devolvia 500 ante un token de Entra perfectamente valido.
 *
 * Estas pruebas fijan el comportamiento para que el arreglo no se pierda en un
 * refactor: cualquiera que vuelva a leer el claim crudo en lugar de
 * {@code getAudience()} las pone en rojo.
 */
class AudienciaDelTokenTest {

    private static final String AUDIENCIA = "api://c483f102-da2d-41b5-b48e-26e7ca78ab98";

    private static Jwt tokenCon(Object aud) {
        return Jwt.withTokenValue("token-de-prueba")
                .header("alg", "none")
                .claim("sub", "usuario-de-prueba")
                .claim("aud", aud)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    void unAudComoCadenaSeLeeComoUnaAudiencia() {
        assertThat(tokenCon(AUDIENCIA).getAudience()).containsExactly(AUDIENCIA);
    }

    @Test
    void unAudComoListaSeLeeIgual() {
        assertThat(tokenCon(List.of(AUDIENCIA)).getAudience()).containsExactly(AUDIENCIA);
    }

    @Test
    void unAudConVariasAudienciasLasDevuelveTodas() {
        assertThat(tokenCon(List.of(AUDIENCIA, "otra-api")).getAudience())
                .containsExactly(AUDIENCIA, "otra-api");
    }
}
