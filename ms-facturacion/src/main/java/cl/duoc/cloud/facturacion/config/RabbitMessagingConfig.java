package cl.duoc.cloud.facturacion.config;

import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ver cl.duoc.cloud.productos.config.RabbitMessagingConfig para la explicacion de INFERRED. */
@Configuration
public class RabbitMessagingConfig {

    @Bean
    MessageConverter convertidorJson() {
        JacksonJsonMessageConverter convertidor = new JacksonJsonMessageConverter();
        convertidor.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        return convertidor;
    }
}
