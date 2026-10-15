package cl.duoc.cloud.notificaciones.controller.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Cuerpo del endpoint de prueba: los datos minimos para simular una orden creada. */
public record PruebaCorreoDTO(
        @NotNull(message = "el id de orden es obligatorio") Long idOrden,
        @NotBlank(message = "el id de usuario es obligatorio") String idUsuario,
        @NotBlank(message = "el correo es obligatorio") @Email(message = "el correo no tiene formato valido") String correo,
        @NotNull(message = "el total es obligatorio") @Positive(message = "el total debe ser mayor a 0") Long total,
        List<@Valid Linea> items) {

    public record Linea(
            @NotNull Long idProducto,
            @Positive int cantidad,
            @Positive long precioUnitario) {
    }
}
