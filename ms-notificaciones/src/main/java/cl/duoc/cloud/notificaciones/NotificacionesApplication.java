package cl.duoc.cloud.notificaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ms-notificaciones: avisa al cliente cuando pasa algo con su orden.
 *
 * El envio real de correo (SMTP) queda fuera de alcance del laboratorio: lo
 * que importa para la pauta es la mensajeria (consumir, ACK manual, DLQ), asi
 * que el "envio" se simula con un log, tal como lo permite el enunciado.
 */
@SpringBootApplication
public class NotificacionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificacionesApplication.class, args);
    }
}
