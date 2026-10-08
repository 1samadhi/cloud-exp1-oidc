package cl.duoc.cloud.ordenes.mensajeria.evento;

import java.time.Instant;
import java.util.List;

/**
 * Evento que se publica cuando una orden queda registrada.
 *
 * Es el contrato con los consumidores, asi que va aparte de la entidad JPA: si
 * manda la entidad, cualquier cambio en la base de datos cambia el mensaje y
 * rompe a los tres servicios que lo leen.
 *
 * Lleva el correo del cliente para que ms-notificaciones no tenga que consultar
 * al IdP, y el precio unitario para que la boleta no dependa de que el catalogo
 * conserve el precio que tenia en el momento de la compra.
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
