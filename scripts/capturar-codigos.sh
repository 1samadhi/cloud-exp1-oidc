#!/usr/bin/env bash
#
# Genera docs/evidencias/08-codigos-http.txt: la evidencia que pide el
# entregable sobre el bloqueo de peticiones sin token frente al exito de las
# autorizadas, mas los dos casos de 403.
#
# Uso:  IP_EC2=<ip> ./scripts/capturar-codigos.sh [url-del-stage]
#
set -uo pipefail
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE=${1:-${EXP1_URL:?Define EXP1_URL o pasa la URL como argumento}}
SALIDA="$RAIZ/docs/evidencias/08-codigos-http.txt"

set -a; source "$RAIZ/.env" >/dev/null 2>&1; set +a

pedir_token() {
  curl -s -m 25 -X POST "https://login.microsoftonline.com/$ENTRA_TENANT_ID/oauth2/v2.0/token" \
    -d "client_id=$ENTRA_CLIENT_ID" -d "grant_type=password" \
    -d "scope=api://$ENTRA_CLIENT_ID/productos.leer api://$ENTRA_CLIENT_ID/pedidos.escribir" \
    -d "username=$1" --data-urlencode "password=$2" \
    | python3 -c 'import sys,json;print(json.load(sys.stdin).get("access_token",""))' 2>/dev/null
}

TOKEN_ADMIN=$(pedir_token "$ENTRA_USUARIO_ADMIN" "$ENTRA_PASSWORD_ADMIN")
TOKEN_CLIENTE=$(pedir_token "$ENTRA_USUARIO_CLIENTE" "$ENTRA_PASSWORD_CLIENTE")

codigo() { curl -s -m 25 -o /dev/null -w '%{http_code}' "$@"; }

{
  echo "Codigos de respuesta del sistema Pedidos360"
  echo "Capturado por scripts/capturar-codigos.sh el $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "Stage: $BASE"
  echo
  echo "== 200: peticion autorizada =="
  printf '  GET  /v1/productos            con token de Entra   -> %s\n' \
    "$(codigo "$BASE/v1/productos" -H "Authorization: Bearer $TOKEN_ADMIN")"
  printf '  POST /v1/pedidos              con token de Entra   -> %s\n' \
    "$(codigo -X POST "$BASE/v1/pedidos" -H "Authorization: Bearer $TOKEN_ADMIN" \
       -H 'Content-Type: application/json' -d '{"productoId":1,"cantidad":1}')"
  printf '  GET  /v1/public               sin token, ruta abierta -> %s\n' "$(codigo "$BASE/v1/public")"
  echo
  echo "== 401: el autorizador del API Gateway rechaza en el borde =="
  printf '  GET  /v1/productos            sin token            -> %s\n' "$(codigo "$BASE/v1/productos")"
  printf '  GET  /v1/productos            token inventado      -> %s\n' \
    "$(codigo "$BASE/v1/productos" -H 'Authorization: Bearer no-es-un-token')"
  printf '  GET  /v1/pedidos              sin token            -> %s\n' "$(codigo "$BASE/v1/pedidos")"
  echo
  echo "== 403: token valido, pero sin permiso =="
  printf '  GET  /v1/pedidos/todos        admin, rol ADMIN     -> %s\n' \
    "$(codigo "$BASE/v1/pedidos/todos" -H "Authorization: Bearer $TOKEN_ADMIN")"
  printf '  GET  /v1/pedidos/todos        cliente, solo USER   -> %s\n' \
    "$(codigo "$BASE/v1/pedidos/todos" -H "Authorization: Bearer $TOKEN_CLIENTE")"
  echo
  if [[ -n "${IP_EC2:-}" ]]; then
    echo "== 403: el API Gateway es el unico punto de entrada =="
    echo "   Llamadas directas a la instancia, con un token perfectamente valido."
    for P in "8081/api/v1/productos" "8082/api/v1/pedidos" "9000/api/v1/estado"; do
      printf '  http://IP:%-24s con token            -> %s\n' "$P" \
        "$(codigo "http://$IP_EC2:$P" -H "Authorization: Bearer $TOKEN_ADMIN")"
    done
    printf '  http://IP:%-24s (frontend)           -> %s\n' "80/" "$(codigo "http://$IP_EC2:80/")"
    echo
  fi
  echo "== CORS: solo el dominio del frontend =="
  printf '  OPTIONS /v1/productos  Origin del frontend  -> %s\n' \
    "$(curl -s -m 25 -i -X OPTIONS "$BASE/v1/productos" -H "Origin: ${BASE%/desarrollo*}" \
       -H 'Access-Control-Request-Method: GET' | grep -ci 'access-control-allow-origin' | sed 's/^1$/permitido/;s/^0$/bloqueado/')"
  printf '  OPTIONS /v1/productos  Origin desconocido   -> %s\n' \
    "$(curl -s -m 25 -i -X OPTIONS "$BASE/v1/productos" -H 'Origin: https://sitio-cualquiera.com' \
       -H 'Access-Control-Request-Method: GET' | grep -ci 'access-control-allow-origin' | sed 's/^1$/permitido/;s/^0$/bloqueado/')"
} | tee "$SALIDA"

echo
echo "Guardado en docs/evidencias/08-codigos-http.txt"
