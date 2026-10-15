package cl.duoc.cloud.facturacion.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.cloud.facturacion.model.Factura;
import cl.duoc.cloud.facturacion.repository.FacturaRepository;

@RestController
@RequestMapping("/api/v1")
public class FacturaV1Controller {

    private final FacturaRepository repositorio;

    public FacturaV1Controller(FacturaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> estado() {
        return ResponseEntity.ok(Map.of("servicio", "ms-facturacion"));
    }

    /** Evidencia para la demo: la boleta que genero el consumidor de orden.creada. */
    @GetMapping("/facturas/orden/{idOrden}")
    public ResponseEntity<Factura> buscarPorOrden(@PathVariable Long idOrden) {
        return repositorio.findByIdOrden(idOrden)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
