package cl.duoc.cloud.facturacion.mensajeria.evento;

import java.time.Instant;
import java.util.List;

/** Espejo del contrato de ms-ordenes. Ver ms-productos/.../OrdenCreadaEvento para el patron. */
public record OrdenCreadaEvento(
        Long idOrden,
        String idUsuario,
        String correo,
        List<LineaEvento> items,
        long total,
        Instant fecha) {

    public record LineaEvento(Long idProducto, int cantidad, long precioUnitario) {
    }
}
