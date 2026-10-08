package cl.duoc.cloud.ordenes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ms-ordenes: registra la orden y publica sus eventos.
 *
 * Es el productor del sistema de mensajeria. El carrito es mutable y del
 * usuario; la orden es un hecho ya ocurrido que se publica una vez y al que
 * reaccionan stock, notificaciones y facturacion por su cuenta.
 */
@SpringBootApplication
public class OrdenesApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrdenesApplication.class, args);
    }
}
