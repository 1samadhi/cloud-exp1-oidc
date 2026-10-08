package cl.duoc.cloud.carrito.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
        // Sin timeout, un catalogo caido dejaba el hilo de la peticion colgado
        // hasta que el cliente o el gateway cortaban la conexion.
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(2));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.cliente = RestClient.builder().baseUrl(urlProductos).requestFactory(fabrica).build();
        this.secretoGateway = secretoGateway;
    }

    /**
     * Confirma que el producto exista en el catalogo.
     *
     * Solo el 404 significa "no existe". Cualquier otra falla (401, 403, timeout,
     * servicio caido) se propaga como {@link CatalogoNoDisponibleException}: darla
     * por inexistente responderia 400 con un mensaje enganioso, y el usuario
     * creeria que el producto no esta cuando el problema es del sistema.
     */
    public boolean existeProducto(Long id) {
        String token = tokenActual();
        if (token == null) {
            throw new CatalogoNoDisponibleException(id,
                    new IllegalStateException("No hay token en el contexto de seguridad"));
        }
        try {
            cliente.get()
                    .uri("/api/v1/productos/{id}", id)
                    .header("Authorization", "Bearer " + token)
                    // ms-productos solo atiende peticiones que lleguen del API
                    // Gateway. Esta llamada es interna, asi que reenvia la misma
                    // cabecera; sin ella recibiria 403.
                    .headers(h -> {
                        if (!secretoGateway.isBlank()) {
                            h.set("X-Origen-Gateway", secretoGateway);
                        }
                    })
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (RestClientException e) {
            log.error("ms-productos no confirmo el producto {}: {}", id, e.getMessage());
            throw new CatalogoNoDisponibleException(id, e);
        }
    }

    private String tokenActual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion instanceof JwtAuthenticationToken jwt ? jwt.getToken().getTokenValue() : null;
    }
}
