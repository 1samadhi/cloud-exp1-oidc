package cl.duoc.cloud.facturacion.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Boleta emitida para una orden. Un registro por orden: la unicidad de
 * {@code idOrden} es lo que hace idempotente al consumidor (ver
 * FacturacionListener), no una deduplicacion aparte.
 */
@Entity
@Table(name = "facturas", uniqueConstraints = @UniqueConstraint(columnNames = "id_orden"))
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_orden", nullable = false)
    private Long idOrden;

    @Column(name = "id_usuario", nullable = false)
    private String idUsuario;

    @Column(nullable = false)
    private long total;

    @Column(name = "fecha_emision", nullable = false)
    private Instant fechaEmision;

    protected Factura() {
        // JPA
    }

    public Factura(Long idOrden, String idUsuario, long total) {
        this.idOrden = idOrden;
        this.idUsuario = idUsuario;
        this.total = total;
        this.fechaEmision = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getIdOrden() {
        return idOrden;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public long getTotal() {
        return total;
    }

    public Instant getFechaEmision() {
        return fechaEmision;
    }
}
