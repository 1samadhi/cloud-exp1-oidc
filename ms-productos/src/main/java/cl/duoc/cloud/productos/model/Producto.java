package cl.duoc.cloud.productos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA del catalogo. Deja de ser un record porque JPA necesita una clase
 * mutable con constructor sin argumentos para poder instanciarla al leer filas.
 */
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false)
    private int precio;

    /**
     * Unidades disponibles. Lo descuenta el consumidor de stock al procesar
     * "orden.creada"; nunca se escribe desde un endpoint REST expuesto al
     * cliente.
     */
    @Column(nullable = false)
    private int stock;

    /** Requerido por JPA. No se usa desde el codigo de la aplicacion. */
    protected Producto() {
    }

    public Producto(String nombre, int precio, int stock) {
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getPrecio() {
        return precio;
    }

    public int getStock() {
        return stock;
    }

    /**
     * Descuenta unidades del stock.
     *
     * @throws StockInsuficienteException si no alcanza; es un error de negocio,
     *         no transitorio: reintentar el mismo mensaje no lo arregla.
     */
    public void descontarStock(int cantidad) {
        if (cantidad > stock) {
            throw new StockInsuficienteException(id, cantidad, stock);
        }
        stock -= cantidad;
    }
}
