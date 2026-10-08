package cl.duoc.cloud.carrito.service;

/**
 * ms-productos no respondio: timeout, error interno o un rechazo que no es el
 * 404 de "producto inexistente".
 *
 * Se separa del 400 porque son cosas distintas para quien llama: un producto que
 * no existe es un problema de lo que pidio, mientras que un catalogo inaccesible
 * es una falla del sistema y corresponde un 502.
 *
 * Diagnostico original de Diego Villota en la rama fix/auditoria-v9.
 */
public class CatalogoNoDisponibleException extends RuntimeException {

    public CatalogoNoDisponibleException(Long productoId, Throwable causa) {
        super("No se pudo consultar el producto " + productoId + " en ms-productos", causa);
    }
}
