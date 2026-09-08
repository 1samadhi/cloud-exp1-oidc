#!/usr/bin/env bash
#
# Prueba de humo de todos los endpoints, con los codigos HTTP esperados.
#
# Uso:
#   ./scripts/probar-endpoints.sh                      # contra el API Gateway
#   ./scripts/probar-endpoints.sh http://localhost     # contra el stack local
#
# En local los servicios escuchan en puertos distintos, asi que se pasa la base
# sin puerto y el script agrega el que corresponde a cada servicio.
#
set -uo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

BASE=${1:-}
LOCAL=false
if [[ -n "$BASE" && "$BASE" == http://localhost* ]]; then
  LOCAL=true
  AUTH="http://localhost:9000"
  PROD="http://localhost:8081"
  PED="http://localhost:8082"
  # Llamando directo al servicio hay que usar su path real
  PREFIJO="/api/v1"
else
  BASE=${BASE:-${EXP1_URL:?Define EXP1_URL con la URL del stage o pasala como argumento}}
  AUTH="$BASE"; PROD="$BASE"; PED="$BASE"
  # El gateway expone rutas limpias y traduce al path interno del microservicio
  PREFIJO="/v1"
fi

ok=0; fallos=0

probar() {
  local esperado=$1 descripcion=$2; shift 2
  local codigo
  codigo=$(curl -s -m 25 -o /dev/null -w '%{http_code}' "$@")
  if [[ "$codigo" == "$esperado" ]]; then
    printf '  \033[32mOK\033[0m   %-3s  %s\n' "$codigo" "$descripcion"
    ok=$((ok+1))
  else
    printf '  \033[31mFALLA\033[0m %-3s (esperaba %s)  %s\n' "$codigo" "$esperado" "$descripcion"
    fallos=$((fallos+1))
  fi
}

echo "=== 1. Rutas publicas (no exigen token) ==="
probar 200 "GET  ${PREFIJO}/public"                     "$PROD${PREFIJO}/public"
probar 200 "GET  /.well-known/openid-configuration"  "$AUTH/.well-known/openid-configuration"
probar 200 "GET  /.well-known/jwks.json"             "$AUTH/.well-known/jwks.json"

echo
echo "=== 2. Rutas protegidas SIN token (deben rechazar) ==="
probar 401 "GET  ${PREFIJO}/productos"                  "$PROD${PREFIJO}/productos"
probar 401 "GET  ${PREFIJO}/pedidos"                    "$PED${PREFIJO}/pedidos"
# El IdP propio quedo retirado en V8.1.0: su login ya no se publica.
probar 404 "POST /auth/login (retirado del gateway)"   -X POST "$AUTH/auth/login" \
  -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}' 

echo
echo "=== 3. Token de Microsoft Entra ID ==="
# El frontend usa Authorization Code con PKCE, que necesita navegador. Para una
# prueba automatizada se usa el flujo de contrasenia contra el mismo tenant: el
# token resultante es equivalente y lo emite el mismo Entra ID.
if [[ -f "$RAIZ/.env" ]]; then
  set -a; source "$RAIZ/.env" >/dev/null 2>&1; set +a
fi

if [[ -z "${ENTRA_TENANT_ID:-}" || -z "${ENTRA_CLIENT_ID:-}" || -z "${ENTRA_PASSWORD_ADMIN:-}" ]]; then
  echo "  Faltan ENTRA_TENANT_ID, ENTRA_CLIENT_ID o ENTRA_PASSWORD_ADMIN en .env" >&2
  exit 1
fi

probar 400 "Token con credenciales incorrectas" \
  -X POST "https://login.microsoftonline.com/$ENTRA_TENANT_ID/oauth2/v2.0/token" \
  -d "client_id=$ENTRA_CLIENT_ID" -d "grant_type=password" \
  -d "scope=api://$ENTRA_CLIENT_ID/productos.leer" \
  -d "username=$ENTRA_USUARIO_ADMIN" -d "password=incorrecta"

TOKEN=$(curl -s -m 25 -X POST "https://login.microsoftonline.com/$ENTRA_TENANT_ID/oauth2/v2.0/token" \
        -d "client_id=$ENTRA_CLIENT_ID" -d "grant_type=password" \
        -d "scope=api://$ENTRA_CLIENT_ID/productos.leer api://$ENTRA_CLIENT_ID/pedidos.escribir" \
        -d "username=$ENTRA_USUARIO_ADMIN" --data-urlencode "password=$ENTRA_PASSWORD_ADMIN" \
        | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])' 2>/dev/null)

if [[ -z "$TOKEN" ]]; then
  echo "  No se pudo obtener el token de Entra: se omiten las pruebas autenticadas." >&2
  exit 1
fi
echo "  token de Entra obtenido (${#TOKEN} caracteres)"

AUTORIZACION=(-H "Authorization: Bearer $TOKEN")

echo
echo "=== 4. Rutas protegidas CON token ==="
probar 200 "GET  ${PREFIJO}/productos"                  "$PROD${PREFIJO}/productos" "${AUTORIZACION[@]}"
probar 200 "GET  ${PREFIJO}/productos/1"                "$PROD${PREFIJO}/productos/1" "${AUTORIZACION[@]}"
probar 404 "GET  ${PREFIJO}/productos/999 (no existe)"  "$PROD${PREFIJO}/productos/999" "${AUTORIZACION[@]}"
probar 200 "GET  ${PREFIJO}/productos/quien-soy"        "$PROD${PREFIJO}/productos/quien-soy" "${AUTORIZACION[@]}"
probar 201 "POST ${PREFIJO}/pedidos"                    -X POST "$PED${PREFIJO}/pedidos" \
  "${AUTORIZACION[@]}" -H 'Content-Type: application/json' -d '{"productoId":1,"cantidad":2}'
probar 400 "POST ${PREFIJO}/pedidos con producto inexistente" -X POST "$PED${PREFIJO}/pedidos" \
  "${AUTORIZACION[@]}" -H 'Content-Type: application/json' -d '{"productoId":999,"cantidad":1}'
probar 400 "POST ${PREFIJO}/pedidos con cantidad cero"  -X POST "$PED${PREFIJO}/pedidos" \
  "${AUTORIZACION[@]}" -H 'Content-Type: application/json' -d '{"productoId":1,"cantidad":0}'
probar 200 "GET  ${PREFIJO}/pedidos"                    "$PED${PREFIJO}/pedidos" "${AUTORIZACION[@]}"

echo
echo "=== 5. Registro de usuarios en el tenant (Microsoft Graph) ==="
probar 400 "POST /auth/registro con datos invalidos"  -X POST "$AUTH/auth/registro" \
  -H 'Content-Type: application/json' -d '{"nombre":"","usuario":"ab","password":"corta"}'

echo "=== 6. Token invalido ==="
probar 401 "GET  ${PREFIJO}/productos con token basura" "$PROD${PREFIJO}/productos" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOi8vZmFsc28ifQ.x"

echo
echo "======================================"
printf '  correctas: %s   fallidas: %s\n' "$ok" "$fallos"
echo "======================================"
[[ "$fallos" -eq 0 ]]
