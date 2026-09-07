package cl.duoc.cloud.productos.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import cl.duoc.cloud.productos.model.Producto;
import cl.duoc.cloud.productos.repository.ProductoRepository;

/**
 * Catalogo persistido en la base de datos cloud. Antes era una lista en memoria:
 * al reiniciar el contenedor se perdia el estado y cada replica veia datos
 * distintos.
 */
@Service
public class ProductoService {

    private final ProductoRepository repositorio;

    public ProductoService(ProductoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<Producto> listar() {
        return repositorio.findAll();
    }

    public Optional<Producto> buscarPorId(Long id) {
        return repositorio.findById(id);
    }
}
