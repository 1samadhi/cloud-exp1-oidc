package cl.duoc.cloud.productos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.cloud.productos.model.Producto;

/**
 * Spring Data genera la implementacion en el arranque: no hay SQL escrito a mano.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
