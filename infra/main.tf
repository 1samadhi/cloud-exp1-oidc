# Infraestructura de pedidos360 en el laboratorio de AWS Academy.
#
# Existe porque el Learner Lab se borra completo al terminar: cambia el numero
# de cuenta y no sobrevive ni la API ni la EC2 ni el RDS ni el pool de Cognito.
# Recrearlo a mano son decenas de pasos y es facil olvidar uno (el caso tipico:
# el security group del RDS apuntando a un grupo que ya no existe).
#
# Uso en un laboratorio nuevo:
#   1. Pegar las credenciales en ~/.aws/credentials (AWS Academy > AWS Details)
#   2. rm -f terraform.tfstate*        (el estado anterior apunta a otra cuenta)
#   3. set -a; . ../.env; set +a
#   4. export TF_VAR_db_password="$DB_PASSWORD"
#      export TF_VAR_secreto_gateway="$SEGURIDAD_SECRETO_GATEWAY"
#   5. terraform init && terraform apply
#   6. Con los outputs (ec2_instance_id, ec2_ip_publica):
#        ../scripts/desplegar-en-ec2.sh <ec2_instance_id>
#      Ese script clona/compila el front y levanta el stack del back por SSM;
#      el user_data de este Terraform solo deja Docker instalado y los
#      repositorios clonados, para no tener que recrear la instancia cada vez
#      que cambia el codigo de la app (ver user_data_replace_on_change).
#   7. Probar: curl "$(terraform output -raw api_gateway_url)/v1/public"
#
# Si la instancia se detiene y se vuelve a encender por fuera de Terraform
# (por ejemplo, un resize manual de tipo de instancia), su IP publica cambia
# sin que el estado se entere hasta el proximo apply. "terraform apply" de
# nuevo detecta la deriva via refresh y actualiza las 10 integraciones del
# gateway solo, sin tocar nada mas.
#
# Lo que NO esta aqui: los roles IAM, porque el laboratorio no permite crearlos
# (se referencian los que ya vienen), y el build de las imagenes Docker, que
# va por SSM despues del apply (ver paso 6).

terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Proyecto = "pedidos360"
      Ramo     = "DSY1107"
      Gestion  = "terraform"
    }
  }
}

# ---------- Red: se usa la VPC por defecto del laboratorio ----------

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# ---------- Rol existente del laboratorio ----------
#
# El Learner Lab prohibe crear roles. LabInstanceProfile ya viene creado y es el
# que permite operar la instancia por SSM sin tener la llave privada de vockey.

data "aws_iam_instance_profile" "lab" {
  name = "LabInstanceProfile"
}

# ---------- AMI ----------

data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}
