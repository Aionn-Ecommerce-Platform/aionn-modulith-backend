locals {
  name = "aionn-${var.environment}"

  common_tags = {
    Application = "aionn"
    Environment = var.environment
    ManagedBy   = "terraform"
  }

  public_subnet_cidrs  = [cidrsubnet(var.vpc_cidr, 8, 0), cidrsubnet(var.vpc_cidr, 8, 1)]
  private_subnet_cidrs = [cidrsubnet(var.vpc_cidr, 8, 10), cidrsubnet(var.vpc_cidr, 8, 11)]

  runtime_environment = {
    SPRING_PROFILES_ACTIVE        = "prod"
    SERVER_PORT                   = "8080"
    FLYWAY_ENABLED                = "true"
    POSTGRES_HOST                 = aws_db_instance.postgres.address
    POSTGRES_LOCAL_PORT           = tostring(aws_db_instance.postgres.port)
    POSTGRES_DB                   = aws_db_instance.postgres.db_name
    POSTGRES_USER                 = aws_db_instance.postgres.username
    REDIS_HOST                    = aws_elasticache_replication_group.redis.primary_endpoint_address
    REDIS_LOCAL_PORT              = "6379"
    SPRING_DATA_REDIS_SSL_ENABLED = "true"
    SECURITY_CORS_ALLOWED_ORIGINS = var.allowed_cors_origins
    CHAT_WS_ALLOWED_ORIGINS       = var.allowed_cors_origins
    CATALOG_SEARCH_PROVIDER       = "in-process"
    LOG_LEVEL                     = "INFO"
  }
}