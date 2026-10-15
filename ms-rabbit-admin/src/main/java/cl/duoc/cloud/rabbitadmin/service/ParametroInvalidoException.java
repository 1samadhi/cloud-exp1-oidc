package cl.duoc.cloud.rabbitadmin.service;

/** Error de validacion de negocio: datos correctos en forma, invalidos en significado. */
public class ParametroInvalidoException extends RuntimeException {
    public ParametroInvalidoException(String mensaje) {
        super(mensaje);
    }
}
