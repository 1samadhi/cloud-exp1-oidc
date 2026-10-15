package cl.duoc.cloud.notificaciones.controller;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.cloud.notificaciones.controller.dto.PruebaCorreoDTO;
import cl.duoc.cloud.notificaciones.mensajeria.PublicadorEventos;
import cl.duoc.cloud.notificaciones.mensajeria.evento.OrdenCreadaEvento;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class NotificacionV1Controller {

    private final PublicadorEventos publicador;

    public NotificacionV1Controller(PublicadorEventos publicador) {
        this.publicador = publicador;
    }

    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> estado() {
        return ResponseEntity.ok(Map.of("servicio", "ms-notificaciones"));
    }

    /**
     * Publica un evento sintetico para probar el consumidor sin pasar por
     * ms-ordenes. Pensado para Postman, como pide el enunciado.
     */
    @PostMapping("/notificaciones/prueba")
    public ResponseEntity<Void> enviarCorreoDePrueba(@Valid @RequestBody PruebaCorreoDTO datos) {
        List<OrdenCreadaEvento.LineaEvento> items = datos.items() == null ? Collections.emptyList()
                : datos.items().stream()
                        .map(l -> new OrdenCreadaEvento.LineaEvento(l.idProducto(), l.cantidad(), l.precioUnitario()))
                        .toList();
        OrdenCreadaEvento evento = new OrdenCreadaEvento(
                datos.idOrden(), datos.idUsuario(), datos.correo(), items, datos.total(), Instant.now());
        publicador.publicarCorreoDePrueba(evento);
        return ResponseEntity.accepted().build();
    }
}
