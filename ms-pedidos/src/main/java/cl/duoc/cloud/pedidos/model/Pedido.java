package cl.duoc.cloud.pedidos.model;

import java.time.Instant;

public record Pedido(Long id, String cliente, Long productoId, int cantidad, Instant creado) {
}
