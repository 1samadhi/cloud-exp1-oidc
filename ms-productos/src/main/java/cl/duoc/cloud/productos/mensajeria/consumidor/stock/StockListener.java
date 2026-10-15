package cl.duoc.cloud.productos.mensajeria.consumidor.stock;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.cloud.productos.mensajeria.evento.OrdenCreadaEvento;
import cl.duoc.cloud.productos.model.Producto;
import cl.duoc.cloud.productos.model.StockInsuficienteException;
import cl.duoc.cloud.productos.repository.ProductoRepository;

/**
 * Descuenta stock cuando se registra una orden.
 *
 * ACK manual (spring.rabbitmq.listener.simple.acknowledge-mode=manual en
 * application.yml): el framework no confirma nada por su cuenta, as todo pasa
 * por basicAck/basicNack explicitos aqui.
 *
 * Tres desenlaces posibles para cada mensaje:
 * - Procesado bien                          -> basicAck.
 * - Error transitorio (BD caida, timeout)    -> basicNack(requeue=true), hasta
 *                                               un limite de reintentos.
 * - Error permanente o reintentos agotados  -> basicNack(requeue=false), la
 *                                               cola lo manda a su DLQ por la
 *                                               x-dead-letter-exchange que
 *                                               declaro ms-ordenes.
 */
@Component
public class StockListener {

    private static final Logger log = LoggerFactory.getLogger(StockListener.class);

    /** Reintentos antes de dar un error transitorio por agotado y mandarlo a la DLQ. */
    private static final int MAX_REINTENTOS = 3;

    private final ProductoRepository repositorio;

    public StockListener(ProductoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @RabbitListener(queues = "${pedidos360.rabbit.cola-stock:productos.stock}")
    public void alRecibirOrdenCreada(
            @Payload OrdenCreadaEvento evento,
            Message mensaje,
            Channel canal,
            @Header(AmqpHeaders.DELIVERY_TAG) long etiquetaEntrega) throws java.io.IOException {

        try {
            descontarStockDe(evento);
            canal.basicAck(etiquetaEntrega, false);
            log.info("stock descontado para la orden {}", evento.idOrden());

        } catch (StockInsuficienteException | ProductoInexistenteException e) {
            // Error de negocio: reintentar el mismo mensaje no cambia el
            // resultado. Se rechaza de inmediato, sin reintento.
            log.error("orden {} rechazada en forma permanente: {}", evento.idOrden(), e.getMessage());
            canal.basicNack(etiquetaEntrega, false, false);

        } catch (Exception e) {
            // Error transitorio: BD caida, timeout, lo que sea que no es culpa
            // del contenido del mensaje. Se reintenta hasta el limite; agotado
            // ese limite, se trata igual que un error permanente.
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

    private void descontarStockDe(OrdenCreadaEvento evento) {
        for (OrdenCreadaEvento.LineaEvento linea : evento.items()) {
            Producto producto = repositorio.findById(linea.idProducto())
                    .orElseThrow(() -> new ProductoInexistenteException(linea.idProducto()));
            producto.descontarStock(linea.cantidad());
            repositorio.save(producto);
        }
    }

    /**
     * RabbitMQ cuenta los reintentos en la cabecera x-death que agrega cada vez
     * que un nack(requeue=true) devuelve el mensaje a la cola.
     */
    private int contarReintentosPrevios(Message mensaje) {
        List<?> muertes = mensaje.getMessageProperties().getXDeathHeader();
        if (muertes == null || muertes.isEmpty()) {
            return 0;
        }
        Object primera = muertes.get(0);
        if (primera instanceof java.util.Map<?, ?> mapa && mapa.get("count") instanceof Number numero) {
            return numero.intValue();
        }
        return 0;
    }

    /** No existe el producto que la orden dice haber comprado: dato corrupto, no se reintenta. */
    private static class ProductoInexistenteException extends RuntimeException {
        ProductoInexistenteException(Long id) {
            super("No existe el producto " + id + " en el catalogo");
        }
    }
}
