package cl.duoc.cloud.ordenes.controller.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Peticion de creacion de orden.
 *
 * No incluye precio ni total: los pone el servidor a partir del catalogo. No
 * incluye cliente: sale del claim sub del token.
 */
public record NuevaOrdenDTO(
        @NotEmpty(message = "La orden debe tener al menos un item") @Valid List<LineaDTO> items) {

    public record LineaDTO(
            @NotNull(message = "Falta el productoId") Long productoId,
            @Min(value = 1, message = "La cantidad debe ser mayor que cero") int cantidad) {
    }
}
