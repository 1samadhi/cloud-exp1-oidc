package cl.duoc.cloud.ordenes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Linea de una orden: un producto, su cantidad y el precio al que se vendio.
 *
 * El precio se copia en el momento de crear la orden. Si se consultara el
 * catalogo despues, una boleta reimpresa mostraria el precio de hoy y no el que
 * pago el cliente.
 */
@Entity
@Table(name = "orden_lineas")
public class LineaOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false)
    private long precioUnitario;

    /** Requerido por JPA. */
    protected LineaOrden() {
    }

    LineaOrden(Orden orden, Long productoId, int cantidad, long precioUnitario) {
        this.orden = orden;
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public Long getId() {
        return id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public int getCantidad() {
        return cantidad;
    }

    public long getPrecioUnitario() {
        return precioUnitario;
    }
}
