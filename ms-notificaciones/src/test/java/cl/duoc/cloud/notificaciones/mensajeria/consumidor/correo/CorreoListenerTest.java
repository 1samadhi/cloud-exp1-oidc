package cl.duoc.cloud.notificaciones.mensajeria.consumidor.correo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.rabbitmq.client.Channel;

import cl.duoc.cloud.notificaciones.mensajeria.evento.OrdenCreadaEvento;

@ExtendWith(MockitoExtension.class)
class CorreoListenerTest {

    @Mock
    private Channel canal;

    private final CorreoListener listener = new CorreoListener();

    private static OrdenCreadaEvento ordenCon(String correo) {
        return new OrdenCreadaEvento(1L, "usuario-1", correo,
                List.of(new OrdenCreadaEvento.LineaEvento(1L, 2, 10000)), 20000L, Instant.now());
    }

    private static Message mensajeSinHistorial() {
        return new Message(new byte[0], new MessageProperties());
    }

    @Test
    void correoValidoSeConfirma() throws Exception {
        listener.alRecibirEventoDeOrden(ordenCon("cliente@duocuc.cl"), mensajeSinHistorial(), canal, 7L);

        verify(canal).basicAck(7L, false);
        verify(canal, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void correoSinArrobaSeRechazaSinReintentar() throws Exception {
        listener.alRecibirEventoDeOrden(ordenCon("correo-invalido"), mensajeSinHistorial(), canal, 7L);

        verify(canal).basicNack(7L, false, false);
        verify(canal, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void correoVacioSeRechazaSinReintentar() throws Exception {
        listener.alRecibirEventoDeOrden(ordenCon(""), mensajeSinHistorial(), canal, 7L);

        verify(canal).basicNack(7L, false, false);
    }
}
