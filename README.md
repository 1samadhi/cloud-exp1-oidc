# EXP1 — Arquitectura segura en la nube con OIDC y OAuth 2.0

Solucion de la Experiencia 1 de Cloud: tres microservicios Spring Boot desacoplados
de la logica de autenticacion, protegidos con OAuth 2.0 y publicados a Internet
a traves de AWS API Gateway.

## Microservicios

| Servicio       | Puerto | Rol                                                |
|----------------|--------|----------------------------------------------------|
| `ms-auth`      | 9000   | Identity Provider propio (OIDC)                    |
| `ms-productos` | 8081   | Catalogo de productos — Resource Server            |
| `ms-pedidos`   | 8082   | Gestion de pedidos — Resource Server               |

## Stack

- Java 21, Spring Boot 4.1.0
- Docker (multistage, usuario no root) y Docker Compose
- AWS EC2 + AWS API Gateway (HTTP API)

## Endpoints disponibles

### ms-auth
| Metodo | Ruta              | Descripcion                    |
|--------|-------------------|--------------------------------|
| GET    | `/api/v1/estado`  | Estado y version del servicio  |

### ms-productos
| Metodo | Ruta                  | Descripcion               |
|--------|-----------------------|---------------------------|
| GET    | `/api/v1/productos`      | Lista el catalogo         |
| GET    | `/api/v1/productos/{id}` | Consulta un producto      |

### ms-pedidos
| Metodo | Ruta                | Descripcion            |
|--------|---------------------|------------------------|
| GET    | `/api/v1/pedidos`   | Lista los pedidos      |
| POST   | `/api/v1/pedidos`   | Registra un pedido     |

## Como ejecutar

Con Docker Compose (levanta los tres servicios):

```bash
docker compose up --build -d
```

Verificar:

```bash
curl http://localhost:9000/api/v1/estado
curl http://localhost:8081/api/v1/productos
curl http://localhost:8082/api/v1/pedidos
```

Detener:

```bash
docker compose down
```

Un servicio por separado, sin Docker:

```bash
cd ms-productos && ./mvnw spring-boot:run
```

## Nota sobre persistencia

El catalogo y los pedidos viven en memoria. La actividad permite simular el almacen
de datos, y evitar un contenedor de base de datos deja la solucion dentro del
presupuesto de la cuenta AWS Academy y de la RAM de una instancia pequenia.
