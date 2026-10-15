package cl.duoc.cloud.facturacion.mensajeria.consumidor.facturacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.dao.DataIntegrityViolationException;

import com.rabbitmq.client.Channel;

import cl.duoc.cloud.facturacion.mensajeria.evento.OrdenCreadaEvento;
import cl.duoc.cloud.facturacion.model.Factura;
import cl.duoc.cloud.facturacion.repository.FacturaRepository;

@ExtendWith(MockitoExtension.class)
class FacturacionListenerTest {

    @Mock
    private FacturaRepository repositorio;

    @Mock
    private Channel canal;

    private FacturacionListener listener;

    private static OrdenCreadaEvento ordenCon(Long idOrden) {
        return new OrdenCreadaEvento(idOrden, "usuario-1", "cliente@duocuc.cl",
                List.of(new OrdenCreadaEvento.LineaEvento(1L, 2, 10000)), 20000L, Instant.now());
    }

    private static Message mensajeSinHistorial() {
        return new Message(new byte[0], new MessageProperties());
    }

    @Test
    void emiteLaBoletaYConfirma() throws Exception {
        listener = new FacturacionListener(repositorio);
        given(repositorio.findByIdOrden(1L)).willReturn(Optional.empty());
        given(repositorio.save(any())).willReturn(new Factura(1L, "usuario-1", 20000L));

        listener.alRecibirEventoDeOrden(ordenCon(1L), mensajeSinHistorial(), canal, 7L);

        verify(repositorio).save(any());
        verify(canal).basicAck(7L, false);
    }

    @Test
    void siLaBoletaYaExisteNoLaDuplicaYConfirmaIgual() throws Exception {
        listener = new FacturacionListener(repositorio);
        given(repositorio.findByIdOrden(1L)).willReturn(Optional.of(new Factura(1L, "usuario-1", 20000L)));

        listener.alRecibirEventoDeOrden(ordenCon(1L), mensajeSinHistorial(), canal, 7L);

        verify(repositorio, never()).save(any());
        verify(canal).basicAck(7L, false);
    }

    @Test
    void unaCarreraConOtroConsumidorSeToleraComoExito() throws Exception {
        listener = new FacturacionListener(repositorio);
        given(repositorio.findByIdOrden(1L)).willReturn(Optional.empty());
        given(repositorio.save(any())).willThrow(new DataIntegrityViolationException("duplicado"));

        listener.alRecibirEventoDeOrden(ordenCon(1L), mensajeSinHistorial(), canal, 7L);

        // La restriccion UNIQUE gano la carrera, pero el resultado (una sola
        // boleta) es el correcto: se confirma el mensaje, no se reintenta.
        verify(canal).basicAck(7L, false);
    }

    @Test
    void errorTransitorioBajoElLimiteReintenta() throws Exception {
        listener = new FacturacionListener(repositorio);
        given(repositorio.findByIdOrden(1L)).willThrow(new RuntimeException("timeout simulado"));

        listener.alRecibirEventoDeOrden(ordenCon(1L), mensajeSinHistorial(), canal, 7L);

        verify(canal).basicNack(7L, false, true);
    }
}
