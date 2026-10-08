# Registro de cambios

El versionado sigue [SemVer](https://semver.org/lang/es/): MAYOR.MENOR.PARCHE.

## [10.4.0]

- Un catalogo inaccesible responde **502** y ya no el 400 de "el producto no
  existe". Antes, cualquier falla al consultar `ms-productos` (401, 403, timeout,
  servicio caido) se traducia en producto inexistente, de modo que una caida del
  catalogo se veia como un error del usuario. Ahora solo el 404 significa eso, y
  el resto sube como `CatalogoNoDisponibleException`. En `ms-carrito` y en
  `ms-ordenes`, que nacio con el mismo defecto.
- `CatalogoClient` con timeouts de 2 s para conectar y 5 s para leer. Sin ellos,
  un catalogo caido dejaba el hilo de la peticion colgado hasta que cortaba el
  cliente o el gateway.
- `POST /v1/carrito` valida que venga el `productoId` antes de llamar al
  catalogo.
- `AudienciaDelTokenTest`: fija que el claim `aud` se lea igual venga como
  cadena (Entra ID v2) o como lista (Cognito, client_credentials). El validador
  actual ya lo resuelve usando `getAudience()`, que normaliza ambos formatos;
  las pruebas existen para que un refactor no vuelva a leer el claim crudo y
  reviva el ClassCastException que devolvia 500.

  Diagnostico del problema del `aud` y del 502: Diego Villota, rama
  `fix/auditoria-v9`.

## [10.3.0]

- `ms-ordenes`: nuevo microservicio, productor de la mensajeria. Registra la
  orden como hecho historico y publica `orden.creada` en el exchange topic
  `pedidos360.ordenes`. A diferencia del carrito, que es mutable y se vacia, la
  orden no se modifica: es lo que respalda la boleta y el descuento de stock,
  asi que guarda el precio unitario de cada linea en vez de mirar el catalogo
  cada vez.
- El precio lo pone el catalogo, nunca la peticion: si viniera del cliente,
  cualquiera podria comprar al valor que quisiera.
- `config/RabbitConfig`: la topologia completa, un bloque por caso de uso.
  Tres colas (`productos.stock`, `notificaciones.correo`, `facturacion.ordenes`),
  cada una con su DLQ via `x-dead-letter-exchange`, los exchanges `topic` y
  `direct`, y el `pedidos360.dlx`. Sin la DLX, lo que un consumidor rechaza se
  descarta en silencio.
- Los nombres salen de `application.yml` leidos con `@ConfigurationProperties`
  (`pedidos360.rabbit.*`). Ninguno se escribe a mano en el codigo: un nombre
  repetido en dos clases se desincroniza y el sintoma es un mensaje que se
  publica bien y que nadie consume, sin error en ningun log.
- Guardar y publicar van en la misma transaccion. Si el broker esta caido, la
  orden no queda registrada: una orden que nadie procesa deja stock sin
  descontar y sin boleta, y detectarla despues exige comparar base contra broker
  a mano.
- El convertidor JSON es `JacksonJsonMessageConverter`, no el `Jackson2...`:
  Spring Boot 4 trae Jackson 3 y el converter viejo busca clases de
  `com.fasterxml` que ya no estan en el classpath.
- 8 pruebas del controlador, con el publicador sustituido por un doble para no
  exigir un broker levantado.

## [10.2.0]

- El API Gateway valida el JWT en el borde. Autorizador `entra-jwt` (tipo JWT,
  emisor de Entra, audiencias `api://<clientId>` y `<clientId>`) asociado a
  `/v1/productos`, `/v1/carrito` y sus comodines. Antes las nueve rutas estaban
  en `AuthorizationType: NONE` y el token lo revisaba unicamente Spring: un
  token basura llegaba a consumir CPU de la EC2 antes del rechazo.
- `/v1/public`, `/`, `/{proxy+}` y `/auth/registro` siguen abiertas. El registro
  no puede exigir token porque quien se registra todavia no tiene cuenta.
- Verificado contra el stage: sin token, con token mal formado y con token de
  otro emisor responde 401 desde el gateway; con un token real de Entra,
  `/v1/productos` responde 200.
- Queda anotada en `docs/03-api-gateway.md` una limitacion del diseño: un
  autorizador JWT admite un solo emisor, asi que los usuarios de Cognito reciben
  401 en el gateway aunque los microservicios los acepten.

## [10.1.0]

- `scripts/renombrar-rutas-carrito.sh`: renombra en el API Gateway las rutas
  `/v1/pedidos` a `/v1/carrito` y corrige el path de las integraciones, que
  apuntan a `/api/v1/pedidos` en la EC2. Sin esto el stage responde 404 desde el
  propio microservicio, porque `ms-carrito` ya no expone ese path. Corre en
  simulacion por defecto y solo cambia algo con `--aplicar`.

## [10.0.0]

- Punto de partida de la Evaluacion 2. `ms-pedidos` pasa a `ms-carrito`: el
  paquete queda en `cl.duoc.cloud.carrito`, la entidad `Pedido` en `ItemCarrito`
  y la tabla en `carrito_items`. El servicio guardaba cliente, producto y
  cantidad, que es un item de carrito y no una orden; el nombre "orden" queda
  libre para el microservicio que publica los eventos de mensajeria.
- Las rutas pasan de `/v1/pedidos` a `/v1/carrito`. Rompe el contrato con el
  frontend y con el API Gateway, de ahi la version mayor. El prefijo sigue
  siendo `v1` porque es un recurso nuevo de esta linea de trabajo, no una
  segunda version del anterior.
- `ms-carrito` sube a 3.0.0 y su imagen a `exp1/ms-carrito:4.0.0`. El resto de
  los servicios queda igual: su codigo no cambia.
- Los scopes del tenant (`pedidos.leer`, `pedidos.escribir`), el resource server
  `exp1-api` y la base de datos `pedidos360` conservan su nombre: estan
  declarados en el IDaaS y en RDS, y renombrarlos obliga a reconfigurar el
  tenant y volver a emitir los tokens.
- `docker-compose.rabbitmq.yml` y `rabbitmq/rabbitmq.conf`: cluster de dos nodos
  con el panel en 15672 y 15673. Los microservicios apuntan a los dos nodos con
  `SPRING_RABBITMQ_ADDRESSES` para seguir funcionando si uno cae.
- `rabbitmq/TOPOLOGIA.md` fija los nombres de exchanges, colas, bindings y DLQ,
  y el contrato del mensaje `orden.creada`. Ningun nombre se escribe a mano en
  el codigo: cada servicio los lee de su `application.yml`.

## [9.5.0]

- Cognito vuelve como segundo emisor confiable, ahora para usuarios externos que
  se registran solos con su correo. Entra ID sigue siendo el de la institucion.
- La audiencia se exige por defecto. Acepta el claim `aud` o `client_id`, porque
  los access token de Cognito no traen `aud`.
- Quien no trae roles ni grupos queda como `USER`. `POST /v1/pedidos` acepta el
  scope `pedidos.escribir` o el rol `USER` o `ADMIN`.
- Se deja de versionar el archivo de bloqueo de LibreOffice.

## [9.2.0]

- `scripts/verificar-pkce.mjs`: abre la aplicacion con un navegador, intercepta
  la peticion al endpoint de autorizacion de Entra y comprueba que lleve
  `response_type=code`, `code_challenge` y `code_challenge_method=S256`. Que el
  login funcione no demuestra PKCE: un flujo implicito tambien iniciaria sesion.
- `scripts/capturar-codigos.sh`: deja por escrito los 200, 401 y 403 del
  sistema, incluidos los dos motivos distintos de 403.
- `docs/evidencias/` con el resultado y un README que explica cada archivo.

## [9.1.1]

- El filtro de origen llevaba `@Order(1)`, pero la cadena de Spring Security
  esta en el orden -100, asi que corria despues. Una peticion directa sin token
  recibia 401 de Security y con token 403 del filtro: dos codigos para el mismo
  motivo. Con `Ordered.HIGHEST_PRECEDENCE` el rechazo es siempre 403.

## [9.1.0]

- Se elimina el codigo del Identity Provider propio: `AuthController`,
  `DiscoveryController`, `TokenService`, `ClaveService`, `UsuarioService`,
  `Usuario`, `LoginDTO` y `TokenDTO`, con su configuracion `oidc`, la
  dependencia de `oauth2-resource-server` y el volumen de llaves del compose.
- `ms-auth` queda con un unico proposito: el BFF que registra usuarios en el
  tenant a traves de Microsoft Graph.
- Se completa el punto de entrada unico. El filtro de origen solo estaba en
  ms-productos y ms-pedidos, de modo que `/auth/registro` y el frontend seguian
  siendo alcanzables sin pasar por el gateway. Como ese endpoint crea usuarios
  en el tenant, era el peor de los tres.

## [9.0.2]

- `docs/03-api-gateway.md` documenta la cabecera del gateway, por que se eligio
  frente a VPC Link y, sobre todo, que no es: no es aislamiento de red.
- Tabla de codigos 200, 401 y 403 y que capa emite cada uno.
- CORS restringido al dominio del frontend, sin `localhost`.

## [9.0.1]

- El filtro de origen habia roto la comunicacion entre microservicios:
  ms-pedidos validaba el producto contra ms-productos y esa llamada interna no
  llevaba la cabecera, asi que crear un pedido respondia 400 en lugar de 201.
  `CatalogoClient` reenvia el secreto.

## [9.0.0]

Cambio mayor: el API Gateway pasa a ser el unico punto de entrada.

- El gateway inyecta `X-Origen-Gateway` en cada integracion y los
  microservicios rechazan con 403 lo que llegue sin ella.
- Se cierra el puerto 22, abierto al mundo sin utilidad.
- `GET /v1/pedidos/todos`, reservado al rol ADMIN, permite demostrar el 403 con
  token valido que pide la pauta.

## [8.1.3]

- El script comprobaba `GET /auth/userinfo`, ruta retirada en 8.1.0. Al no
  existir caia en el comodin del frontend y nginx devolvia 200. Se reemplaza
  por comprobar que `POST /auth/login` responda 404.

## [8.1.2]

- Se regenera la coleccion de Postman, que seguia pidiendo tokens al IdP propio
  y contradecia a la de Thunder Client.

## [8.1.1]

- El README describia un sistema que ya no existia: frontend React, datos en
  memoria, tres emisores activos y las credenciales del IdP propio. Se corrige.
- La coleccion de Thunder Client pedia tokens que ahora se rechazan, de modo
  que la evidencia habria salido en rojo. La peticion de registro usa
  `{{$timestamp}}` para poder repetirse.

## [8.1.0]

- Se retira el Identity Provider propio de la superficie publica: sus rutas
  salen del gateway, su emisor sale de `SEGURIDAD_EMISORES` y `UsuarioService`
  deja de traer usuarios incorporados. Las credenciales de prueba estaban
  escritas en el codigo de un repositorio publico y el login estaba expuesto
  sin autorizador.

## [8.0.0]

Cambio mayor: el proyecto se entrega en dos repositorios, uno por componente.

- El frontend Angular se traslada a `cloud-exp1-front-angular`, con su propio
  historial de versiones.
- Se retira el frontend de React, que ya no forma parte del sistema.
- `desplegar-en-ec2.sh` clona y actualiza ambos repositorios en la instancia.

## [7.1.0]

- El API Gateway no tenia CORS configurado: existia en Spring, pero la pauta lo
  pide en el API Manager. Se configura con origenes explicitos.
- `ANY /{proxy+}` capturaba el preflight OPTIONS y nginx respondia 405. Se
  restringe a GET para que el gateway lo atienda por su cuenta.
- Se publica `POST /auth/registro`, que existia en el backend sin exponerse.

## [7.0.0]

Cambio mayor: el frontend pasa de React a Angular.

- Angular 22 con `@azure/msal-angular`: `MsalGuard` protege las rutas privadas
  y `protectedResourceMap` asocia cada URL con su scope, de modo que ningun
  servicio manipula tokens.
- Authorization Code con PKCE no se programa: MSAL lo aplica porque la
  aplicacion esta registrada en Entra con redirecciones de tipo `spa`.
- `POST /auth/registro` crea cuentas en el tenant mediante Microsoft Graph con
  el flujo `client_credentials`. Una SPA no podria hacerlo sin exponer un
  secreto.

## [6.0.0]

Cambio mayor: Microsoft Entra ID es el Identity as a Service del sistema.

- Tenant Pedidos360 con la aplicacion registrada como `spa`, sus scopes, sus
  roles y `requestedAccessTokenVersion: 2`.
- Autorizador JWT del gateway apuntado al JWKS de Azure.
- El codigo Java no cambia: `SecurityConfig` elegia el validador por el claim
  `iss` y ya leia el claim `scp` de Entra, asi que migrar fue editar dos
  variables de entorno.

## [5.0.0]

Cambio mayor: cambia la URL publica de la API.

- Las rutas del gateway dejan de ser un espejo del backend. La version vive en
  la ruta publica (`/v1/productos`) y el gateway traduce al path interno, de
  modo que publicar una `v2` no afecta a los clientes de `v1`.

## [4.1.1]

- El servicio `mysql` estaba en el compose principal, asi que desplegarlo en la
  EC2 habria levantado un motor al lado del RDS. Se separa en
  `docker-compose.local.yml`.

## [4.1.0]

- Base de datos en **Amazon RDS MySQL**, sin acceso publico: el puerto 3306 se
  abre hacia el security group de la EC2 y no hacia un rango de IP.
- Usuario de aplicacion con permisos solo sobre su esquema.

## [4.0.0]

Cambio mayor: los servicios ya no arrancan sin base de datos.

- Entidades JPA, repositorios de Spring Data y propiedades de conexion por
  variables de entorno. Antes el estado vivia en listas en memoria y se perdia
  al reiniciar el contenedor.

## [3.2.0]

- Colecciones de Thunder Client y Postman y `scripts/probar-endpoints.sh`, con
  las aserciones de codigo HTTP de cada ruta.

## [3.1.0]

- Despliegue en **EC2 t3.small** con Docker Compose, operado por **SSM** sin SSH.
- **AWS API Gateway** (HTTP API) como unico punto de entrada publico, con doce
  rutas y una integracion por ruta.
- **Autorizador JWT** en el borde, con el issuer del Identity Provider propio.
  Las rutas de discovery quedan publicas para que AWS pueda descargar el JWKS.
- Segundo autorizador preconfigurado para Cognito, listo para asociar a rutas.
- `scripts/actualizar-api-gateway.sh`: re-apunta las integraciones cuando la EC2
  cambia de IP publica al reiniciarse el laboratorio.
- `scripts/desplegar-en-ec2.sh`: despliegue remoto por SSM.
- `docs/03-api-gateway.md` y `docs/04-despliegue-ec2.md`.

## [3.0.0]

Cambio mayor: la solucion deja de ser solo backend y suma frontend.

- Frontend **React + Vite** con **MSAL** (`@azure/msal-browser`, `@azure/msal-react`).
- Login dual: boton de Microsoft (Entra ID) y formulario contra el IdP propio,
  para evidenciar en pantalla que el mismo Resource Server acepta ambos emisores.
- Panel que muestra el token vigente, su emisor y sus claims decodificados.
- Panel para consumir la API protegida y ver los codigos de respuesta.
- Ningun identificador de Azure queda hardcodeado: todo sale de variables `VITE_*`.
  La app arranca y avisa si Entra ID no esta configurado, en vez de romperse.
- `Dockerfile` multistage con nginx y `base: './'` en Vite, necesario para servir
  la app detras del stage del API Gateway sin romper las rutas de los assets.
- `docker-compose.yml` levanta el stack completo, con volumen para la llave del IdP.

## [2.4.0]

- CORS configurable en ambos Resource Server (`seguridad.origenes-cors`). Sin
  esto el navegador aborta la peticion en el preflight `OPTIONS` y el frontend
  nunca alcanza a enviar el header `Authorization`.
- Se permiten solo los metodos y cabeceras que la aplicacion usa, y se limita a
  los origenes configurados: un origen desconocido recibe 403 en el preflight.

## [2.3.0]

- Se incorpora **Microsoft Entra ID** como tercer emisor confiable. El
  convertidor ya contemplaba su claim `scp` y sus `roles`.
- Validacion de **audiencia** configurable via `seguridad.audiencias`. Queda
  desactivada por defecto porque los tokens `client_credentials` de Cognito no
  llevan claim `aud` y exigirla romperia el flujo maquina a maquina.
- Los decodificadores se construyen con `NimbusJwtDecoder.withIssuerLocation`
  para poder encadenar validadores propios sobre los de firma, expiracion y emisor.
- `docs/02-entra-id.md` con la configuracion del portal y la tabla de diferencias
  entre los tres emisores.

## [2.2.0]

- Se incorpora **Amazon Cognito** como segundo emisor confiable (Identity as a
  Service), con User Pool, resource server `exp1-api` y cliente maquina a maquina
  con flujo `client_credentials`.
- El convertidor de autoridades recorta el prefijo que Cognito antepone a los
  scopes (`exp1-api/pedidos.escribir` pasa a `SCOPE_pedidos.escribir`), sin lo
  cual el token seria valido pero `@PreAuthorize` devolveria 403.
- `docs/01-cognito.md` con el paso a paso y los identificadores del pool.
- `.env.example` con las variables necesarias.
- Los microservicios no cambiaron para aceptar Cognito: solo su configuracion.

## [2.1.0]

- `ms-productos` y `ms-pedidos` pasan a ser **Resource Server OAuth 2.0**: ya no
  gestionan contrasenias, solo verifican la firma del JWT contra las llaves
  publicas del emisor.
- Resolucion **multi emisor** (`JwtIssuerAuthenticationManagerResolver`): la lista
  de emisores confiables es configuracion (`SEGURIDAD_EMISORES`), no codigo.
- Los scopes y roles de cualquier emisor se normalizan a `SCOPE_x` y `ROLE_x`.
- Autorizacion por scope con `@PreAuthorize` en `POST /api/v1/pedidos`.
- `GET /api/v1/public` queda abierto en ambos servicios para comprobar despliegues.
- `ms-pedidos` valida el producto contra `ms-productos` **propagando el token**
  del usuario en lugar de usar una credencial de servicio.
- Los pedidos se asocian al `sub` del token, no a un campo enviado por el cliente.

## [2.0.0]

Cambio mayor: `ms-auth` deja de ser un servicio de estado y pasa a ser un
Identity Provider OIDC completo.

- Firma de tokens en **RS256** con par de llaves RSA de 2048 bits. Se abandona
  HS256 porque el autorizador JWT de AWS API Gateway valida la firma con llaves
  publicas descargadas del JWKS, y un secreto simetrico no le sirve.
- `POST /auth/login` emite `access_token` e `id_token` con claims OIDC.
- `GET /.well-known/openid-configuration` y `GET /.well-known/jwks.json`
  publican los metadatos y las llaves publicas del emisor.
- `GET /auth/userinfo` protegido con el propio token.
- Contrasenias codificadas con BCrypt.
- La llave RSA se persiste en un volumen para que sobreviva a los reinicios.

## [1.1.1]

- Correccion: `GET /api/v1/productos/{id}` respondia 500 ante un id inexistente porque
  `orElseThrow()` propagaba `NoSuchElementException`. Ahora responde 404.
- Pruebas de `ProductoV1Controller` cubriendo los casos 200 y 404.

## [1.1.0]

- `ms-productos`: nuevo endpoint `GET /api/v1/productos/{id}` para consultar un producto puntual.
- `ms-pedidos`: nuevo endpoint `POST /api/v1/pedidos` para registrar un pedido.

## [1.0.0]

- Creacion de los tres microservicios base: `ms-auth`, `ms-productos` y `ms-pedidos`.
- Cada servicio expone un endpoint GET que responde en formato JSON.
- Contenerizacion con Dockerfile multistage (build con Maven, runtime con JRE 21 y usuario no root).
- Orquestacion local con Docker Compose.
