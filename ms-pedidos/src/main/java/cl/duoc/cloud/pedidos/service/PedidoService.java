package cl.duoc.cloud.pedidos.service;

import java.util.List;

import org.springframework.stereotype.Service;

import cl.duoc.cloud.pedidos.model.Pedido;
import cl.duoc.cloud.pedidos.repository.PedidoRepository;

/**
 * Pedidos persistidos en la base de datos cloud.
 *
 * El cliente nunca llega como parametro desde fuera: lo inyecta el controlador
 * a partir del claim sub del token, de modo que nadie puede leer ni crear
 * pedidos a nombre de otro.
 */
@Service
public class PedidoService {

    private final PedidoRepository repositorio;

    public PedidoService(PedidoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<Pedido> listarPorCliente(String cliente) {
        return repositorio.findByClienteOrderByCreadoDesc(cliente);
    }

    public Pedido crear(String cliente, Long productoId, int cantidad) {
        return repositorio.save(new Pedido(cliente, productoId, cantidad));
    }
}
