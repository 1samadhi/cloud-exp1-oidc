# EXP1 — Arquitectura segura en la nube con OIDC y OAuth 2.0

Sistema **Pedidos360**: arquitectura cloud-native multi nube. La identidad la
administra **Microsoft Entra ID** y el computo vive en **AWS**, con los
microservicios publicados a Internet unicamente a traves de un API Gateway que
valida cada token en el borde.


## Repositorios

El sistema se entrega en dos repositorios, uno por componente:

| Componente                | Repositorio                                                  |
|---------------------------|--------------------------------------------------------------|
| Microservicios (backend)  | https://github.com/1samadhi/cloud-exp1-oidc                  |
| Frontend Angular          | https://github.com/1samadhi/cloud-exp1-front-angular         |

`scripts/desplegar-en-ec2.sh` clona y actualiza ambos en la instancia: compila
la imagen del frontend en su repositorio y levanta el stack desde este.

## Despliegue en marcha

| Recurso        | Valor                                              |
|----------------|----------------------------------------------------|
| URL publica    | https://j37oj1wn16.execute-api.us-east-1.amazonaws.com/desarrollo |
| HTTP API       | `j37oj1wn16`                                         |
| Instancia EC2  | `i-0314ddd12fadeb125`                              |

## Arquitectura

```
                    Azure (identidad)
        +-----------------------------------+
        |  Microsoft Entra ID               |
        |  tenant Pedidos360                |
        |  emite tokens · publica su JWKS   |
        +----+-------------------------+----+
             | 1. login PKCE           | 3. AWS descarga
             |    (desde el navegador) |    las llaves publicas
             v                         v
        +-----------------------------------+
        |  AWS API Gateway (HTTP API)       |
        |  stage: desarrollo — HTTPS        |
        |                                   |
        |  Rutas limpias  /v1/...           |
        |  CORS por origen declarado        |
        |  Autorizador JWT — emisor: Entra  |
        +----------------+------------------+
                         | HTTP  2. el token viaja
                         v          en Authorization
        +-----------------------------------+
        |  EC2 t3.small — Docker Compose    |
        |                                   |
        |  front-angular  :80   nginx       |
        |  ms-auth        :9000 BFF         |
        |  ms-productos   :8081 Resource    |
        |  ms-pedidos     :8082 Resource    |
        +----------------+------------------+
                         | 3306 (privado)
                         v
        +-----------------------------------+
        |  RDS MySQL — sin acceso publico   |
        +-----------------------------------+
```

Las dos nubes no comparten red ni credenciales. El unico puente es el JWKS de
Azure, una URL publica desde la que AWS obtiene las llaves con que verificar
firmas.

Solo el API Gateway ofrece HTTPS. Los microservicios no confian en la red: cada
peticion se autoriza por el token, tanto en el borde como dentro de cada
servicio. Esa duplicacion es deliberada, porque la instancia tiene IP publica y
se puede alcanzar sin pasar por el gateway.

## Componentes

| Servicio         | Puerto | Rol                                                  |
|------------------|--------|------------------------------------------------------|
| `front-angular`  | 80     | SPA Angular 22 con MSAL — repositorio aparte         |
| `ms-auth`        | 9000   | BFF: registra usuarios en el tenant via Graph        |
| `ms-productos`   | 8081   | Catalogo — Resource Server OAuth 2.0                 |
| `ms-pedidos`     | 8082   | Pedidos — Resource Server OAuth 2.0                  |
| RDS MySQL        | 3306   | Persistencia, sin acceso desde Internet              |

## Identidad

El proveedor de identidad del sistema es **Microsoft Entra ID**. Los
microservicios no guardan contrasenias ni saben autenticar: solo verifican la
firma de los tokens contra las llaves publicas del emisor.

| Dato        | Valor                                                       |
|-------------|-------------------------------------------------------------|
| Tenant      | Pedidos360                                                  |
| Emisor      | `https://login.microsoftonline.com/<tenant>/v2.0`           |
| Flujo       | Authorization Code con PKCE, mediante MSAL                  |
| Scopes      | `productos.leer`, `pedidos.escribir`                        |
| Roles       | `ADMIN`, `USER`                                             |

`SecurityConfig` resuelve el validador segun el claim `iss` del token entrante y
acepta una lista de emisores configurable, de modo que sumar o quitar un
proveedor no exige recompilar. En produccion esa lista contiene unicamente a
Entra ID.

### Sobre el Identity Provider propio

Durante el desarrollo se construyo un IdP OIDC completo, con firma RS256 y su
propio JWKS, visible en los commits `V2.0.0` en adelante. Sirvio para levantar y
probar toda la cadena (rutas, CORS, autorizador, resource servers) antes de que
existiera el tenant de Azure.

Se retiro de la superficie publica en `V8.1.0` y su codigo se **elimino** en
`V9.1.0`. Mantenerlo habria dejado un camino de acceso paralelo y mas debil que
el que exige la solucion. El historial de git lo conserva completo.

`ms-auth` conserva un unico proposito: es el BFF que registra usuarios en el
tenant a traves de Microsoft Graph.

### Amazon Cognito

Se integro como Identity as a Service en `V2.2.0`, siguiendo el material de la
asignatura, y esta documentado en `docs/01-cognito.md`. No forma parte de la
solucion entregada: el enunciado pide que el IDaaS sea Azure.

## Stack

- Java 21, Spring Boot 4.1.0, Spring Security 7
- Angular 22 con `@azure/msal-angular` 6
- MySQL 8 sobre Amazon RDS, con JPA e Hibernate
- Docker multietapa y Docker Compose
- AWS EC2, API Gateway (HTTP API) y RDS
- Microsoft Entra ID como IDaaS

## Endpoints

Rutas publicas del API Gateway. El gateway traduce a la ruta interna del
microservicio, que conserva su propio prefijo `/api/v1`.

### Publicas

| Metodo | Ruta               | Descripcion                                    |
|--------|--------------------|------------------------------------------------|
| GET    | `/`                | La aplicacion Angular                          |
| GET    | `/v1/public`       | Comprobacion de despliegue, sin token          |
| POST   | `/auth/registro`   | Crea una cuenta en el tenant via Graph         |

### Protegidas — exigen un token de Entra ID

| Metodo | Ruta                      | Descripcion                                  |
|--------|---------------------------|----------------------------------------------|
| GET    | `/v1/productos`           | Lista el catalogo                            |
| GET    | `/v1/productos/{id}`      | Consulta un producto                         |
| GET    | `/v1/productos/quien-soy` | Emisor, sujeto y claims del token presentado |
| GET    | `/v1/pedidos`             | Pedidos del usuario del token                |
| GET    | `/v1/pedidos/todos`       | Todos los pedidos — exige el rol `ADMIN`     |
| POST   | `/v1/pedidos`             | Crea un pedido                               |

`POST /v1/pedidos` exige el scope `pedidos.escribir` o el rol `ADMIN`, y valida
el producto llamando a `ms-productos` con el mismo token del usuario: la
identidad viaja entre servicios en lugar de confiar en el llamador interno.

El pedido se asocia al claim `sub` del token, nunca a un campo que envie el
cliente, de modo que nadie puede crear ni consultar pedidos a nombre de otro.

### Codigos de respuesta

| Situacion                               | Codigo |
|-----------------------------------------|--------|
| Token valido y permisos suficientes     | 200    |
| Sin token, invalido o expirado          | 401    |
| Token valido sin el rol o el scope      | 403    |
| Peticion que no viene del API Gateway   | 403    |

`GET /v1/pedidos/todos` demuestra el tercer caso: el usuario `cliente` recibe
403 con un token perfectamente valido, que es distinto del 401 de quien no
presenta ninguno.

### Punto de entrada unico

Los microservicios rechazan con 403 cualquier peticion que no traiga la
cabecera `X-Origen-Gateway` que inyecta el API Gateway, de modo que la
instancia no se puede llamar directamente aunque tenga IP publica. El alcance
y las limitaciones de esa medida estan en `docs/03-api-gateway.md`.

### Usuarios de prueba

Viven en el tenant de Entra ID; sus contrasenias estan en `.env`, que no se
commitea.

| Usuario                       | Roles         | Origen                        |
|-------------------------------|---------------|-------------------------------|
| `admin@<tenant>`              | ADMIN, USER   | creado con `az`               |
| `cliente@<tenant>`            | USER          | creado con `az`               |
| `ana.perez@<tenant>`          | USER          | creado desde `/auth/registro` |

## Variables de entorno

| Variable                   | Servicio            | Descripcion                                  |
|----------------------------|---------------------|----------------------------------------------|
| `SEGURIDAD_EMISORES`       | productos, pedidos  | Emisores confiables, separados por coma      |
| `SEGURIDAD_AUDIENCIAS`     | productos, pedidos  | Audiencias aceptadas                         |
| `SEGURIDAD_ORIGENES_CORS`  | productos, pedidos  | Origenes permitidos por CORS                 |
| `PRODUCTOS_URL`            | ms-pedidos          | URL base de ms-productos                     |
| `DB_HOST` … `DB_PASSWORD`  | productos, pedidos  | Conexion a la base de datos                  |
| `GRAPH_*`                  | ms-auth             | Credenciales de la app de backend en Entra   |
| `SEGURIDAD_SECRETO_GATEWAY`| los tres y nginx    | Cabecera que exige que la peticion venga del gateway |

Ver `.env.example`. El archivo `.env` esta en `.gitignore`.

## Probar los endpoints

### Script de humo

Ejecuta las 20 comprobaciones y verifica el codigo HTTP de cada una:

```bash
./scripts/probar-endpoints.sh https://TU-API.execute-api.us-east-1.amazonaws.com/desarrollo
./scripts/probar-endpoints.sh http://localhost      # contra el stack local

# Definir IP_EC2 agrega la comprobacion de que la instancia rechaza las
# llamadas que no vienen del gateway:
IP_EC2=<ip> ./scripts/probar-endpoints.sh https://TU-API...
```

### Thunder Client

1. Menu **Collections** → `...` → **Import** → `thunder-client/thunder-collection_EXP1.json`
2. Pestania **Env** → `...` → **Import** → `thunder-client/thunder-environment_EXP1.json`
3. **Activar el entorno EXP1** marcandolo con el check. Sin entorno activo la
   peticion del token no tiene donde guardarlo y todo lo demas responde 401.
4. Rellenar `passwordAdmin` con la contrasenia del usuario de prueba del tenant.
5. Ejecutar **0. Token de Entra ID**. Su test `set-env-var` guarda el
   `access_token` en `{{token}}` y las demas peticiones lo toman solas.

Cada peticion trae aserciones sobre el codigo HTTP, asi que la barra de tests
queda en verde y sirve directamente como evidencia.

La peticion del token usa el flujo de contrasenia, que no necesita navegador.
El frontend **no** lo usa: emplea Authorization Code con PKCE, que es lo que
exige la evaluacion. El token que devuelven ambos flujos es equivalente y lo
emite el mismo Entra ID.

### Postman

Importar `postman/EXP1.postman_collection.json`, equivalente a la de Thunder
Client. Mismo criterio: ejecutar primero **0. Token de Entra ID**, que guarda el
token en una variable de coleccion, y rellenar antes `passwordAdmin`.

### Evidencias reproducibles

```bash
# verifica PKCE y captura el navegador
PLAYWRIGHT_DIR=/ruta/node_modules node scripts/verificar-pkce.mjs

# codigos 200, 401 y 403, y el punto de entrada unico
IP_EC2=<ip> ./scripts/capturar-codigos.sh <stage>
```

El resultado queda en `docs/evidencias/`. Ver su README para el detalle.

### Navegador

Abrir la URL del stage. El frontend permite iniciar sesion, ver el token con sus
claims decodificados y llamar a cada endpoint mostrando la respuesta.

## Documentacion

| Documento                     | Contenido                                     |
|-------------------------------|-----------------------------------------------|
| `docs/01-cognito.md`          | Cognito (integrado en V2.2.0, fuera del alcance final) |
| `docs/02-entra-id.md`         | Tenant, app registrada, scopes, roles y PKCE  |
| `docs/03-api-gateway.md`      | Rutas, autorizador JWT y la IP cambiante      |
| `docs/04-despliegue-ec2.md`   | La instancia, SSM y el control de costos      |
| `docs/05-base-de-datos.md`    | RDS MySQL, aislamiento de red y esquema       |
| `docs/06-frontend-angular.md` | Angular, MSAL, guard, interceptor y PKCE      |
| `docs/evidencias/`            | Capturas y codigos de respuesta del sistema   |
| `thunder-client/`             | Coleccion y entorno de Thunder Client         |
| `postman/`                    | La misma coleccion en formato Postman         |
| `scripts/`                    | Despliegue, pruebas y correccion de la IP      |

## Como ejecutar en local

El backend necesita una base de datos. `docker-compose.local.yml` agrega un
contenedor MySQL y apunta los servicios a el:

```bash
cp .env.example .env        # y rellenar los valores
docker compose -f docker-compose.yml -f docker-compose.local.yml up --build -d
```

Comprobar. En local se llama directamente a cada microservicio, asi que las
rutas llevan su prefijo real `/api/v1`, no el `/v1` que publica el gateway:

```bash
curl http://localhost:8081/api/v1/public
curl http://localhost:8081/api/v1/productos -H "Authorization: Bearer <token de Entra>"
```

El frontend se ejecuta desde su propio repositorio:

```bash
git clone https://github.com/1samadhi/cloud-exp1-front-angular
cd cloud-exp1-front-angular && npm install && npm start
```

`http://localhost:4200` esta registrado como redireccion en Entra ID y
autorizado en el CORS del API Gateway, de modo que el frontend en desarrollo
consume la API desplegada en AWS.

Detener:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml down
```

## Despliegue en AWS

```bash
# Tras cada reinicio del laboratorio la EC2 recibe una IP publica nueva
./scripts/actualizar-api-gateway.sh <API_ID> <INSTANCE_ID>
./scripts/desplegar-en-ec2.sh <INSTANCE_ID>
```

El primero corrige las integraciones del gateway; el segundo clona ambos
repositorios en la instancia, construye la imagen del frontend y levanta el
stack. Ninguno necesita SSH: operan por AWS Systems Manager.

La regla de firewall del RDS apunta al security group de la instancia y no a
una direccion, asi que el cambio de IP no la afecta.

## Persistencia

El catalogo y los pedidos viven en **Amazon RDS MySQL**. La instancia no tiene
acceso publico: solo la alcanzan los microservicios de la EC2. Las entidades,
los repositorios y el aislamiento de red estan documentados en
`docs/05-base-de-datos.md`.
