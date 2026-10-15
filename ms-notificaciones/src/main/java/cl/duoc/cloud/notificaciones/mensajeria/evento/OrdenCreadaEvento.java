package cl.duoc.cloud.notificaciones.mensajeria.evento;

import java.time.Instant;
import java.util.List;

/**
 * Espejo del contrato que publica ms-ordenes. Cada microservicio tiene su
 * propia copia deliberadamente (no hay libreria compartida entre
 * microservicios): ver cl.duoc.cloud.ordenes.mensajeria.evento.OrdenCreadaEvento
 * para el original y cl.duoc.cloud.productos.mensajeria.evento del mismo
 * patron.
 */
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
