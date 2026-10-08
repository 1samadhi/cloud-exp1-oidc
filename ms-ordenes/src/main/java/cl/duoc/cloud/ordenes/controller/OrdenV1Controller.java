package cl.duoc.cloud.ordenes.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.cloud.ordenes.controller.dto.NuevaOrdenDTO;
import cl.duoc.cloud.ordenes.model.Orden;
import cl.duoc.cloud.ordenes.service.CatalogoNoDisponibleException;
import cl.duoc.cloud.ordenes.service.OrdenService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class OrdenV1Controller {

    private final OrdenService servicio;

    public OrdenV1Controller(OrdenService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/public")
    public Map<String, String> publico() {
        return Map.of(
                "servicio", "ms-ordenes",
                "version", "1.0.0",
                "mensaje", "endpoint sin validacion de token");
    }

    /** La orden se asocia al sujeto del token, no a un campo que mande el cliente. */
    @GetMapping("/ordenes")
    public List<Orden> listar(@AuthenticationPrincipal Jwt jwt) {
        return servicio.listarPorCliente(jwt.getSubject());
    }

    /**
     * Todas las ordenes del sistema, no solo las del usuario del token.
     *
     * Reservado al rol ADMIN: es el endpoint que demuestra el 403 frente al 401
     * de quien no presenta token.
     */
    @GetMapping("/ordenes/todas")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Orden> listarTodas() {
        return servicio.listarTodas();
    }

    /**
     * Confirma la compra: registra la orden y publica orden.creada.
     *
     * El correo sale del token (claim email o preferred_username) para que
     * ms-notificaciones no tenga que consultar al IdP.
     */
    @PostMapping("/ordenes")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribir') or hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Orden> crear(@Valid @RequestBody NuevaOrdenDTO peticion,
            @AuthenticationPrincipal Jwt jwt) {
        Orden orden = servicio.crear(jwt.getSubject(), correoDe(jwt), peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(orden);
    }

    /** Un producto inexistente es error del cliente, no del servidor: 400. */
    @ExceptionHandler(OrdenService.OrdenInvalida.class)
    public ResponseEntity<Map<String, String>> ordenInvalida(OrdenService.OrdenInvalida e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    /**
     * Un catalogo inaccesible no es una orden invalida: 502, no el 400
     * enganioso de "el producto no existe".
     *
     * Diagnostico original de Diego Villota en la rama fix/auditoria-v9.
     */
    @ExceptionHandler(CatalogoNoDisponibleException.class)
    public ResponseEntity<Map<String, String>> catalogoNoDisponible(CatalogoNoDisponibleException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "El catalogo de productos no esta disponible"));
    }

    private String correoDe(Jwt jwt) {
        String correo = jwt.getClaimAsString("email");
        return correo != null ? correo : jwt.getClaimAsString("preferred_username");
    }
}
