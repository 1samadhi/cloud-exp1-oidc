#!/usr/bin/env bash
#
# Despliega el stack en la EC2 mediante SSM, sin necesidad de SSH.
#
# Uso:  ./scripts/desplegar-en-ec2.sh <ID_DE_LA_INSTANCIA>
#
set -euo pipefail

INSTANCIA=${1:?Falta el ID de la instancia. Uso: $0 <INSTANCE_ID>}
REGION=${AWS_REGION:-us-east-1}
RUTA=/home/ec2-user/cloud-exp1-oidc

CMD=$(aws ssm send-command --region "$REGION" --instance-ids "$INSTANCIA" \
  --document-name AWS-RunShellScript --timeout-seconds 600 \
  --parameters "commands=[
    'set -eux',
    'export HOME=/root',
    'git config --global --add safe.directory $RUTA',
    'cd $RUTA',
    'git pull --ff-only origin main',
    'docker compose --env-file .env up --build -d',
    'docker compose ps'
  ]" --query Command.CommandId --output text)

echo "CommandId: $CMD"
echo "Siguiendo el avance..."

while true; do
  ESTADO=$(aws ssm get-command-invocation --command-id "$CMD" --instance-id "$INSTANCIA" \
            --region "$REGION" --query Status --output text 2>/dev/null || echo Pending)
  [[ "$ESTADO" == "Success" || "$ESTADO" == "Failed" || "$ESTADO" == "TimedOut" ]] && break
  sleep 10
done

echo "Estado: $ESTADO"
aws ssm get-command-invocation --command-id "$CMD" --instance-id "$INSTANCIA" \
  --region "$REGION" --query StandardOutputContent --output text | tail -20
