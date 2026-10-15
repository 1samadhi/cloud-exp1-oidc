package cl.duoc.cloud.ordenes.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Fuerza la declaracion de la topologia de RabbitMQ al arrancar.
 *
 * Spring AMQP declara los beans Queue/Exchange/Binding en forma perezosa: solo
 * cuando se abre una conexion real al broker, y eso ocurre recien con el
 * primer RabbitTemplate.send() o con un @RabbitListener activo. ms-ordenes es
 * puro productor, sin listener propio, asi que sin este empujon las colas y
 * la DLQ no existen hasta la primera orden creada; mientras tanto el
 * consumidor de ms-productos no encuentra "productos.stock" y queda
 * reintentando conexion cada pocos segundos, gastando CPU sin hacer nada.
 *
 * Deshabilitado en las pruebas (pedidos360.rabbit.declarar-al-arrancar=false
 * en test/resources/application.yml): ApplicationReadyEvent se dispara igual
 * dentro de un @SpringBootTest, y sin un broker real la conexion fallaria.
 */
@Component
@ConditionalOnProperty(prefix = "pedidos360.rabbit", name = "declarar-al-arrancar",
        havingValue = "true", matchIfMissing = true)
public class InicializadorRabbit {

    private static final Logger log = LoggerFactory.getLogger(InicializadorRabbit.class);

    private final RabbitAdmin admin;

    public InicializadorRabbit(RabbitAdmin admin) {
        this.admin = admin;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void declararTopologia() {
        admin.initialize();
        log.info("topologia de RabbitMQ declarada al arrancar (exchanges, colas y DLQ)");
    }
}
