# Microsoft Entra ID (Azure AD) como tercer emisor

Entra ID es el emisor que usa el frontend: el usuario inicia sesion con su
cuenta Microsoft mediante MSAL y el token resultante viaja a los microservicios.

## Configuracion en el portal de Azure

### 1. Registrar la aplicacion

`Entra ID` → `Registros de aplicaciones` → `Nuevo registro`.

**Critico:** en `Redirect URI` hay que elegir el tipo **SPA (aplicacion de una
sola pagina)**, no `Web`. Si se elige `Web`, MSAL falla en el navegador porque
Azure espera un client secret que una SPA no puede guardar.

Como URI de redireccion va la URL del frontend. En desarrollo
`http://localhost:5173`; en AWS, la URL del stage del API Gateway (ver
`docs/03-api-gateway.md`), con la barra final incluida.

De la pagina de inicio del registro se anotan:

| Dato                        | Donde se usa                                |
|-----------------------------|---------------------------------------------|
| Directory (tenant) ID        | `ENTRA_TENANT_ID`, y el issuer              |
| Application (client) ID      | `ENTRA_CLIENT_ID`, y el `aud` de los tokens |

### 2. Exponer la API

`Exponer una API` → `Agregar un ambito`. Esto define los scopes que el frontend
puede pedir para consumir **nuestro** backend.

Se crea el Application ID URI (queda como `api://<client-id>`) y dentro un
ambito, por ejemplo `acceso.api`.

Por que hace falta: un token pedido con scopes de Microsoft Graph
(`User.Read`) **no** sirve para el backend. Graph devuelve tokens cifrados que
Spring no puede validar. Solo un token pedido con un scope de *nuestra* API
viene firmado por el tenant en formato JWT verificable.

### 3. Secreto de cliente (solo para pruebas maquina a maquina)

`Certificados y secretos` → `Nuevo secreto de cliente`. El valor se muestra una
sola vez. Sirve para reproducir en Postman el flujo `client_credentials`:

```
POST https://login.microsoftonline.com/{TENANT_ID}/oauth2/v2.0/token
grant_type=client_credentials
client_id={CLIENT_ID}
client_secret={SECRETO}
scope=api://{CLIENT_ID}/.default
```

El frontend con MSAL **no** usa este secreto: una SPA no puede custodiarlo.

## Conectarlo a los microservicios

El issuer de Entra ID v2 es:

```
https://login.microsoftonline.com/{TENANT_ID}/v2.0
```

Se agrega a la lista, sin tocar codigo:

```bash
SEGURIDAD_EMISORES=http://localhost:9000,https://cognito-idp.us-east-1.amazonaws.com/{POOL_ID},https://login.microsoftonline.com/{TENANT_ID}/v2.0
```

## Diferencias entre los tres emisores

Todas quedan absorbidas por `SecurityConfig`, que normaliza a `SCOPE_x` y `ROLE_x`:

| Aspecto             | IdP propio        | Cognito                        | Entra ID              |
|---------------------|-------------------|--------------------------------|-----------------------|
| Claim de scopes     | `scope`           | `scope`                        | `scp`                 |
| Formato del scope   | `pedidos.leer`    | `exp1-api/pedidos.leer`        | `acceso.api`          |
| Claim de roles      | `roles`           | `cognito:groups`               | `roles`               |
| Claim `aud`         | Si                | **No** en `client_credentials` | Si                    |

La ausencia de `aud` en los tokens `client_credentials` de Cognito es la razon
de que la validacion de audiencia sea opcional (`seguridad.audiencias`): si se
exigiera siempre, el flujo maquina a maquina de Cognito devolveria 401.
