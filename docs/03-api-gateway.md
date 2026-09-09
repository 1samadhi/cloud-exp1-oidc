# AWS API Gateway como API Manager

El API Gateway es el unico punto de entrada publico. Ofrece HTTPS, enruta hacia
cada microservicio y aplica el autorizador JWT antes de que la peticion llegue a
la EC2.

## Lo que quedo creado

| Recurso  | Valor                                          |
|----------|------------------------------------------------|
| HTTP API | `j37oj1wn16`                                    |
| Stage    | `desarrollo` con auto-deploy                   |
| URL base | `https://j37oj1wn16.execute-api.us-east-1.amazonaws.com/desarrollo` |

## Rutas

| Metodo | Ruta                                  | Destino            | Autorizador |
|--------|---------------------------------------|--------------------|-------------|
| GET    | `/.well-known/openid-configuration`   | ms-auth :9000      | No          |
| GET    | `/.well-known/jwks.json`              | ms-auth :9000      | No          |
| POST   | `/auth/login`                         | ms-auth :9000      | No          |
| GET    | `/auth/userinfo`                      | ms-auth :9000      | Si          |
| GET    | `/v1/public`                      | ms-productos :8081 | No          |
| GET    | `/v1/productos`                   | ms-productos :8081 | Si          |
| GET    | `/v1/productos/quien-soy`         | ms-productos :8081 | Si          |
| GET    | `/v1/productos/{id}`              | ms-productos :8081 | Si          |
| GET    | `/v1/pedidos`                     | ms-pedidos :8082   | Si          |
| POST   | `/v1/pedidos`                     | ms-pedidos :8082   | Si          |
| GET    | `/`                                   | front-end :80      | No          |
| ANY    | `/{proxy+}`                           | front-end :80      | No          |

La ruta comodin `/{proxy+}` sirve los assets del SPA. Las rutas literales tienen
prioridad sobre ella, asi que `/v1/productos` sigue llegando a su servicio.

## El autorizador JWT

Se crea con dos datos que se leen del propio token (se pueden ver decodificandolo
en jwt.io o con el panel de tokens del frontend):

- **URL del emisor**: el claim `iss`.
- **Audiencia**: el claim `aud`.

AWS descarga `{iss}/.well-known/openid-configuration`, lee de ahi el `jwks_uri`,
baja las llaves publicas y con ellas verifica la firma de cada token entrante.

### Por que el issuer del IdP propio es la URL del stage

Hay una dependencia circular que conviene entender antes de configurarlo:

1. El autorizador necesita alcanzar el discovery del emisor **por HTTPS publico**.
2. `ms-auth` corre en la EC2, en HTTP y detras del gateway.
3. La unica URL publica y con HTTPS que tiene es la del propio stage.

Por eso `ms-auth` arranca con `OIDC_ISSUER` igual a la URL del stage y firma sus
tokens con ese `iss`; y las rutas `/.well-known/*` se publican sin autorizador,
para que AWS pueda leerlas. El orden correcto es: crear la API y el stage, tomar
la URL, desplegar `ms-auth` con esa URL como issuer y recien entonces crear el
autorizador.

### Un autorizador acepta un solo emisor

El autorizador JWT de una HTTP API valida **un** issuer. No existe forma de que
una misma ruta acepte tokens del IdP propio y de Cognito a la vez.

La solucion adoptada reparte la responsabilidad en dos capas:

- **En el borde (API Gateway):** autorizador con el IdP propio. Filtra el trafico
  no autenticado antes de que consuma recursos de la EC2.
- **En cada microservicio (Spring):** validacion multi emisor, que acepta los
  tres. Es la que permite que un token de Cognito o de Entra ID funcione.

Para exponer tambien Cognito o Entra en el borde se crea un autorizador adicional
y se asocia a las rutas que corresponda; no hay que tocar codigo.

## La IP publica cambia en cada sesion del laboratorio

Al cerrar la sesion de AWS Academy la instancia se detiene, y al volver recibe
otra IP publica. Todas las integraciones quedan apuntando a una direccion muerta
y el stage responde **503**.

Una Elastic IP lo resolveria, pero una EIP asociada a una instancia detenida se
cobra por hora y a lo largo del semestre sale mas cara que ejecutar:

```bash
./scripts/actualizar-api-gateway.sh j37oj1wn16 i-0314ddd12fadeb125
```

El stage tiene auto-deploy, asi que los cambios quedan activos de inmediato.

## Rutas limpias y versionadas

La ruta que se publica no es la misma que expone el microservicio:

| Ruta publica (API Gateway) | Destino interno (EC2)                  |
|----------------------------|----------------------------------------|
| `GET /v1/productos`        | `http://IP:8081/api/v1/productos`      |
| `GET /v1/productos/{id}`   | `http://IP:8081/api/v1/productos/{id}` |
| `GET /v1/productos/quien-soy` | `http://IP:8081/api/v1/productos/quien-soy` |
| `GET /v1/public`           | `http://IP:8081/api/v1/public`         |
| `GET /v1/pedidos`          | `http://IP:8082/api/v1/pedidos`        |
| `POST /v1/pedidos`         | `http://IP:8082/api/v1/pedidos`        |

Separar ambas cosas es justamente lo que aporta un API Manager. La version vive
en la ruta publica, de modo que publicar una `v2` es apuntar `GET /v2/productos`
a otra integracion sin que los clientes de `v1` se enteren, y sin que los
microservicios tengan que coordinar sus rutas internas entre si.

Al elegir la ruta, API Gateway prefiere los segmentos literales sobre las
variables: `/v1/productos/quien-soy` gana sobre `/v1/productos/{id}`, asi que
"quien-soy" no se interpreta como un identificador.

## Punto de entrada unico

El enunciado exige que el API Gateway sea el unico punto de entrada. La
instancia EC2, sin embargo, tiene IP publica y expone los puertos de cada
microservicio, asi que un `curl` directo se saltaba el gateway y su autorizador.

La solucion correcta es una subred privada con **VPC Link**, que exige un
balanceador y cuesta del orden de USD 16 al mes: se come el presupuesto de la
cuenta academica. En su lugar:

```
integracion del gateway  ──append:header.X-Origen-Gateway=<secreto>──▶  EC2
                                                                        │
                                          FiltroOrigenGateway  ─────────┘
                                          sin la cabecera → 403
```

El filtro se ejecuta **antes** de la cadena de Spring Security, de modo que el
trafico ajeno ni siquiera llega a la validacion del token.

Comprobacion:

```bash
# con un token perfectamente valido, directo a la instancia
curl -H "Authorization: Bearer $TOKEN" http://<ip>:8081/api/v1/productos
# -> 403 {"error":"Esta API solo acepta peticiones a traves del API Gateway"}
```

**Que no es esto.** No es aislamiento de red y no conviene presentarlo como tal:
quien conozca el secreto puede seguir llamando directamente, y el secreto viaja
en claro entre el gateway y la instancia porque la integracion es HTTP. Lo que
consigue es que el gateway sea el unico camino funcional y que las reglas del
borde no se puedan eludir. Es una mitigacion consciente frente a una restriccion
de presupuesto, no la arquitectura ideal.

El puerto 22 quedo cerrado: la instancia se opera por AWS Systems Manager.

### Llamadas entre microservicios

ms-pedidos valida el producto llamando a ms-productos. Esa peticion es interna y
tampoco pasa por el gateway, asi que `CatalogoClient` reenvia la misma cabecera.
Sin eso, ms-productos la rechazaria con 403 y el pedido se rechazaria con un 400
enganioso: "el producto no existe".

## Codigos de respuesta

La pauta pide demostrar 200, 401 y 403 coherentes:

| Situacion                                        | Codigo | Quien lo emite            |
|--------------------------------------------------|--------|---------------------------|
| Token valido y permisos suficientes              | 200    | el microservicio          |
| Sin token, o token invalido o expirado           | 401    | el autorizador del gateway |
| Token valido pero sin el rol o el scope          | 403    | Spring Security           |
| Peticion que no viene del gateway                | 403    | FiltroOrigenGateway       |

`GET /v1/pedidos/todos` existe para demostrar el tercer caso: exige el rol
`ADMIN`, de modo que el usuario `cliente` recibe 403 con un token perfectamente
valido. Es la diferencia entre "no se quien eres" y "se quien eres y no puedes".
