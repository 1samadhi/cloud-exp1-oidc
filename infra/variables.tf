variable "region" {
  description = "Region del laboratorio. us-east-1 es la unica habilitada."
  type        = string
  default     = "us-east-1"
}

variable "nombre" {
  description = "Prefijo de los recursos."
  type        = string
  default     = "pedidos360"
}

variable "tipo_instancia" {
  description = <<-TXT
    t3.small y no t3.micro: una micro tiene 913 MB y quedan unos 600 MB libres,
    insuficientes para cuatro JVM mas los dos nodos de RabbitMQ.
  TXT
  type        = string
  default     = "t3.small"
}

variable "llave_ssh" {
  description = "Par de llaves del laboratorio. vockey viene creada."
  type        = string
  default     = "vockey"
}

# ---------- Base de datos ----------

variable "db_nombre" {
  description = "Nombre del schema inicial."
  type        = string
  default     = "pedidos360"
}

variable "db_usuario" {
  description = "Usuario maestro del RDS."
  type        = string
  default     = "pedidos360"
}

variable "db_password" {
  description = <<-TXT
    Contrasena maestra del RDS. No tiene valor por defecto a proposito: se pasa
    por entorno desde el .env del proyecto, nunca escrita en un archivo
    versionado.

      set -a; . ../.env; set +a
      export TF_VAR_db_password="$DB_MASTER_PASSWORD"
  TXT
  type        = string
  sensitive   = true
}

# ---------- Identidad (Microsoft Entra ID) ----------
#
# Entra vive en Azure y sobrevive al laboratorio, pero el autorizador del API
# Gateway necesita sus identificadores para validar el token.

variable "entra_tenant_id" {
  description = "Tenant de Entra ID (ENTRA_TENANT_ID en el .env)."
  type        = string
}

variable "entra_client_id" {
  description = "Client id de la aplicacion (ENTRA_CLIENT_ID en el .env)."
  type        = string
}

# ---------- Mensajeria ----------

variable "rabbitmq_usuario" {
  description = "Usuario del broker."
  type        = string
  default     = "pedidos360"
}

variable "stage" {
  description = "Nombre del stage del API Gateway."
  type        = string
  default     = "desarrollo"
}

variable "puertos_abiertos" {
  description = <<-TXT
    Puertos de la EC2 expuestos a internet. El API Gateway integra por HTTP
    publico, asi que los puertos de los microservicios tienen que ser
    alcanzables; el filtro real lo hace la cabecera X-Origen-Gateway que exige
    cada servicio.
  TXT
  type        = list(number)
  default     = [22, 80, 8081, 8082, 8083, 9000, 15672, 15673]
}
