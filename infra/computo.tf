# Computo y base de datos.

# ---------- Security groups ----------

resource "aws_security_group" "ec2" {
  name        = "${var.nombre}-ec2"
  description = "EC2 pedidos360: frontend, microservicios y RabbitMQ"
  vpc_id      = data.aws_vpc.default.id

  dynamic "ingress" {
    for_each = var.puertos_abiertos
    content {
      description = "tcp/${ingress.value}"
      from_port   = ingress.value
      to_port     = ingress.value
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
    }
  }

  egress {
    description = "Salida libre: la instancia baja imagenes y dependencias"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.nombre}-ec2" }
}

resource "aws_security_group" "rds" {
  name        = "${var.nombre}-rds"
  description = "RDS pedidos360: 3306 solo desde la EC2"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description     = "MySQL unicamente desde el grupo de la EC2"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2.id]
  }

  tags = { Name = "${var.nombre}-rds" }
}

# ---------- Base de datos ----------

resource "aws_db_subnet_group" "principal" {
  name        = "${var.nombre}-subnets"
  description = "Subredes por defecto del laboratorio"
  subnet_ids  = data.aws_subnets.default.ids
}

resource "aws_db_instance" "mysql" {
  identifier     = "${var.nombre}-db"
  engine         = "mysql"
  engine_version = "8.0"
  instance_class = "db.t3.micro"

  allocated_storage = 20
  storage_type      = "gp2"

  db_name  = var.db_nombre
  username = var.db_usuario
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.principal.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  # Sin IP publica: solo se alcanza desde la EC2. Es lo que convierte al
  # security group en la unica puerta.
  publicly_accessible = false

  # El laboratorio se borra entero, asi que respaldos y snapshot final solo
  # agregan tiempo al destroy.
  backup_retention_period = 0
  skip_final_snapshot     = true
  apply_immediately       = true
  multi_az                = false

  tags = { Name = "${var.nombre}-db" }
}

# ---------- EC2 ----------

resource "aws_instance" "app" {
  ami                         = data.aws_ssm_parameter.al2023.value
  instance_type               = var.tipo_instancia
  key_name                    = var.llave_ssh
  subnet_id                   = data.aws_subnets.default.ids[0]
  vpc_security_group_ids      = [aws_security_group.ec2.id]
  iam_instance_profile        = data.aws_iam_instance_profile.lab.name
  associate_public_ip_address = true

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
  }

  user_data = file("${path.module}/user-data.sh")

  # Cambiar el user_data obliga a recrear la instancia: aplicar el script sobre
  # una maquina ya arrancada no hace nada.
  user_data_replace_on_change = true

  tags = { Name = var.nombre }
}
