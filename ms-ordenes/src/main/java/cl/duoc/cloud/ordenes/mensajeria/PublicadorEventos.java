package cl.duoc.cloud.ordenes.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import cl.duoc.cloud.ordenes.mensajeria.evento.OrdenCreadaEvento;

/**
 * Unico punto por donde el servicio publica eventos.
 *
 * El negocio (OrdenService) depende de esta clase y no de RabbitTemplate: asi
 * la logica de la orden no sabe que existe AMQP, y cambiar de broker o agregar
 * una cabecera a todos los mensajes se hace en un solo lugar.
 */
@Component
public class PublicadorEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorEventos.class);

    private final RabbitTemplate plantilla;
    private final PropiedadesRabbit nombres;

    public PublicadorEventos(RabbitTemplate plantilla, PropiedadesRabbit nombres) {
        this.plantilla = plantilla;
        this.nombres = nombres;
    }

    /**
     * Publica el evento de orden creada en el exchange topic.
     *
     * No captura la excepcion a proposito: se llama dentro de la transaccion
     * que guarda la orden, de modo que si el broker esta caido la orden no
     * queda registrada sin que nadie la procese. Es preferible devolver un
     * error al cliente que dejar stock sin descontar y boletas sin emitir.
     */
    public void publicarOrdenCreada(OrdenCreadaEvento evento) {
        plantilla.convertAndSend(
                nombres.getExchangeOrdenes(),
                nombres.getRoutingOrdenCreada(),
                evento);
        log.info("publicado {} de la orden {} en {}",
                nombres.getRoutingOrdenCreada(), evento.idOrden(), nombres.getExchangeOrdenes());
    }
}
