package cl.duoc.cloud.notificaciones.mensajeria;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de la topologia que usa este servicio, leidos de application.yml.
 *
 * Quien declara la cola, su DLQ y los bindings es ms-ordenes (el productor);
 * este servicio solo necesita saber el NOMBRE de la cola que escucha y el
 * exchange/routing key para el endpoint de prueba. Ver
 * cl.duoc.cloud.ordenes.mensajeria.PropiedadesRabbit para la topologia
 * completa y rabbitmq/TOPOLOGIA.md para el diagrama.
 */
@ConfigurationProperties(prefix = "pedidos360.rabbit")
public class PropiedadesRabbit {

    private String exchangeNotificaciones = "pedidos360.notificaciones";
    private String colaCorreo = "notificaciones.correo";
    private String routingCorreoPrueba = "correo.enviar";

    public String getExchangeNotificaciones() {
        return exchangeNotificaciones;
    }

    public void setExchangeNotificaciones(String exchangeNotificaciones) {
        this.exchangeNotificaciones = exchangeNotificaciones;
    }

    public String getColaCorreo() {
        return colaCorreo;
    }

    public void setColaCorreo(String colaCorreo) {
        this.colaCorreo = colaCorreo;
    }

    public String getRoutingCorreoPrueba() {
        return routingCorreoPrueba;
    }

    public void setRoutingCorreoPrueba(String routingCorreoPrueba) {
        this.routingCorreoPrueba = routingCorreoPrueba;
    }
}
