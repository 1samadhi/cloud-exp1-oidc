package cl.duoc.cloud.pedidos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.cloud.pedidos.controller.dto.NuevoPedidoDTO;
import cl.duoc.cloud.pedidos.model.Pedido;
import cl.duoc.cloud.pedidos.service.PedidoService;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoV1Controller {

    private final PedidoService servicio;

    public PedidoV1Controller(PedidoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Pedido> listar() {
        return servicio.listar();
    }

    @PostMapping
    public ResponseEntity<Pedido> crear(@RequestBody NuevoPedidoDTO peticion) {
        Pedido pedido = servicio.crear(peticion.cliente(), peticion.productoId(), peticion.cantidad());
        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }
}
