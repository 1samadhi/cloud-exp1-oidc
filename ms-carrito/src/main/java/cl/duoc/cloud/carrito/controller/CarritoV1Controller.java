package cl.duoc.cloud.carrito.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.cloud.carrito.controller.dto.NuevoItemDTO;
import cl.duoc.cloud.carrito.model.ItemCarrito;
import cl.duoc.cloud.carrito.service.CatalogoClient;
import cl.duoc.cloud.carrito.service.CarritoService;

@RestController
@RequestMapping("/api/v1")
public class CarritoV1Controller {

    private final CarritoService servicio;
    private final CatalogoClient catalogo;

    public CarritoV1Controller(CarritoService servicio, CatalogoClient catalogo) {
        this.servicio = servicio;
        this.catalogo = catalogo;
    }

    @GetMapping("/public")
    public Map<String, String> publico() {
        return Map.of(
                "servicio", "ms-carrito",
                "version", "3.0.0",
                "mensaje", "endpoint sin validacion de token");
    }

    /** El item se asocia al sujeto del token, no a un campo que mande el cliente. */
    @GetMapping("/carrito")
    public List<ItemCarrito> listar(@AuthenticationPrincipal Jwt jwt) {
        return servicio.listarPorCliente(jwt.getSubject());
    }

    /**
     * Todos los items del carrito del sistema, no solo los del usuario del token.
     *
     * Reservado al rol ADMIN. Es el endpoint que demuestra el 403: un usuario
     * con token valido pero sin el rol recibe "prohibido", que es distinto del
     * 401 de quien no presenta token.
     */
    @GetMapping("/carrito/todos")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ItemCarrito> listarTodos() {
        return servicio.listarTodos();
    }

    @PostMapping("/carrito")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribir') or hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> crear(@RequestBody NuevoItemDTO peticion,
            @AuthenticationPrincipal Jwt jwt) {
        if (peticion.cantidad() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "La cantidad debe ser mayor que cero"));
        }
        // Propaga el token del usuario a ms-productos: la identidad viaja entre
        // servicios en lugar de confiar ciegamente en el llamador interno.
        if (!catalogo.existeProducto(peticion.productoId())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "El producto " + peticion.productoId() + " no existe en el catalogo"));
        }
        ItemCarrito item = servicio.crear(jwt.getSubject(), peticion.productoId(), peticion.cantidad());
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }
}
