package cl.duoc.cloud.rabbitadmin.service;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.stereotype.Service;

import cl.duoc.cloud.rabbitadmin.controller.dto.NuevaColaDTO;
import cl.duoc.cloud.rabbitadmin.controller.dto.NuevoBindingDTO;
import cl.duoc.cloud.rabbitadmin.controller.dto.NuevoExchangeDTO;

/**
 * Logica de administracion de RabbitMQ, separada del controller.
 *
 * Envuelve {@link AmqpAdmin} (lo implementa RabbitAdmin) en vez de exponerlo
 * directo: aqui vive la lista blanca de tipos de exchange y las decisiones de
 * negocio (que pasa si ya existe, que pasa si no existe). El controller solo
 * traduce HTTP.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    /** AMQP solo reconoce estos 4 tipos nativos; cualquier otro valor es un error del llamador, no del broker. */
    private static final Set<String> TIPOS_EXCHANGE_PERMITIDOS = Set.of("direct", "topic", "fanout", "headers");

    private final AmqpAdmin admin;

    public RabbitAdminService(AmqpAdmin admin) {
        this.admin = admin;
    }

    public void crearCola(NuevaColaDTO datos) {
        QueueBuilder builder = datos.esDurable() ? QueueBuilder.durable(datos.nombre())
                : QueueBuilder.nonDurable(datos.nombre());
        if (datos.exchangeDeadLetter() != null && !datos.exchangeDeadLetter().isBlank()) {
            builder.withArgument("x-dead-letter-exchange", datos.exchangeDeadLetter());
        }
        Queue cola = builder.build();
        admin.declareQueue(cola);
        log.info("cola declarada: {} (durable={})", datos.nombre(), datos.esDurable());
    }

    /** {@code true} si existia y se elimino; {@code false} si no habia nada que borrar. */
    public boolean eliminarCola(String nombre) {
        boolean existia = admin.getQueueInfo(nombre) != null;
        if (existia) {
            admin.deleteQueue(nombre);
            log.info("cola eliminada: {}", nombre);
        }
        return existia;
    }

    public void crearExchange(NuevoExchangeDTO datos) {
        String tipo = datos.tipo() == null ? "" : datos.tipo().trim().toLowerCase();
        if (!TIPOS_EXCHANGE_PERMITIDOS.contains(tipo)) {
            throw new ParametroInvalidoException(
                    "Tipo de exchange invalido: '" + datos.tipo() + "'. Debe ser uno de " + TIPOS_EXCHANGE_PERMITIDOS);
        }
        Exchange exchange = construirExchange(datos.nombre(), tipo, datos.esDurable());
        admin.declareExchange(exchange);
        log.info("exchange declarado: {} (tipo={}, durable={})", datos.nombre(), tipo, datos.esDurable());
    }

    private Exchange construirExchange(String nombre, String tipo, boolean durable) {
        return switch (tipo) {
            case "direct" -> new DirectExchange(nombre, durable, false);
            case "topic" -> new TopicExchange(nombre, durable, false);
            case "fanout" -> new FanoutExchange(nombre, durable, false);
            // "headers" no tiene clase propia en Spring AMQP: se modela como CustomExchange.
            default -> new CustomExchange(nombre, "headers", durable, false);
        };
    }

    public void eliminarExchange(String nombre) {
        admin.deleteExchange(nombre);
        log.info("exchange eliminado: {}", nombre);
    }

    public void crearBinding(NuevoBindingDTO datos) {
        Binding binding = BindingBuilder.bind(new Queue(datos.cola()))
                .to(new CustomExchange(datos.exchange(), "direct", true, false))
                .with(datos.routingKeyOVacia())
                .noargs();
        admin.declareBinding(binding);
        log.info("binding declarado: cola={} exchange={} routingKey='{}'",
                datos.cola(), datos.exchange(), datos.routingKeyOVacia());
    }

    public void eliminarBinding(NuevoBindingDTO datos) {
        Binding binding = BindingBuilder.bind(new Queue(datos.cola()))
                .to(new CustomExchange(datos.exchange(), "direct", true, false))
                .with(datos.routingKeyOVacia())
                .noargs();
        admin.removeBinding(binding);
        log.info("binding eliminado: cola={} exchange={} routingKey='{}'",
                datos.cola(), datos.exchange(), datos.routingKeyOVacia());
    }
}
