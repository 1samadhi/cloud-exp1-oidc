package cl.duoc.cloud.rabbitadmin.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Enlaza una cola existente a un exchange existente por una routing key. */
public record NuevoBindingDTO(
        @NotBlank(message = "la cola es obligatoria") String cola,
        @NotBlank(message = "el exchange es obligatorio") String exchange,
        /** Vacio es valido: así se enlazan fanout y algunos direct. */
        String routingKey) {

    public String routingKeyOVacia() {
        return routingKey == null ? "" : routingKey;
    }
}
