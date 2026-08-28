package cl.duoc.cloud.productos.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.cloud.productos.model.Producto;
import cl.duoc.cloud.productos.service.ProductoService;

@RestController
@RequestMapping("/api/v1")
public class ProductoV1Controller {

    private final ProductoService servicio;

    public ProductoV1Controller(ProductoService servicio) {
        this.servicio = servicio;
    }

    /** Unica ruta abierta: sirve para comprobar el despliegue sin token. */
    @GetMapping("/public")
    public Map<String, String> publico() {
        return Map.of(
                "servicio", "ms-productos",
                "version", "2.1.0",
                "mensaje", "endpoint sin validacion de token");
    }

    @GetMapping("/productos")
    public List<Producto> listar() {
        return servicio.listar();
    }

    /**
     * Un id inexistente devolvia 500 porque orElseThrow() propagaba
     * NoSuchElementException. No es un fallo del servidor sino un recurso
     * ausente, asi que responde 404.
     */
    @GetMapping("/productos/{id}")
    public ResponseEntity<Producto> obtener(@PathVariable Long id) {
        return servicio.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Devuelve la identidad que viaja en el token, sin importar que IdP lo emitio. */
    @GetMapping("/productos/quien-soy")
    public Map<String, Object> quienSoy(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "emisor", jwt.getIssuer().toString(),
                "sujeto", jwt.getSubject(),
                "claims", jwt.getClaims());
    }
}
