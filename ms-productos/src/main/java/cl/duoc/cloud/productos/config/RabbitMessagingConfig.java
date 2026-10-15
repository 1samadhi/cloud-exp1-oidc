package cl.duoc.cloud.productos.config;

import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Converter JSON para el listener de mensajeria.
 *
 * Sin esto, Spring Boot usa un converter que no sabe mapear JSON a un POJO
 * propio y la conversion del mensaje falla antes de llegar a StockListener.
 *
 * TypePrecedence.INFERRED: el tipo del payload se toma del parametro del
 * metodo @RabbitListener, no de la cabecera __TypeId__ que pone el emisor. Es
 * obligatorio aqui porque el productor (ms-ordenes) y este consumidor tienen
 * cada uno su propia clase OrdenCreadaEvento, en paquetes distintos: con la
 * precedencia por defecto (TYPE_ID) este servicio intentaria cargar la clase
 * del paquete de ms-ordenes, que no existe en su classpath.
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
