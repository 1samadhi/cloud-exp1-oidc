package cl.duoc.cloud.facturacion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.cloud.facturacion.model.Factura;

public interface FacturaRepository extends JpaRepository<Factura, Long> {
    Optional<Factura> findByIdOrden(Long idOrden);
}
