#!/usr/bin/env bash
#
# user-data de la EC2: deja Docker listo y clona ambos repositorios.
#
# No construye ni levanta nada: eso lo hace scripts/desplegar-en-ec2.sh (o
# scripts/desplegar-front.sh) despues, por SSM. Mezclar "preparar el sistema" y
# "desplegar la app" en el mismo user-data hace que cualquier fix al deploy
# obligue a recrear la instancia (ver user_data_replace_on_change en
# computo.tf); separandolo, redeploys normales no tocan la instancia.
set -euxo pipefail

# Amazon Linux 2023 trae dnf, no yum.
dnf install -y docker git python3
systemctl enable --now docker
usermod -aG docker ec2-user

# El plugin "compose" (docker compose, sin guion) no viene en el paquete dnf.
mkdir -p /usr/local/lib/docker/cli-plugins
curl -sL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

BACK=/home/ec2-user/cloud-exp1-oidc
FRONT=/home/ec2-user/cloud-exp1-front-angular

sudo -u ec2-user git clone https://github.com/1samadhi/cloud-exp1-oidc.git "$BACK"
sudo -u ec2-user git clone https://github.com/1samadhi/cloud-exp1-front-angular.git "$FRONT"
