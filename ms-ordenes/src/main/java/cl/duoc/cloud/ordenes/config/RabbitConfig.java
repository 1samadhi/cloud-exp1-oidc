package cl.duoc.cloud.ordenes.config;

import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cl.duoc.cloud.ordenes.mensajeria.PropiedadesRabbit;

/**
 * Topologia de mensajeria del dominio de ordenes.
 *
 * Aqui se declara la infraestructura y nada del negocio: un bloque por caso de
 * uso, cada uno con su cola, su binding y su dead letter queue. Los nombres
 * vienen de PropiedadesRabbit, es decir de application.yml.
 *
 * Las declaraciones son idempotentes: si la cola ya existe con los mismos
 * argumentos, RabbitMQ la reutiliza. Si existe con argumentos distintos, falla
 * al arrancar, y eso es deseable: avisa de una topologia divergente en lugar de
 * enrutar mensajes a un sitio inesperado.
 *
 * Quien publica es este servicio, asi que es el que declara los tres destinos
 * del evento. Cada consumidor solo escucha el nombre de cola que lee de su
 * propia configuracion, sin volver a declararla.
 */
@Configuration
@EnableConfigurationProperties(PropiedadesRabbit.class)
public class RabbitConfig {

    private final PropiedadesRabbit nombres;

    public RabbitConfig(PropiedadesRabbit nombres) {
        this.nombres = nombres;
    }

    // ---------- Exchanges ----------

    /**
     * Topic, porque los consumidores eligen por patron: stock solo quiere
     * orden.creada, mientras notificaciones y facturacion quieren orden.#
     * y asi reciben tambien los eventos que se agreguen despues.
     */
    @Bean
    TopicExchange exchangeOrdenes() {
        return new TopicExchange(nombres.getExchangeOrdenes(), true, false);
    }

    /**
     * Direct, porque pedir "enviame este correo" es una instruccion con un
     * unico destinatario, no un hecho que a varios interese.
     */
    @Bean
    DirectExchange exchangeNotificaciones() {
        return new DirectExchange(nombres.getExchangeNotificaciones(), true, false);
    }

    /** Direct: cada DLQ se enlaza con la routing key exacta de su cola. */
    @Bean
    DirectExchange exchangeDlx() {
        return new DirectExchange(nombres.getExchangeDlx(), true, false);
    }

    // ---------- Caso de uso: descontar stock ----------

    @Bean
    Queue colaStock() {
        return colaConDlq(nombres.getColaStock());
    }

    @Bean
    Queue colaStockDlq() {
        return QueueBuilder.durable(nombres.dlqDe(nombres.getColaStock())).build();
    }

    @Bean
    Binding bindingStock() {
        return BindingBuilder.bind(colaStock())
                .to(exchangeOrdenes())
                .with(nombres.getRoutingOrdenCreada());
    }

    @Bean
    Binding bindingStockDlq() {
        return BindingBuilder.bind(colaStockDlq())
                .to(exchangeDlx())
                .with(nombres.dlqDe(nombres.getColaStock()));
    }

    // ---------- Caso de uso: notificar por correo ----------

    @Bean
    Queue colaCorreo() {
        return colaConDlq(nombres.getColaCorreo());
    }

    @Bean
    Queue colaCorreoDlq() {
        return QueueBuilder.durable(nombres.dlqDe(nombres.getColaCorreo())).build();
    }

    /** Cualquier evento de orden es motivo de aviso al cliente. */
    @Bean
    Binding bindingCorreoOrdenes() {
        return BindingBuilder.bind(colaCorreo())
                .to(exchangeOrdenes())
                .with(nombres.getPatronOrdenes());
    }

    /**
     * Segundo binding de la misma cola, ahora al exchange direct: es el que
     * permite la prueba desde Postman sin tener que crear una orden real.
     */
    @Bean
    Binding bindingCorreoDirecto() {
        return BindingBuilder.bind(colaCorreo())
                .to(exchangeNotificaciones())
                .with("correo.enviar");
    }

    @Bean
    Binding bindingCorreoDlq() {
        return BindingBuilder.bind(colaCorreoDlq())
                .to(exchangeDlx())
                .with(nombres.dlqDe(nombres.getColaCorreo()));
    }

    // ---------- Caso de uso: emitir la boleta ----------

    @Bean
    Queue colaFacturacion() {
        return colaConDlq(nombres.getColaFacturacion());
    }

    @Bean
    Queue colaFacturacionDlq() {
        return QueueBuilder.durable(nombres.dlqDe(nombres.getColaFacturacion())).build();
    }

    @Bean
    Binding bindingFacturacion() {
        return BindingBuilder.bind(colaFacturacion())
                .to(exchangeOrdenes())
                .with(nombres.getPatronOrdenes());
    }

    @Bean
    Binding bindingFacturacionDlq() {
        return BindingBuilder.bind(colaFacturacionDlq())
                .to(exchangeDlx())
                .with(nombres.dlqDe(nombres.getColaFacturacion()));
    }

    // ---------- Piezas comunes ----------

    /**
     * Cola durable que manda a la DLX lo que un consumidor rechace con
     * basicNack(requeue=false). Sin x-dead-letter-exchange, ese mensaje se
     * descarta en silencio y el error se pierde.
     */
    private Queue colaConDlq(String nombre) {
        return QueueBuilder.durable(nombre)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", nombres.getExchangeDlx(),
                        "x-dead-letter-routing-key", nombres.dlqDe(nombre)))
                .build();
    }

    /**
     * Los eventos viajan como JSON para que cualquier consumidor los lea.
     *
     * Es JacksonJsonMessageConverter y no Jackson2JsonMessageConverter: Spring
     * Boot 4 trae Jackson 3 (paquete tools.jackson) y el converter "2" busca
     * clases de com.fasterxml que ya no estan en el classpath.
     */
    @Bean
    MessageConverter convertidorJson() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory fabrica, MessageConverter convertidor) {
        RabbitTemplate plantilla = new RabbitTemplate(fabrica);
        plantilla.setMessageConverter(convertidor);
        // Avisa en el log si el exchange acepta el mensaje pero ninguna cola lo
        // recibe: un binding mal escrito no produce ningun error por si solo.
        plantilla.setMandatory(true);
        return plantilla;
    }
}
