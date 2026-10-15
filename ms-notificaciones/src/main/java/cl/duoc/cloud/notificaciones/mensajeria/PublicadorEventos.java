package cl.duoc.cloud.notificaciones.mensajeria;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import cl.duoc.cloud.notificaciones.mensajeria.evento.OrdenCreadaEvento;

/**
 * Unico punto por el que este servicio publica al broker. Lo pide la pauta:
 * la configuracion de mensajeria vive aparte del negocio, y el negocio (aqui,
 * el controller) no conoce RabbitTemplate directamente.
 */
@Component
public class PublicadorEventos {

    private final RabbitTemplate plantilla;
    private final PropiedadesRabbit nombres;

    public PublicadorEventos(RabbitTemplate plantilla, PropiedadesRabbit nombres) {
        this.plantilla = plantilla;
        this.nombres = nombres;
    }

    /**
     * Publica directo al exchange de notificaciones, saltandose el flujo
     * normal de ordenes. Es lo que permite probar el consumidor desde Postman
     * sin tener que crear una orden real primero.
     */
    public void publicarCorreoDePrueba(OrdenCreadaEvento evento) {
        plantilla.convertAndSend(nombres.getExchangeNotificaciones(), nombres.getRoutingCorreoPrueba(), evento);
    }
}
