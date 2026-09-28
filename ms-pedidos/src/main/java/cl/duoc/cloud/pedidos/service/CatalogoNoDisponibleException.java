package cl.duoc.cloud.pedidos.service;

/**
 * ms-productos no respondio: timeout, error interno o un rechazo que no es el
 * 404 de "producto inexistente".
 *
 * Se separa del 400 porque son cosas distintas para quien llama: un producto que
 * no existe es un problema del pedido, mientras que un catalogo inaccesible es
 * una falla del sistema y corresponde un 502, no un "producto inexistente".
 */
public class CatalogoNoDisponibleException extends RuntimeException {

    public CatalogoNoDisponibleException(Long productoId, Throwable causa) {
        super("No se pudo consultar el producto " + productoId + " en ms-productos", causa);
    }
}
