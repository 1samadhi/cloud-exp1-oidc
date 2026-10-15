package cl.duoc.cloud.notificaciones.config;

import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Converter JSON para publicar (el endpoint de prueba) y consumir.
 *
 * TypePrecedence.INFERRED: ver cl.duoc.cloud.productos.config.RabbitMessagingConfig
 * para la explicacion completa. En resumen: este servicio tiene su propia
 * clase OrdenCreadaEvento, distinta (aunque con la misma forma) de la que usa
 * ms-ordenes para publicar, asi que no puede confiar en la cabecera
 * __TypeId__ del emisor para resolver el tipo.
 */
@Configuration
public class RabbitMessagingConfig {

    @Bean
    MessageConverter convertidorJson() {
        JacksonJsonMessageConverter convertidor = new JacksonJsonMessageConverter();
        convertidor.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        return convertidor;
    }
}
