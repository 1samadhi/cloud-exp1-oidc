# Infraestructura de pedidos360 (Terraform)

Para un compañero que quiera levantar su propia copia en su Learner Lab de
AWS Academy.

## Qué crea

VPC por defecto del lab (no crea una nueva), 2 Security Groups, RDS MySQL,
una EC2 con Docker instalado y los dos repos clonados, y el API Gateway
completo (10 rutas + autorizador JWT de Entra ID). Lo que **no** crea: los
roles IAM (el lab no lo permite, se usa `LabInstanceProfile`) ni las imágenes
Docker (eso corre después, por SSM).

## Pasos

```bash
# 1. Pegar las credenciales del lab en ~/.aws/credentials
#    (AWS Academy > tu módulo > AWS Details > copiar al portapapeles)

# 2. Si ya corriste esto antes en OTRO lab, borra el estado viejo:
rm -f terraform.tfstate*

# 3. Necesitas un .env propio en la raíz del proyecto (cloud-exp1-oidc/.env)
#    con al menos: ENTRA_TENANT_ID, ENTRA_CLIENT_ID, DB_PASSWORD,
#    SEGURIDAD_SECRETO_GATEWAY (inventa un secreto random para este último).
cd infra
set -a; . ../.env; set +a
export TF_VAR_db_password="$DB_PASSWORD"
export TF_VAR_secreto_gateway="$SEGURIDAD_SECRETO_GATEWAY"

# 4. Crear todo (tarda ~8-10 min, el RDS es lo que más demora)
terraform init
terraform apply

# 5. Desplegar el código (clona, compila, levanta los 9 contenedores)
terraform output -raw ec2_instance_id
../scripts/desplegar-en-ec2.sh <el_instance_id_de_arriba>

# 6. Probar
curl "$(terraform output -raw api_gateway_url)/v1/public"
```

## Cosas que YA nos pasaron a nosotros (para que no te pase a ti)

- **`t3.small` (2 GB) no alcanza.** Con 7 microservicios + 2 nodos RabbitMQ la
  instancia queda inutilizable (SSM deja de responder). El default de este
  Terraform ya es `t3.medium` (4 GB); no lo bajes salvo que saques algún
  microservicio.
- **Si reinicias (stop/start) la EC2 por fuera de Terraform**, su IP pública
  cambia. Un `terraform apply` de nuevo lo detecta solo y actualiza las 10
  integraciones del gateway; no hace falta tocar nada a mano.
- **El front responde 403 aunque el login funcione**: revisa que la
  integración tenga `overwrite:header.X-Origen-Gateway` con el mismo valor
  que `SEGURIDAD_SECRETO_GATEWAY` del `.env` de la EC2. Este Terraform ya lo
  aplica a las 10 rutas por igual (antes se nos olvidó solo en 2 y costó un
  rato encontrarlo).
- **Para ver el front**, entra por la URL del API Gateway
  (`terraform output api_gateway_url`), no por la IP directa de la EC2: nginx
  rechaza con 403 lo que no traiga esa cabecera.

## Al terminar

`terraform destroy` libera el RDS y la EC2 (ambos cobran por hora mientras
existan, usen o no el lab).
