package cl.duoc.cloud.productos.mensajeria.consumidor.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.rabbitmq.client.Channel;

import cl.duoc.cloud.productos.mensajeria.evento.OrdenCreadaEvento;
import cl.duoc.cloud.productos.model.Producto;
import cl.duoc.cloud.productos.repository.ProductoRepository;

/**
 * Prueba los tres desenlaces del consumidor sin un broker levantado: se llama
 * al metodo directo, con dobles de Channel y del repositorio.
 */
@ExtendWith(MockitoExtension.class)
class StockListenerTest {

    @Mock
    private ProductoRepository repositorio;

    @Mock
    private Channel canal;

    private StockListener listener;

    private static OrdenCreadaEvento ordenCon(Long productoId, int cantidad) {
        return new OrdenCreadaEvento(1L, "usuario-1", "cliente@duocuc.cl",
                List.of(new OrdenCreadaEvento.LineaEvento(productoId, cantidad, 10000)),
                10000L * cantidad, Instant.now());
    }

    private static Message mensajeSinReintentos() {
        return new Message(new byte[0], new MessageProperties());
    }

    @Test
    void stockSuficiente_descuentaYConfirma() throws Exception {
        listener = new StockListener(repositorio);
        Producto producto = new Producto("Teclado", 10000, 5);
        given(repositorio.findById(1L)).willReturn(Optional.of(producto));

        listener.alRecibirOrdenCreada(ordenCon(1L, 2), mensajeSinReintentos(), canal, 7L);

        assertThat(producto.getStock()).isEqualTo(3);
        verify(canal).basicAck(7L, false);
        verify(canal, never()).basicNack(anyLong(), anyBool(), anyBool());
    }

    @Test
    void stockInsuficiente_rechazaSinReintentar() throws Exception {
        listener = new StockListener(repositorio);
        Producto producto = new Producto("Monitor", 10000, 1);
        given(repositorio.findById(1L)).willReturn(Optional.of(producto));

        listener.alRecibirOrdenCreada(ordenCon(1L, 5), mensajeSinReintentos(), canal, 7L);

        verify(canal).basicNack(7L, false, false);
        verify(canal, never()).basicAck(anyLong(), anyBool());
    }

    @Test
    void productoInexistente_rechazaSinReintentar() throws Exception {
        listener = new StockListener(repositorio);
        given(repositorio.findById(99L)).willReturn(Optional.empty());

        listener.alRecibirOrdenCreada(ordenCon(99L, 1), mensajeSinReintentos(), canal, 7L);

        verify(canal).basicNack(7L, false, false);
    }

    @Test
    void errorTransitorioBajoElLimite_reintenta() throws Exception {
        listener = new StockListener(repositorio);
        given(repositorio.findById(1L)).willThrow(new RuntimeException("timeout simulado"));

        Message mensaje = mensajeSinReintentos();
        listener.alRecibirOrdenCreada(ordenCon(1L, 1), mensaje, canal, 7L);

        // requeue=true: vuelve a la cola para un proximo intento.
        verify(canal).basicNack(7L, false, true);
    }

    @Test
    void errorTransitorioAgotaLosReintentos_vaALaDlq() throws Exception {
        listener = new StockListener(repositorio);
        given(repositorio.findById(1L)).willThrow(new RuntimeException("timeout simulado"));

        MessageProperties propiedades = new MessageProperties();
        // Simula que RabbitMQ ya devolvio este mensaje 3 veces (x-death.count=3).
        propiedades.setHeader("x-death", List.of(Map.of("count", 3L)));
        Message mensajeConHistorial = new Message(new byte[0], propiedades);

        listener.alRecibirOrdenCreada(ordenCon(1L, 1), mensajeConHistorial, canal, 7L);

        // requeue=false: agoto los reintentos, se va a la DLQ.
        verify(canal).basicNack(7L, false, false);
    }

    // Mockito's any() boolean helper shortcut.
    private static boolean anyBool() {
        return org.mockito.ArgumentMatchers.anyBoolean();
    }
}
