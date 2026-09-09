package cl.duoc.cloud.pedidos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Cliente hacia ms-productos que reenvia el mismo token del usuario.
 *
 * Propagar la identidad en lugar de usar una credencial de servicio permite que
 * ms-productos siga aplicando sus propias reglas de autorizacion sobre quien
 * consulta, aunque la llamada venga de otro microservicio.
 */
@Service
public class CatalogoClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogoClient.class);

    private final RestClient cliente;
    private final String secretoGateway;

    public CatalogoClient(@Value("${servicios.productos-url}") String urlProductos,
            @Value("${seguridad.secreto-gateway:}") String secretoGateway) {
        this.cliente = RestClient.builder().baseUrl(urlProductos).build();
        this.secretoGateway = secretoGateway;
    }

    public boolean existeProducto(Long id) {
        String token = tokenActual();
        if (token == null) {
            return false;
        }
        try {
            cliente.get()
                    .uri("/api/v1/productos/{id}", id)
                    .header("Authorization", "Bearer " + token)
                    // ms-productos solo atiende peticiones que lleguen del API
                    // Gateway. Esta llamada es interna, asi que reenvia la misma
                    // cabecera; sin ella recibiria 403 y el producto se daria
                    // por inexistente.
                    .headers(h -> {
                        if (!secretoGateway.isBlank()) {
                            h.set("X-Origen-Gateway", secretoGateway);
                        }
                    })
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("ms-productos no confirmo el producto {}: {}", id, e.getMessage());
            return false;
        }
    }

    private String tokenActual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion instanceof JwtAuthenticationToken jwt ? jwt.getToken().getTokenValue() : null;
    }
}
