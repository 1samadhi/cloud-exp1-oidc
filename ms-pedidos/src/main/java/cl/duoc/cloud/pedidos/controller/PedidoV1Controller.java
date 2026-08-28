package cl.duoc.cloud.pedidos.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
