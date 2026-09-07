package cl.duoc.cloud.pedidos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.cloud.pedidos.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /**
     * Spring Data deriva el SQL del nombre del metodo:
     * select * from pedidos where cliente = ?
     */
    List<Pedido> findByClienteOrderByCreadoDesc(String cliente);
}
