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
| `04-sesion-iniciada.png`       | Sesion activa tras volver de Microsoft                |
| `05-catalogo.png`              | Productos servidos desde RDS con el token adjunto    |
| `06-pedidos.png`               | Pedidos del usuario del token                        |
| `07-claims-del-token.png`      | id_token, access_token y lo que ve el microservicio  |
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

## Sobre Security Defaults

El tenant venia con **Security Defaults** activo, que obliga a registrar MFA en
el primer inicio de sesion interactivo y no ofrece opcion de omitir. Se
desactivo desde el portal para poder demostrar el flujo.

Vale la pena conocer el detalle porque aparece en los codigos de respuesta: el
flujo de contrasenia (`grant_type=password`) seguia devolviendo token aun con la
politica activa, asi que las pruebas por HTTP pasaban sin tropezar. El muro
solo aparecia en el navegador. Desactivarla tambien era necesario para cualquier
demostracion en vivo: la misma pantalla habria aparecido delante del evaluador.
