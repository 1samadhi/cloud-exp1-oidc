package cl.duoc.cloud.rabbitadmin.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Alta de un exchange. El tipo se valida contra la lista blanca en
 * {@code RabbitAdminService}: un valor fuera de ella es un 400, no una
 * excepcion generica del driver AMQP.
 */
public record NuevoExchangeDTO(
        @NotBlank(message = "el nombre del exchange es obligatorio") String nombre,
        @NotBlank(message = "el tipo del exchange es obligatorio (direct, topic, fanout o headers)") String tipo,
        Boolean durable) {

    public boolean esDurable() {
        return durable == null || durable;
    }
}
