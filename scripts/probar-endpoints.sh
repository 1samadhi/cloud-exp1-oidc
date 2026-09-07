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

BASE=${1:-}
LOCAL=false
if [[ -n "$BASE" && "$BASE" == http://localhost* ]]; then
  LOCAL=true
  AUTH="http://localhost:9000"
  PROD="http://localhost:8081"
  PED="http://localhost:8082"
else
  BASE=${BASE:-${EXP1_URL:?Define EXP1_URL con la URL del stage o pasala como argumento}}
  AUTH="$BASE"; PROD="$BASE"; PED="$BASE"
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
probar 200 "GET  /api/v1/public"                     "$PROD/api/v1/public"
probar 200 "GET  /.well-known/openid-configuration"  "$AUTH/.well-known/openid-configuration"
probar 200 "GET  /.well-known/jwks.json"             "$AUTH/.well-known/jwks.json"

echo
echo "=== 2. Rutas protegidas SIN token (deben rechazar) ==="
probar 401 "GET  /api/v1/productos"                  "$PROD/api/v1/productos"
probar 401 "GET  /api/v1/pedidos"                    "$PED/api/v1/pedidos"
probar 401 "GET  /auth/userinfo"                     "$AUTH/auth/userinfo"

echo
echo "=== 3. Login en el Identity Provider propio ==="
probar 401 "POST /auth/login con clave incorrecta"   -X POST "$AUTH/auth/login" \
  -H 'Content-Type: application/json' -d '{"username":"admin","password":"incorrecta"}'
probar 200 "POST /auth/login con credenciales validas" -X POST "$AUTH/auth/login" \
  -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}'

TOKEN=$(curl -s -m 25 -X POST "$AUTH/auth/login" -H 'Content-Type: application/json' \
        -d '{"username":"admin","password":"admin123"}' \
        | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])' 2>/dev/null)

if [[ -z "$TOKEN" ]]; then
  echo "  No se pudo obtener el token: se omiten las pruebas autenticadas." >&2
  exit 1
fi
echo "  token obtenido (${#TOKEN} caracteres)"

AUTORIZACION=(-H "Authorization: Bearer $TOKEN")

echo
echo "=== 4. Rutas protegidas CON token ==="
probar 200 "GET  /auth/userinfo"                     "$AUTH/auth/userinfo" "${AUTORIZACION[@]}"
probar 200 "GET  /api/v1/productos"                  "$PROD/api/v1/productos" "${AUTORIZACION[@]}"
probar 200 "GET  /api/v1/productos/1"                "$PROD/api/v1/productos/1" "${AUTORIZACION[@]}"
probar 404 "GET  /api/v1/productos/999 (no existe)"  "$PROD/api/v1/productos/999" "${AUTORIZACION[@]}"
probar 200 "GET  /api/v1/productos/quien-soy"        "$PROD/api/v1/productos/quien-soy" "${AUTORIZACION[@]}"
probar 201 "POST /api/v1/pedidos"                    -X POST "$PED/api/v1/pedidos" \
  "${AUTORIZACION[@]}" -H 'Content-Type: application/json' -d '{"productoId":1,"cantidad":2}'
probar 400 "POST /api/v1/pedidos con producto inexistente" -X POST "$PED/api/v1/pedidos" \
  "${AUTORIZACION[@]}" -H 'Content-Type: application/json' -d '{"productoId":999,"cantidad":1}'
probar 400 "POST /api/v1/pedidos con cantidad cero"  -X POST "$PED/api/v1/pedidos" \
  "${AUTORIZACION[@]}" -H 'Content-Type: application/json' -d '{"productoId":1,"cantidad":0}'
probar 200 "GET  /api/v1/pedidos"                    "$PED/api/v1/pedidos" "${AUTORIZACION[@]}"

echo
echo "=== 5. Token invalido ==="
probar 401 "GET  /api/v1/productos con token basura" "$PROD/api/v1/productos" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOi8vZmFsc28ifQ.x"

echo
echo "======================================"
printf '  correctas: %s   fallidas: %s\n' "$ok" "$fallos"
echo "======================================"
[[ "$fallos" -eq 0 ]]
