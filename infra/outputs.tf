output "ec2_ip_publica" {
  description = "IP publica de la EC2. Usarla para desplegar via SSM: scripts/desplegar-en-ec2.sh"
  value       = aws_instance.app.public_ip
}

output "ec2_instance_id" {
  description = "ID de instancia, lo pide scripts/desplegar-en-ec2.sh y SSM en general."
  value       = aws_instance.app.id
}

output "rds_endpoint" {
  description = "Host:puerto del RDS. Va en DB_HOST/.env (sin el puerto, Spring lo separa)."
  value       = aws_db_instance.mysql.address
}

output "api_gateway_url" {
  description = "URL base del stage. Es el host que debe usar el front en produccion."
  value       = "https://${aws_apigatewayv2_api.pedidos360.id}.execute-api.${var.region}.amazonaws.com/${var.stage}"
}

output "api_gateway_id" {
  description = "Por si hace falta scripts/actualizar-api-gateway.sh tras un resize manual."
  value       = aws_apigatewayv2_api.pedidos360.id
}
