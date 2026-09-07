package cl.duoc.cloud.pedidos.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA del pedido.
 *
 * "cliente" guarda el claim sub del token, es decir el identificador que asigna
 * el IdP. Se indexa porque toda consulta del servicio filtra por el.
 */
@Entity
@Table(name = "pedidos", indexes = @jakarta.persistence.Index(name = "idx_pedidos_cliente", columnList = "cliente"))
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String cliente;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false)
    private Instant creado;

    /** Requerido por JPA. */
    protected Pedido() {
    }

    public Pedido(String cliente, Long productoId, int cantidad) {
        this.cliente = cliente;
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.creado = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCliente() {
        return cliente;
    }

    public Long getProductoId() {
        return productoId;
    }

    public int getCantidad() {
        return cantidad;
    }

    public Instant getCreado() {
        return creado;
    }
}
