package cl.duoc.cloud.rabbitadmin.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Alta de una cola. "durable" por defecto true: una cola no durable pierde su
 * definicion si el nodo se reinicia, y aqui no hay forma de saber si el
 * llamador lo hizo a proposito o por olvido.
 */
public record NuevaColaDTO(
        @NotBlank(message = "el nombre de la cola es obligatorio") String nombre,
        Boolean durable,
        /** Exchange de dead-letter opcional; vacio desactiva el reenvio de rechazos. */
        String exchangeDeadLetter) {

    public boolean esDurable() {
        return durable == null || durable;
    }
}
