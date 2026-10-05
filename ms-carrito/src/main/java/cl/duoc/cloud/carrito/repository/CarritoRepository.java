package cl.duoc.cloud.carrito.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.cloud.carrito.model.ItemCarrito;

public interface CarritoRepository extends JpaRepository<ItemCarrito, Long> {

    /**
     * Spring Data deriva el SQL del nombre del metodo:
     * select * from carrito_items where cliente = ?
     */
    List<ItemCarrito> findByClienteOrderByCreadoDesc(String cliente);

    List<ItemCarrito> findAllByOrderByCreadoDesc();
}
