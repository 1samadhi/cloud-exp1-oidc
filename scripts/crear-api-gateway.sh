#!/usr/bin/env bash
#
# Crea desde cero el API Gateway de pedidos360: rutas, integraciones hacia la
# EC2, stage con auto-deploy y el autorizador JWT de Entra ID.
#
# Existe porque el laboratorio de AWS Academy se borra entero al terminar: la
# cuenta cambia y no queda ninguna API. Rehacer esto a mano son 30 clics; aqui
# es un comando.
#
# Uso:  ./scripts/crear-api-gateway.sh <IP_PUBLICA_EC2>
#
# Requiere en el entorno (vienen del .env):
#   ENTRA_TENANT_ID, ENTRA_CLIENT_ID
#
set -euo pipefail

IP=${1:?Falta la IP publica de la EC2. Uso: $0 <IP_PUBLICA_EC2>}
REGION=${AWS_REGION:-us-east-1}
STAGE=${STAGE:-desarrollo}
: "${ENTRA_TENANT_ID:?Define ENTRA_TENANT_ID (export set -a; . ./.env)}"
: "${ENTRA_CLIENT_ID:?Define ENTRA_CLIENT_ID (export set -a; . ./.env)}"

echo "Creando la API para la EC2 $IP"

API_ID=$(aws apigatewayv2 create-api --name pedidos360 --protocol-type HTTP \
  --region "$REGION" --query ApiId --output text)
echo "  API: $API_ID"

# El autorizador valida los access token de Entra ID. Se declaran las dos
# audiencias porque el claim aud llega como App ID URI o como client id pelado
# segun como el frontend pida el token.
AUTH_ID=$(aws apigatewayv2 create-authorizer --api-id "$API_ID" --name entra-jwt \
  --authorizer-type JWT --identity-source '$request.header.Authorization' \
  --jwt-configuration "Issuer=https://login.microsoftonline.com/${ENTRA_TENANT_ID}/v2.0,Audience=api://${ENTRA_CLIENT_ID},${ENTRA_CLIENT_ID}" \
  --region "$REGION" --query AuthorizerId --output text)
echo "  autorizador: $AUTH_ID"

# ruta|destino en la EC2|protegida
RUTAS=(
  "GET /v1/public|http://$IP:8081/api/v1/public|no"
  "ANY /v1/productos|http://$IP:8081/api/v1/productos|si"
  "ANY /v1/productos/{proxy+}|http://$IP:8081/api/v1/productos/{proxy}|si"
  "ANY /v1/carrito|http://$IP:8082/api/v1/carrito|si"
  "ANY /v1/carrito/{proxy+}|http://$IP:8082/api/v1/carrito/{proxy}|si"
  "ANY /v1/ordenes|http://$IP:8083/api/v1/ordenes|si"
  "ANY /v1/ordenes/{proxy+}|http://$IP:8083/api/v1/ordenes/{proxy}|si"
  "POST /auth/registro|http://$IP:9000/auth/registro|no"
  "GET /|http://$IP:80/|no"
  "ANY /{proxy+}|http://$IP:80/{proxy}|no"
)

for ENTRADA in "${RUTAS[@]}"; do
  IFS='|' read -r CLAVE URI PROTEGIDA <<< "$ENTRADA"
  INT_ID=$(aws apigatewayv2 create-integration --api-id "$API_ID" \
    --integration-type HTTP_PROXY --integration-method ANY \
    --integration-uri "$URI" --payload-format-version 1.0 \
    --region "$REGION" --query IntegrationId --output text)
  if [[ "$PROTEGIDA" == "si" ]]; then
    aws apigatewayv2 create-route --api-id "$API_ID" --route-key "$CLAVE" \
      --target "integrations/$INT_ID" --authorization-type JWT --authorizer-id "$AUTH_ID" \
      --region "$REGION" >/dev/null
  else
    aws apigatewayv2 create-route --api-id "$API_ID" --route-key "$CLAVE" \
      --target "integrations/$INT_ID" --region "$REGION" >/dev/null
  fi
  printf '  %-30s -> %s%s\n' "$CLAVE" "$URI" "$([[ $PROTEGIDA == si ]] && echo '  [JWT]')"
done

# El stage con auto-deploy evita tener que implementar a mano en cada cambio.
aws apigatewayv2 create-stage --api-id "$API_ID" --stage-name "$STAGE" \
  --auto-deploy --region "$REGION" >/dev/null
echo "  stage: $STAGE (auto-deploy)"

echo
echo "URL del stage:"
echo "  https://${API_ID}.execute-api.${REGION}.amazonaws.com/${STAGE}"
echo
echo "Anota el API_ID: hace falta para actualizar-api-gateway.sh cuando cambie la IP."
