package cl.duoc.cloud.productos.model;

/**
 * Se pidio descontar mas unidades de las que hay disponibles.
 *
 * Error de negocio, no de infraestructura: reintentar el mismo mensaje no lo
 * arregla, asi que el consumidor lo trata como permanente y lo manda a la DLQ.
 */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(Long productoId, int pedido, int disponible) {
        super("Stock insuficiente para el producto " + productoId
                + ": se pidieron " + pedido + ", hay " + disponible);
    }
}
