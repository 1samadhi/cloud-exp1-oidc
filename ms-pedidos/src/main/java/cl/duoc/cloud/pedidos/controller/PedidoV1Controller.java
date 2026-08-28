package cl.duoc.cloud.pedidos.controller;

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

import cl.duoc.cloud.pedidos.controller.dto.NuevoPedidoDTO;
import cl.duoc.cloud.pedidos.model.Pedido;
import cl.duoc.cloud.pedidos.service.CatalogoClient;
import cl.duoc.cloud.pedidos.service.PedidoService;

@RestController
@RequestMapping("/api/v1")
public class PedidoV1Controller {

    private final PedidoService servicio;
    private final CatalogoClient catalogo;

    public PedidoV1Controller(PedidoService servicio, CatalogoClient catalogo) {
        this.servicio = servicio;
        this.catalogo = catalogo;
    }

    @GetMapping("/public")
    public Map<String, String> publico() {
        return Map.of(
                "servicio", "ms-pedidos",
                "version", "2.1.0",
                "mensaje", "endpoint sin validacion de token");
    }

    /** El pedido se asocia al sujeto del token, no a un campo que mande el cliente. */
    @GetMapping("/pedidos")
    public List<Pedido> listar(@AuthenticationPrincipal Jwt jwt) {
        return servicio.listarPorCliente(jwt.getSubject());
    }

    @PostMapping("/pedidos")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribir') or hasRole('ADMIN')")
    public ResponseEntity<?> crear(@RequestBody NuevoPedidoDTO peticion,
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
        Pedido pedido = servicio.crear(jwt.getSubject(), peticion.productoId(), peticion.cantidad());
        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }
}
