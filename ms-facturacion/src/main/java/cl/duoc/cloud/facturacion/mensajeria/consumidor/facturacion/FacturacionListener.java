package cl.duoc.cloud.facturacion.mensajeria.consumidor.facturacion;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.cloud.facturacion.mensajeria.evento.OrdenCreadaEvento;
import cl.duoc.cloud.facturacion.model.Factura;
import cl.duoc.cloud.facturacion.repository.FacturaRepository;

/**
 * Emite la boleta de la orden. Mismo patron de ACK manual que StockListener
 * (ms-productos) y CorreoListener (ms-notificaciones): ver cualquiera de los
 * dos para la explicacion completa del contrato de reintentos.
 *
 * Idempotencia: {@code id_orden} es UNIQUE en la tabla. Si el mismo mensaje
 * llega dos veces (p. ej. un ACK que se perdio en la red pero el consumidor si
 * proceso el mensaje), el segundo intento choca con la restriccion y se trata
 * como exito, no como error: la boleta ya existe, que es justo el resultado
 * deseado.
 */
@Component
public class FacturacionListener {

    private static final Logger log = LoggerFactory.getLogger(FacturacionListener.class);
    private static final int MAX_REINTENTOS = 3;

    private final FacturaRepository repositorio;

    public FacturacionListener(FacturaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @RabbitListener(queues = "${pedidos360.rabbit.cola-facturacion:facturacion.ordenes}")
    public void alRecibirEventoDeOrden(
            @Payload OrdenCreadaEvento evento,
            Message mensaje,
            Channel canal,
            @Header(AmqpHeaders.DELIVERY_TAG) long etiquetaEntrega) throws java.io.IOException {

        try {
            emitirBoleta(evento);
            canal.basicAck(etiquetaEntrega, false);

        } catch (Exception e) {
            int intento = contarReintentosPrevios(mensaje) + 1;
            if (intento >= MAX_REINTENTOS) {
                log.error("orden {} agoto los {} reintentos, va a la DLQ: {}",
                        evento.idOrden(), MAX_REINTENTOS, e.getMessage());
                canal.basicNack(etiquetaEntrega, false, false);
            } else {
                log.warn("error transitorio emitiendo la boleta de la orden {} (intento {}/{}): {}",
                        evento.idOrden(), intento, MAX_REINTENTOS, e.getMessage());
                canal.basicNack(etiquetaEntrega, false, true);
            }
        }
    }

    private void emitirBoleta(OrdenCreadaEvento evento) {
        if (repositorio.findByIdOrden(evento.idOrden()).isPresent()) {
            log.info("boleta de la orden {} ya existia, no se duplica (mensaje redundante)", evento.idOrden());
            return;
        }
        try {
            Factura factura = repositorio.save(
                    new Factura(evento.idOrden(), evento.idUsuario(), evento.total()));
            log.info("boleta #{} emitida para la orden {}: total ${}",
                    factura.getId(), evento.idOrden(), evento.total());
        } catch (DataIntegrityViolationException carreraConOtroConsumidor) {
            // Dos consumidores (o reintentos simultaneos) llegaron a la vez; la
            // restriccion UNIQUE de id_orden gano la carrera por uno de los dos.
            // El resultado final (una sola boleta) es el correcto.
            log.info("boleta de la orden {} ya la emitio otro intento en paralelo", evento.idOrden());
        }
    }

    private int contarReintentosPrevios(Message mensaje) {
        List<?> muertes = mensaje.getMessageProperties().getXDeathHeader();
        if (muertes == null || muertes.isEmpty()) {
            return 0;
        }
        Object primera = muertes.get(0);
        if (primera instanceof Map<?, ?> mapa && mapa.get("count") instanceof Number numero) {
            return numero.intValue();
        }
        return 0;
    }
}
