output "ecr_repository_url" {
  description = "ECR repository used by the backend release pipeline."
  value       = aws_ecr_repository.backend.repository_url
}

output "load_balancer_url" {
  description = "Public application endpoint."
  value       = "${var.certificate_arn == null ? "http" : "https"}://${aws_lb.application.dns_name}"
}

output "ec2_instance_id" {
  description = "Backend EC2 instance managed through Systems Manager."
  value       = aws_instance.application.id
}

output "ec2_public_ip" {
  description = "Development instance public IP used for outbound provider calls."
  value       = aws_instance.application.public_ip
}

output "runtime_secret_arn" {
  value = aws_secretsmanager_secret.runtime.arn
}

output "database_endpoint" {
  value     = aws_db_instance.postgres.endpoint
  sensitive = true
}

output "redis_endpoint" {
  value     = aws_elasticache_replication_group.redis.primary_endpoint_address
  sensitive = true
}