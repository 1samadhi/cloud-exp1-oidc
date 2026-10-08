package cl.duoc.cloud.ordenes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.cloud.ordenes.model.Orden;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    /**
     * Spring Data deriva el SQL del nombre del metodo:
     * select * from ordenes where cliente = ? order by creado desc
     */
    List<Orden> findByClienteOrderByCreadoDesc(String cliente);

    List<Orden> findAllByOrderByCreadoDesc();
}
