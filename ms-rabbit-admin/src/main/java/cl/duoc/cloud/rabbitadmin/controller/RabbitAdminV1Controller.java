package cl.duoc.cloud.rabbitadmin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.cloud.rabbitadmin.controller.dto.NuevaColaDTO;
import cl.duoc.cloud.rabbitadmin.controller.dto.NuevoBindingDTO;
import cl.duoc.cloud.rabbitadmin.controller.dto.NuevoExchangeDTO;
import cl.duoc.cloud.rabbitadmin.service.ParametroInvalidoException;
import cl.duoc.cloud.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;

/**
 * REST de administracion de RabbitMQ.
 *
 * No modela el dominio de pedidos360: es una herramienta operativa para crear
 * o borrar colas/exchanges/bindings sin entrar a la consola de management.
 *
 * Reservado al rol ADMIN: un usuario comun (rol USER) nunca deberia poder
 * tocar la topologia de mensajeria del sistema completo.
 */
@RestController
@RequestMapping("/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class RabbitAdminV1Controller {

    private final RabbitAdminService servicio;

    public RabbitAdminV1Controller(RabbitAdminService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/colas")
    public ResponseEntity<Void> crearCola(@Valid @RequestBody NuevaColaDTO datos) {
        servicio.crearCola(datos);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/colas/{nombre}")
    public ResponseEntity<Void> eliminarCola(@PathVariable String nombre) {
        boolean existia = servicio.eliminarCola(nombre);
        return existia ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<Void> crearExchange(@Valid @RequestBody NuevoExchangeDTO datos) {
        servicio.crearExchange(datos);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/exchanges/{nombre}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String nombre) {
        servicio.eliminarExchange(nombre);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    public ResponseEntity<Void> crearBinding(@Valid @RequestBody NuevoBindingDTO datos) {
        servicio.crearBinding(datos);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid @RequestBody NuevoBindingDTO datos) {
        servicio.eliminarBinding(datos);
        return ResponseEntity.noContent().build();
    }

    /** Un dato con forma correcta pero valor invalido (p. ej. tipo de exchange) es un 400, no un 500. */
    @ExceptionHandler(ParametroInvalidoException.class)
    public ResponseEntity<String> alRecibirParametroInvalido(ParametroInvalidoException error) {
        return ResponseEntity.badRequest().body(error.getMessage());
    }
}
