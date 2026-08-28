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

### ms-auth — Identity Provider OIDC
| Metodo | Ruta                                    | Auth | Descripcion                          |
|--------|-----------------------------------------|------|--------------------------------------|
| POST   | `/auth/login`                           | No   | Valida credenciales y emite tokens   |
| GET    | `/auth/userinfo`                        | Si   | Claims del token presentado          |
| GET    | `/.well-known/openid-configuration`     | No   | Metadatos OIDC del emisor            |
| GET    | `/.well-known/jwks.json`                | No   | Llaves publicas para validar la firma|
| GET    | `/api/v1/estado`                        | No   | Estado y version del servicio        |

Usuarios de prueba:

| Usuario   | Contrasenia  | Roles                   |
|-----------|--------------|-------------------------|
| `admin`   | `admin123`   | `ROLE_ADMIN`, `ROLE_USER` |
| `cliente` | `cliente123` | `ROLE_USER`             |

Obtener un token:

```bash
curl -X POST http://localhost:9000/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

#### Por que RS256 y no HS256

El autorizador JWT de AWS API Gateway verifica la firma descargando las llaves
publicas desde el `jwks_uri` que anuncia el discovery del emisor. Con HS256 la
firma depende de un secreto compartido que AWS nunca va a tener, asi que el
autorizador no podria validar nada. Por eso el IdP firma con RSA y publica su
llave publica.

### ms-productos
| Metodo | Ruta                  | Descripcion               |
|--------|-----------------------|---------------------------|
| GET    | `/api/v1/productos`      | Lista el catalogo         |
| GET    | `/api/v1/productos/{id}` | Consulta un producto      |

### ms-pedidos — Resource Server
| Metodo | Ruta                | Auth | Descripcion                                        |
|--------|---------------------|------|----------------------------------------------------|
| GET    | `/api/v1/public`    | No   | Comprobacion de despliegue                         |
| GET    | `/api/v1/pedidos`   | Si   | Pedidos del usuario del token                      |
| POST   | `/api/v1/pedidos`   | Si   | Crea un pedido — requiere scope `pedidos.escribir` |

`POST /api/v1/pedidos` valida el producto llamando a `ms-productos` y reenviando
el mismo token del usuario, de modo que la identidad viaja entre servicios en
lugar de confiar ciegamente en el llamador interno.

## Variables de entorno

| Variable              | Servicio            | Descripcion                                     |
|-----------------------|---------------------|-------------------------------------------------|
| `OIDC_ISSUER`         | ms-auth             | Valor del claim `iss`. En AWS, la URL del stage |
| `OIDC_AUDIENCIA`      | ms-auth             | Valor del claim `aud`                           |
| `OIDC_RUTA_LLAVE`     | ms-auth             | Ruta del JWK persistido                         |
| `SEGURIDAD_EMISORES`  | productos, pedidos  | Emisores confiables, separados por coma         |
| `SEGURIDAD_AUDIENCIAS`| productos, pedidos  | Audiencias aceptadas. Vacio desactiva el chequeo|
| `SEGURIDAD_ORIGENES_CORS` | productos, pedidos | Origenes del frontend permitidos             |
| `PRODUCTOS_URL`       | ms-pedidos          | URL base de ms-productos                        |

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
