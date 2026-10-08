package cl.duoc.cloud.ordenes.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Entidad JPA de la orden.
 *
 * A diferencia del carrito, que es mutable y se vacia, la orden es un hecho
 * historico: una vez registrada no se modifica, porque es lo que respalda la
 * boleta y el descuento de stock. Por eso guarda el precio unitario de cada
 * linea en lugar de mirar el catalogo cada vez.
 *
 * "cliente" guarda el claim sub del token y esta indexado porque toda consulta
 * del servicio filtra por el.
 */
@Entity
@Table(name = "ordenes", indexes = @Index(name = "idx_ordenes_cliente", columnList = "cliente"))
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String cliente;

    @Column(length = 320)
    private String correo;

    /** En pesos, sin decimales: evita los errores de redondeo de double. */
    @Column(nullable = false)
    private long total;

    @Column(nullable = false)
    private Instant creado;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineaOrden> lineas = new ArrayList<>();

    /** Requerido por JPA. */
    protected Orden() {
    }

    public Orden(String cliente, String correo) {
        this.cliente = cliente;
        this.correo = correo;
        this.creado = Instant.now();
        this.total = 0;
    }

    /** Agrega una linea y recalcula el total, que nunca se fija desde fuera. */
    public void agregarLinea(Long productoId, int cantidad, long precioUnitario) {
        lineas.add(new LineaOrden(this, productoId, cantidad, precioUnitario));
        this.total += (long) cantidad * precioUnitario;
    }

    public Long getId() {
        return id;
    }

    public String getCliente() {
        return cliente;
    }

    public String getCorreo() {
        return correo;
    }

    public long getTotal() {
        return total;
    }

    public Instant getCreado() {
        return creado;
    }

    public List<LineaOrden> getLineas() {
        return List.copyOf(lineas);
    }
}
