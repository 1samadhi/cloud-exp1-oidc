package cl.duoc.cloud.pedidos.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import cl.duoc.cloud.pedidos.model.Pedido;

/**
 * Almacen en memoria de pedidos, coherente con la simulacion de datos de ms-productos.
 */
@Service
public class PedidoService {

    private final List<Pedido> pedidos = new CopyOnWriteArrayList<>();
    private final AtomicLong secuencia = new AtomicLong(1);

    public List<Pedido> listarPorCliente(String cliente) {
        return pedidos.stream().filter(p -> p.cliente().equals(cliente)).toList();
    }

    public Pedido crear(String cliente, Long productoId, int cantidad) {
        Pedido pedido = new Pedido(secuencia.getAndIncrement(), cliente, productoId, cantidad, Instant.now());
        pedidos.add(pedido);
        return pedido;
    }
}
