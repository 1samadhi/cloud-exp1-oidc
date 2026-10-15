package cl.duoc.cloud.productos.mensajeria.evento;

import java.time.Instant;
import java.util.List;

/**
 * Espejo de cl.duoc.cloud.ordenes.mensajeria.evento.OrdenCreadaEvento.
 *
 * Es el contrato publicado por ms-ordenes: deliberadamente no se comparte una
 * libreria comun entre microservicios independientes, asi que se duplica aqui.
 * Solo se leen los campos que necesita el descuento de stock.
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
