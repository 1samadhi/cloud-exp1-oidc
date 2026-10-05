package cl.duoc.cloud.carrito.service;

import java.util.List;

import org.springframework.stereotype.Service;

import cl.duoc.cloud.carrito.model.ItemCarrito;
import cl.duoc.cloud.carrito.repository.CarritoRepository;

/**
 * Items del carrito persistidos en la base de datos cloud.
 *
 * El cliente nunca llega como parametro desde fuera: lo inyecta el controlador
 * a partir del claim sub del token, de modo que nadie puede leer ni crear
 * items del carrito a nombre de otro.
 */
@Service
public class CarritoService {

    private final CarritoRepository repositorio;

    public CarritoService(CarritoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<ItemCarrito> listarPorCliente(String cliente) {
        return repositorio.findByClienteOrderByCreadoDesc(cliente);
    }

    /** Solo para administradores: ignora el filtro por cliente. */
    public List<ItemCarrito> listarTodos() {
        return repositorio.findAllByOrderByCreadoDesc();
    }

    public ItemCarrito crear(String cliente, Long productoId, int cantidad) {
        return repositorio.save(new ItemCarrito(cliente, productoId, cantidad));
    }
}
