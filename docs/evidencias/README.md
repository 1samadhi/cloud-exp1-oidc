# Evidencias de funcionamiento

Generadas por los scripts del proyecto, no a mano, para que se puedan rehacer.

```bash
# PKCE y capturas del navegador
PLAYWRIGHT_DIR=/ruta/node_modules node scripts/verificar-pkce.mjs

# codigos de respuesta del sistema
IP_EC2=<ip> EXP1_URL=<stage> ./scripts/capturar-codigos.sh
```

| Archivo                        | Que demuestra                                        |
|--------------------------------|------------------------------------------------------|
| `01-inicio.png`                | La aplicacion servida por el API Gateway             |
| `02-login-microsoft.png`       | Redireccion a Entra ID para iniciar sesion           |
| `03-peticion-authorize.txt`    | **Authorization Code con PKCE**                      |
| `08-codigos-http.txt`          | 200, 401 y 403 coherentes, y el punto de entrada unico |

## Sobre PKCE

`03-peticion-authorize.txt` es la evidencia central del flujo. Que el login
funcione no demuestra PKCE: un flujo implicito tambien iniciaria sesion. Lo que
lo demuestra es la peticion al endpoint de autorizacion:

```
response_type        = code     -> Authorization Code, no implicito
code_challenge       = wKgfDWE2Xsjbnw0Vb50XLGrTVfQYtl9D9VRU-B1J_qk
code_challenge_method = S256    -> viaja el hash, nunca el secreto
```

No aparece ningun `client_secret`, y no puede aparecer: el JavaScript de una
aplicacion de pagina unica es publico. Esa es exactamente la razon de ser de
PKCE.

## Capturas del recorrido con sesion iniciada

Faltan las de `/catalogo`, `/pedidos` y `/perfil` con la sesion abierta. El
script se detiene antes porque el tenant tiene **Security Defaults** activo y
exige registrar MFA en el primer inicio de sesion interactivo, sin opcion de
omitir.

Es una politica del directorio, no un fallo del sistema, y conviene resolverla
antes de cualquier demostracion en vivo:

- **Desactivar Security Defaults**: `entra.microsoft.com` → Identity →
  Overview → Properties → Manage security defaults → Disabled.
- **O registrar MFA** en la cuenta de prueba con Microsoft Authenticator.

Hecho eso, `scripts/verificar-pkce.mjs` completa el recorrido y genera
`04-sesion-iniciada.png`, `05-catalogo.png`, `06-pedidos.png` y
`07-claims-del-token.png`.

Detalle util: el flujo de contrasenia (`grant_type=password`) sigue devolviendo
token, por eso las pruebas automatizadas por HTTP pasan sin tropezar con MFA.
El muro aparece solo en el inicio de sesion desde el navegador.
