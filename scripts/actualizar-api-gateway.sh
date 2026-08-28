#!/usr/bin/env bash
#
# Re-apunta las integraciones del API Gateway a la IP publica actual de la EC2.
#
# En AWS Academy la instancia se detiene al cerrar la sesion del laboratorio y
# al volver a arrancar recibe una IP publica distinta, con lo que todas las
# integraciones quedan apuntando a una direccion muerta y el stage responde 503.
#
# Una Elastic IP lo evitaria, pero una EIP asociada a una instancia detenida se
# cobra por hora: a lo largo del semestre cuesta mas que ejecutar esto.
#
# Uso:  ./scripts/actualizar-api-gateway.sh <ID_DE_LA_API> <ID_DE_LA_INSTANCIA>
#
set -euo pipefail

API_ID=${1:?Falta el ID de la API. Uso: $0 <API_ID> <INSTANCE_ID>}
INSTANCIA=${2:?Falta el ID de la instancia. Uso: $0 <API_ID> <INSTANCE_ID>}
REGION=${AWS_REGION:-us-east-1}

IP=$(aws ec2 describe-instances --instance-ids "$INSTANCIA" --region "$REGION" \
      --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)

if [[ -z "$IP" || "$IP" == "None" ]]; then
  echo "La instancia $INSTANCIA no tiene IP publica. Verifica que este encendida." >&2
  exit 1
fi

echo "IP publica actual: $IP"

aws apigatewayv2 get-integrations --api-id "$API_ID" --region "$REGION" \
  --query 'Items[].[IntegrationId,IntegrationUri]' --output text |
while read -r ID URI; do
  # Reemplaza solo el host, conservando puerto y ruta de cada integracion
  NUEVA=$(echo "$URI" | sed -E "s|http://[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:|http://$IP:|")
  if [[ "$NUEVA" != "$URI" ]]; then
    aws apigatewayv2 update-integration --api-id "$API_ID" --integration-id "$ID" \
      --integration-uri "$NUEVA" --region "$REGION" >/dev/null
    echo "  $ID  ->  $NUEVA"
  fi
done

echo "Listo. El stage tiene auto-deploy activado, no hace falta implementar a mano."
