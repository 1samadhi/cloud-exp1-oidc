#!/usr/bin/env bash
#
# Despliega el sistema completo en la EC2 mediante SSM, sin necesidad de SSH.
#
# El proyecto vive en dos repositorios, uno por componente, tal como pide el
# enunciado. Este script clona o actualiza ambos: construye la imagen del
# frontend en su repositorio y levanta el stack desde el del backend.
#
# Uso:  ./scripts/desplegar-en-ec2.sh <ID_DE_LA_INSTANCIA>
#
set -euo pipefail

INSTANCIA=${1:?Falta el ID de la instancia. Uso: $0 <INSTANCE_ID>}
REGION=${AWS_REGION:-us-east-1}
BACK=/home/ec2-user/cloud-exp1-oidc
FRONT=/home/ec2-user/cloud-exp1-front-angular
REPO_FRONT=https://github.com/1samadhi/cloud-exp1-front-angular.git

PARAMS=$(python3 - "$BACK" "$FRONT" "$REPO_FRONT" <<'PY'
import json, sys
back, front, repo = sys.argv[1], sys.argv[2], sys.argv[3]
print(json.dumps({"commands": [
    "set -eux",
    "export HOME=/root",
    f"git config --global --add safe.directory {back}",
    f"git config --global --add safe.directory {front}",
    # Frontend: se clona la primera vez y se actualiza en las siguientes
    f"if [ -d {front}/.git ]; then cd {front} && git pull --ff-only origin main; "
    f"else git clone {repo} {front}; fi",
    f"cd {front} && docker build --build-arg BASE_HREF=/desarrollo/ -t exp1/front-angular:6.0.0 .",
    # Backend: microservicios y orquestacion
    f"cd {back} && git pull --ff-only origin main",
    f"cd {back} && docker compose --env-file .env up --build -d",
    "sleep 30",
    f"cd {back} && docker compose ps",
]}))
PY
)

ARCHIVO=$(mktemp)
echo "$PARAMS" > "$ARCHIVO"

CMD=$(aws ssm send-command --region "$REGION" --instance-ids "$INSTANCIA" \
  --document-name AWS-RunShellScript --timeout-seconds 900 \
  --parameters "file://$ARCHIVO" --query Command.CommandId --output text)

rm -f "$ARCHIVO"
echo "CommandId: $CMD"
echo "Siguiendo el avance..."

while true; do
  ESTADO=$(aws ssm get-command-invocation --command-id "$CMD" --instance-id "$INSTANCIA" \
            --region "$REGION" --query Status --output text 2>/dev/null || echo Pending)
  [[ "$ESTADO" == "Success" || "$ESTADO" == "Failed" || "$ESTADO" == "TimedOut" ]] && break
  sleep 15
done

echo "Estado: $ESTADO"
aws ssm get-command-invocation --command-id "$CMD" --instance-id "$INSTANCIA" \
  --region "$REGION" --query StandardOutputContent --output text | tail -20
