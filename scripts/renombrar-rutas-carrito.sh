#!/usr/bin/env bash
#
# Renombra en el API Gateway las rutas de /v1/pedidos a /v1/carrito.
#
# El microservicio paso de llamarse ms-pedidos a ms-carrito y sus endpoints
# cambiaron, asi que las rutas y las integraciones de la API quedaron apuntando
# a un path que ya no existe: el stage responderia 404 desde el propio servicio.
#
# Es idempotente: si una ruta ya quedo renombrada, la deja como esta.
#
# Uso:  ./scripts/renombrar-rutas-carrito.sh <ID_DE_LA_API> [--aplicar]
#
# Sin --aplicar solo muestra lo que haria.
#
set -euo pipefail

API_ID=${1:?Falta el ID de la API. Uso: $0 <API_ID> [--aplicar]}
APLICAR=${2:-}
REGION=${AWS_REGION:-us-east-1}

if [[ "$APLICAR" != "--aplicar" && -n "$APLICAR" ]]; then
  echo "Segundo argumento desconocido: $APLICAR (solo se acepta --aplicar)" >&2
  exit 2
fi

if [[ "$APLICAR" == "--aplicar" ]]; then
  echo "Modo aplicar: se van a modificar rutas e integraciones de $API_ID"
else
  echo "Modo simulacion: no se cambia nada. Agrega --aplicar para ejecutarlo."
fi

cambios=0

# ---------- Rutas ----------
# RouteKey tiene la forma "<METODO> <ruta>", por ejemplo "GET /v1/pedidos".
while read -r ID CLAVE_METODO CLAVE_RUTA; do
  [[ -z "${ID:-}" ]] && continue
  case "$CLAVE_RUTA" in
    /v1/pedidos|/v1/pedidos/*)
      NUEVA_RUTA=${CLAVE_RUTA/\/v1\/pedidos/\/v1\/carrito}
      echo "  ruta  $ID  $CLAVE_METODO $CLAVE_RUTA  ->  $CLAVE_METODO $NUEVA_RUTA"
      if [[ "$APLICAR" == "--aplicar" ]]; then
        aws apigatewayv2 update-route --api-id "$API_ID" --route-id "$ID" \
          --route-key "$CLAVE_METODO $NUEVA_RUTA" --region "$REGION" >/dev/null
      fi
      cambios=$((cambios + 1))
      ;;
  esac
done < <(aws apigatewayv2 get-routes --api-id "$API_ID" --region "$REGION" \
           --query 'Items[].[RouteId,RouteKey]' --output text)

# ---------- Integraciones ----------
# El backend expone /api/v1/carrito, asi que el path de la integracion tambien
# tiene que cambiar; renombrar solo la ruta dejaria un 404 del microservicio.
while read -r ID URI; do
  [[ -z "${ID:-}" ]] && continue
  if [[ "$URI" == *"/api/v1/pedidos"* ]]; then
    NUEVA_URI=${URI//\/api\/v1\/pedidos//api/v1/carrito}
    echo "  integ $ID  $URI  ->  $NUEVA_URI"
    if [[ "$APLICAR" == "--aplicar" ]]; then
      aws apigatewayv2 update-integration --api-id "$API_ID" --integration-id "$ID" \
        --integration-uri "$NUEVA_URI" --region "$REGION" >/dev/null
    fi
    cambios=$((cambios + 1))
  fi
done < <(aws apigatewayv2 get-integrations --api-id "$API_ID" --region "$REGION" \
           --query 'Items[].[IntegrationId,IntegrationUri]' --output text)

if [[ "$cambios" -eq 0 ]]; then
  echo "No queda nada que renombrar: la API ya usa /v1/carrito."
elif [[ "$APLICAR" == "--aplicar" ]]; then
  echo "Listo, $cambios cambios. El stage tiene auto-deploy, no hace falta implementar a mano."
else
  echo "$cambios cambios pendientes. Repite el comando con --aplicar."
fi
