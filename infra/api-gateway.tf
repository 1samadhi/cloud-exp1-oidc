# API Gateway de pedidos360.
#
# Espejo de scripts/crear-api-gateway.sh, pero declarativo: aqui las rutas
# viven en una sola lista (locals.rutas) para no repetir el mismo bloque de
# recursos 10 veces. Si cambia una ruta, cambia una linea en la lista.
#
# Las integraciones apuntan a aws_instance.app.public_ip: si la EC2 se detiene
# y se vuelve a encender (cambia la IP publica), un "terraform apply" comun
# detecta la deriva y actualiza las 10 integraciones solo. No hace falta
# correr actualizar-api-gateway.sh a mano salvo que la IP haya cambiado por
# fuera de Terraform (por ejemplo, un resize manual como el del 14/10/2026).

locals {
  ec2 = aws_instance.app.public_ip

  # clave = metodo + ruta (formato que exige apigatewayv2 route-key)
  # uri    = destino en la EC2
  # protegida = si exige el JWT de Entra
  rutas = [
    { clave = "GET /v1/public", uri = "http://${local.ec2}:8081/api/v1/public", protegida = false },
    { clave = "ANY /v1/productos", uri = "http://${local.ec2}:8081/api/v1/productos", protegida = true },
    { clave = "ANY /v1/productos/{proxy+}", uri = "http://${local.ec2}:8081/api/v1/productos/{proxy}", protegida = true },
    { clave = "ANY /v1/carrito", uri = "http://${local.ec2}:8082/api/v1/carrito", protegida = true },
    { clave = "ANY /v1/carrito/{proxy+}", uri = "http://${local.ec2}:8082/api/v1/carrito/{proxy}", protegida = true },
    { clave = "ANY /v1/ordenes", uri = "http://${local.ec2}:8083/api/v1/ordenes", protegida = true },
    { clave = "ANY /v1/ordenes/{proxy+}", uri = "http://${local.ec2}:8083/api/v1/ordenes/{proxy}", protegida = true },
    { clave = "ANY /v1/admin/{proxy+}", uri = "http://${local.ec2}:8084/v1/admin/{proxy}", protegida = true },
    { clave = "ANY /v1/facturas/{proxy+}", uri = "http://${local.ec2}:8086/api/v1/facturas/{proxy}", protegida = true },
    { clave = "POST /auth/registro", uri = "http://${local.ec2}:9000/auth/registro", protegida = false },
    { clave = "GET /", uri = "http://${local.ec2}:80/", protegida = false },
    { clave = "ANY /{proxy+}", uri = "http://${local.ec2}:80/{proxy}", protegida = false },
  ]
}

resource "aws_apigatewayv2_api" "pedidos360" {
  name          = var.nombre
  protocol_type = "HTTP"
}

# Un solo emisor por autorizador HTTP API: ver docs/03-api-gateway.md para la
# limitacion con Cognito (usuarios externos quedan sin acceso por este borde).
resource "aws_apigatewayv2_authorizer" "entra_jwt" {
  api_id           = aws_apigatewayv2_api.pedidos360.id
  name             = "entra-jwt"
  authorizer_type  = "JWT"
  identity_sources = ["$request.header.Authorization"]

  jwt_configuration {
    issuer   = "https://login.microsoftonline.com/${var.entra_tenant_id}/v2.0"
    audience = ["api://${var.entra_client_id}", var.entra_client_id]
  }
}

resource "aws_apigatewayv2_integration" "destino" {
  for_each = { for r in local.rutas : r.clave => r }

  api_id                 = aws_apigatewayv2_api.pedidos360.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = each.value.uri
  payload_format_version = "1.0"

  # Sin esto, FiltroOrigenGateway rechaza con 403 cualquier peticion que pase
  # por el gateway: el filtro corre antes que la validacion JWT y exige esta
  # cabecera exacta. Se olvido una vez en el front (ver CHANGELOG V10.6.0); al
  # estar en el recurso compartido for_each ya no puede faltar en una ruta sola.
  request_parameters = {
    "overwrite:header.X-Origen-Gateway" = var.secreto_gateway
  }
}

resource "aws_apigatewayv2_route" "ruta" {
  for_each = { for r in local.rutas : r.clave => r }

  api_id    = aws_apigatewayv2_api.pedidos360.id
  route_key = each.key
  target    = "integrations/${aws_apigatewayv2_integration.destino[each.key].id}"

  authorization_type = each.value.protegida ? "JWT" : "NONE"
  authorizer_id      = each.value.protegida ? aws_apigatewayv2_authorizer.entra_jwt.id : null
}

resource "aws_apigatewayv2_stage" "stage" {
  api_id      = aws_apigatewayv2_api.pedidos360.id
  name        = var.stage
  auto_deploy = true
}
