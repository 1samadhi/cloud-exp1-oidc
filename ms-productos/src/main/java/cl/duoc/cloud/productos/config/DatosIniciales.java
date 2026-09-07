package cl.duoc.cloud.productos.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import cl.duoc.cloud.productos.model.Producto;
import cl.duoc.cloud.productos.repository.ProductoRepository;

/**
 * Siembra el catalogo la primera vez que arranca contra una base vacia.
 * Es idempotente: en los arranques siguientes encuentra datos y no hace nada,
 * asi que no duplica filas al redesplegar.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final ProductoRepository repositorio;

    public DatosIniciales(ProductoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void run(String... args) {
        if (repositorio.count() > 0) {
            return;
        }
        repositorio.save(new Producto("Teclado mecanico", 45990));
        repositorio.save(new Producto("Mouse inalambrico", 19990));
        repositorio.save(new Producto("Monitor 27 pulgadas", 189990));
        repositorio.save(new Producto("Audifonos con cancelacion de ruido", 89990));
        log.info("Catalogo inicial sembrado: {} productos", repositorio.count());
    }
}
