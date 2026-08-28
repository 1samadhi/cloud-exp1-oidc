package cl.duoc.cloud.productos.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import cl.duoc.cloud.productos.model.Producto;

/**
 * Catalogo en memoria. La actividad permite simular el almacen de datos, asi que
 * no se levanta una base de datos: el foco de la experiencia es OIDC y el API Manager.
 */
@Service
public class ProductoService {

    private final List<Producto> catalogo = List.of(
            new Producto(1L, "Teclado mecanico", 45990),
            new Producto(2L, "Mouse inalambrico", 19990),
            new Producto(3L, "Monitor 27 pulgadas", 189990),
            new Producto(4L, "Audifonos con cancelacion de ruido", 89990));

    public List<Producto> listar() {
        return catalogo;
    }

    public Optional<Producto> buscarPorId(Long id) {
        return catalogo.stream().filter(p -> p.id().equals(id)).findFirst();
    }
}
