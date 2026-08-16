resource "aws_secretsmanager_secret" "runtime" {
  name                    = "${local.name}/backend/runtime"
  recovery_window_in_days = var.environment == "production" ? 30 : 0
}

resource "aws_secretsmanager_secret_version" "runtime" {
  secret_id = aws_secretsmanager_secret.runtime.id
  secret_string = jsonencode(merge(var.secret_values, {
    POSTGRES_PASSWORD = random_password.database.result
    REDIS_PASSWORD    = random_password.redis.result
  }))
}