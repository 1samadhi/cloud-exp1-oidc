# Topologia de mensajeria (EXP2)

Fuente unica de los nombres. Cada microservicio los lee de su `application.yml`
(prefijo `pedidos360.rabbit`); ninguno se escribe a mano en el codigo.

## Exchanges

| Exchange | Tipo | Para que |
|---|---|---|
| `pedidos360.ordenes` | **topic** | Eventos del ciclo de vida de una orden (`orden.creada`, `orden.cancelada`, ...) |
| `pedidos360.notificaciones` | **direct** | Peticiones directas de envio de correo (la prueba desde Postman) |
| `pedidos360.dlx` | direct | Dead letter exchange: recibe lo que los consumidores rechazan |

## Colas (todas durables, cada una con su DLQ)

| Cola | Escucha | Binding (exchange → routing key) | DLQ |
|---|---|---|---|
| `productos.stock` | ms-productos | `pedidos360.ordenes` → `orden.creada` | `productos.stock.dlq` |
| `notificaciones.correo` | ms-notificaciones | `pedidos360.ordenes` → `orden.*` y `pedidos360.notificaciones` → `correo.enviar` | `notificaciones.correo.dlq` |
| `<nuevo>.ordenes` | ms-a-eleccion | `pedidos360.ordenes` → `orden.#` | `<nuevo>.ordenes.dlq` |

Cada cola se declara con:

- `x-dead-letter-exchange = pedidos360.dlx`
- `x-dead-letter-routing-key = <cola>.dlq`

Y cada DLQ queda enlazada a `pedidos360.dlx` con la routing key `<cola>.dlq`.

## Mensaje `orden.creada` (JSON)

```json
{
  "idOrden": 42,
  "idUsuario": "entra-oid-del-usuario",
  "correo": "cliente@dominio.cl",
  "items": [ { "idProducto": 3, "cantidad": 2, "precioUnitario": 9990 } ],
  "total": 19980,
  "fecha": "2026-10-05T13:00:00Z"
}
```

## Reglas de los consumidores (20 % de la pauta)

- `acknowledge-mode: manual`: cada mensaje se confirma a mano.
- Procesado bien → `basicAck`.
- Error transitorio (BD caida, timeout) → `basicNack(requeue=true)`, con un limite de reintentos.
- Error permanente (JSON invalido, producto inexistente, stock insuficiente) → `basicNack(requeue=false)` → va a la DLQ y queda en el log con el motivo.

## Ejemplo de `application.yml`

```yaml
spring:
  rabbitmq:
    addresses: ${SPRING_RABBITMQ_ADDRESSES:localhost:5672}
    username: ${RABBITMQ_USUARIO:pedidos360}
    password: ${RABBITMQ_PASSWORD:cambiar}
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 1

pedidos360:
  rabbit:
    exchange-ordenes: pedidos360.ordenes
    exchange-dlx: pedidos360.dlx
    cola-stock: productos.stock
    routing-orden-creada: orden.creada
```
