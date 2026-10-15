package cl.duoc.cloud.rabbitadmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;

import cl.duoc.cloud.rabbitadmin.controller.dto.NuevaColaDTO;
import cl.duoc.cloud.rabbitadmin.controller.dto.NuevoBindingDTO;
import cl.duoc.cloud.rabbitadmin.controller.dto.NuevoExchangeDTO;

@ExtendWith(MockitoExtension.class)
class RabbitAdminServiceTest {

    @Mock
    private AmqpAdmin admin;

    private RabbitAdminService servicio;

    @Test
    void creaUnaColaDurablePorDefecto() {
        servicio = new RabbitAdminService(admin);
        servicio.crearCola(new NuevaColaDTO("q.prueba", null, null));

        verify(admin).declareQueue(any(Queue.class));
    }

    @Test
    void colaConDlqAgregaElArgumentoDeDeadLetter() {
        servicio = new RabbitAdminService(admin);
        servicio.crearCola(new NuevaColaDTO("q.prueba", true, "pedidos360.dlx"));

        var captor = org.mockito.ArgumentCaptor.forClass(Queue.class);
        verify(admin).declareQueue(captor.capture());
        assertThat(captor.getValue().getArguments()).containsEntry("x-dead-letter-exchange", "pedidos360.dlx");
    }

    @Test
    void eliminarColaExistenteInformaQueSeBorro() {
        servicio = new RabbitAdminService(admin);
        given(admin.getQueueInfo("q.prueba")).willReturn(new QueueInformation("q.prueba", 0, 0));

        boolean resultado = servicio.eliminarCola("q.prueba");

        assertThat(resultado).isTrue();
        verify(admin).deleteQueue("q.prueba");
    }

    @Test
    void eliminarColaInexistenteNoLlamaAlBroker() {
        servicio = new RabbitAdminService(admin);
        given(admin.getQueueInfo("no-existe")).willReturn(null);

        boolean resultado = servicio.eliminarCola("no-existe");

        assertThat(resultado).isFalse();
        verify(admin, never()).deleteQueue(any());
    }

    @Test
    void creaUnExchangeTopic() {
        servicio = new RabbitAdminService(admin);
        servicio.crearExchange(new NuevoExchangeDTO("pedidos360.ordenes", "topic", true));

        verify(admin).declareExchange(any(Exchange.class));
    }

    @Test
    void tipoDeExchangeInvalidoEs400() {
        servicio = new RabbitAdminService(admin);

        assertThatThrownBy(() -> servicio.crearExchange(new NuevoExchangeDTO("x", "inventado", true)))
                .isInstanceOf(ParametroInvalidoException.class)
                .hasMessageContaining("inventado");
        verify(admin, never()).declareExchange(any());
    }

    @Test
    void creaUnBinding() {
        servicio = new RabbitAdminService(admin);
        servicio.crearBinding(new NuevoBindingDTO("q.prueba", "pedidos360.ordenes", "orden.creada"));

        verify(admin).declareBinding(any(Binding.class));
    }
}
