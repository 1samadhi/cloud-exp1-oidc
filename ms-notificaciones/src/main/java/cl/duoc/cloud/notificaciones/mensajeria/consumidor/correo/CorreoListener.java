package cl.duoc.cloud.notificaciones.mensajeria.consumidor.correo;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.cloud.notificaciones.mensajeria.evento.OrdenCreadaEvento;

/**
 * Consume cualquier evento de orden y "envia" el correo de aviso.
 *
 * El envio real queda fuera de alcance (no hay servidor SMTP en el
 * laboratorio): el aviso se registra en el log con el detalle suficiente para
 * servir de evidencia en la demo, tal como lo permite el enunciado.
 *
 * Mismo patron de ACK manual que StockListener en ms-productos: reintento
 * limitado a 3 veces leyendo la cabecera x-death, y rechazo permanente (DLQ)
 * si el dato de negocio esta mal formado (sin reintentar, porque reintentar
 * un correo invalido no lo arregla).
 */
@Component
public class CorreoListener {

    private static final Logger log = LoggerFactory.getLogger(CorreoListener.class);
    private static final int MAX_REINTENTOS = 3;

    @RabbitListener(queues = "${pedidos360.rabbit.cola-correo:notificaciones.correo}")
    public void alRecibirEventoDeOrden(
            @Payload OrdenCreadaEvento evento,
            Message mensaje,
            Channel canal,
            @Header(AmqpHeaders.DELIVERY_TAG) long etiquetaEntrega) throws java.io.IOException {

        try {
            validarCorreo(evento);
            log.info("correo de aviso enviado (simulado) a {} para la orden {}: total ${}",
                    evento.correo(), evento.idOrden(), evento.total());
            canal.basicAck(etiquetaEntrega, false);

        } catch (CorreoInvalidoException e) {
            log.error("orden {} rechazada en forma permanente: {}", evento.idOrden(), e.getMessage());
            canal.basicNack(etiquetaEntrega, false, false);

        } catch (Exception e) {
            int intento = contarReintentosPrevios(mensaje) + 1;
            if (intento >= MAX_REINTENTOS) {
                log.error("orden {} agoto los {} reintentos, va a la DLQ: {}",
                        evento.idOrden(), MAX_REINTENTOS, e.getMessage());
                canal.basicNack(etiquetaEntrega, false, false);
            } else {
                log.warn("error transitorio en la orden {} (intento {}/{}), se reintenta: {}",
                        evento.idOrden(), intento, MAX_REINTENTOS, e.getMessage());
                canal.basicNack(etiquetaEntrega, false, true);
            }
        }
    }

    private void validarCorreo(OrdenCreadaEvento evento) {
        String correo = evento.correo();
        if (correo == null || correo.isBlank() || !correo.contains("@")) {
            throw new CorreoInvalidoException(
                    "Correo invalido para la orden " + evento.idOrden() + ": '" + correo + "'");
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

    /** El evento llego con un correo vacio o sin arroba: dato corrupto, no se reintenta. */
    private static class CorreoInvalidoException extends RuntimeException {
        CorreoInvalidoException(String mensaje) {
            super(mensaje);
        }
    }
}
