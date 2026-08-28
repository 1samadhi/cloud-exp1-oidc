package cl.duoc.cloud.productos.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.cloud.productos.model.Producto;
import cl.duoc.cloud.productos.service.ProductoService;

@RestController
@RequestMapping("/api/v1/productos")
public class ProductoV1Controller {

    private final ProductoService servicio;

    public ProductoV1Controller(ProductoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Producto> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Producto obtener(@PathVariable Long id) {
        return servicio.buscarPorId(id).orElseThrow();
    }
}
