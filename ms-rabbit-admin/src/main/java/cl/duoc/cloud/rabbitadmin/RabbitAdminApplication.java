package cl.duoc.cloud.rabbitadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ms-rabbit-admin: administra la topologia de RabbitMQ por REST.
 *
 * Lo pide la pauta como microservicio aparte (30 % repartido en 3 criterios):
 * crear/eliminar colas, exchanges y bindings sin entrar a la consola de
 * management ni al broker a mano. No es parte del flujo de negocio (no
 * publica ni consume eventos de ordenes): es una herramienta operativa.
 */
@SpringBootApplication
public class RabbitAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitAdminApplication.class, args);
    }
}
