package cl.duoc.cloud.notificaciones.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import cl.duoc.cloud.notificaciones.mensajeria.PropiedadesRabbit;

/** Habilita la lectura de pedidos360.rabbit.* desde application.yml. */
@Configuration
@EnableConfigurationProperties(PropiedadesRabbit.class)
public class PropiedadesRabbitConfig {
}
