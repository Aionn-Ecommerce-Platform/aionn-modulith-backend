variable "aws_region" {
  description = "AWS region used by all resources."
  type        = string
  default     = "ap-southeast-1"
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "staging"

  validation {
    condition     = contains(["development", "staging"], var.environment)
    error_message = "environment must be development or staging."
  }
}

variable "vpc_cidr" {
  description = "CIDR range for the application VPC."
  type        = string
  default     = "10.20.0.0/16"

  validation {
    condition     = can(cidrsubnet(var.vpc_cidr, 8, 11))
    error_message = "vpc_cidr must provide enough address space for the required /24 subnets."
  }
}

variable "container_image" {
  description = "Immutable ECR image reference, preferably pinned by sha256 digest."
  type        = string
}

variable "certificate_arn" {
  description = "ACM certificate ARN. Leave null for an HTTP-only staging deployment."
  type        = string
  default     = null
  nullable    = true
}

variable "allowed_cors_origins" {
  description = "Comma-separated browser origins accepted by the backend."
  type        = string
}

variable "ec2_instance_type" {
  description = "Free Plan eligible EC2 instance type used by the development backend."
  type        = string
  default     = "t3.small"

  validation {
    condition     = contains(["t3.micro", "t3.small", "c7i-flex.large", "m7i-flex.large"], var.ec2_instance_type)
    error_message = "Use an x86_64 EC2 instance type listed by AWS for the new Free Plan."
  }
}

variable "ec2_root_volume_size" {
  description = "EC2 gp3 root volume size in GiB."
  type        = number
  default     = 20
}

variable "db_instance_class" {
  description = "Free Plan eligible RDS PostgreSQL instance class."
  type        = string
  default     = "db.t4g.micro"

  validation {
    condition     = contains(["db.t3.micro", "db.t4g.micro"], var.db_instance_class)
    error_message = "RDS Free Plan supports db.t3.micro or db.t4g.micro for PostgreSQL."
  }
}

variable "db_allocated_storage" {
  description = "Initial RDS gp3 storage in GiB."
  type        = number
  default     = 20
}

variable "db_backup_retention_days" {
  description = "RDS point-in-time recovery retention period."
  type        = number
  default     = 7
}

variable "redis_node_type" {
  description = "ElastiCache Redis node type."
  type        = string
  default     = "cache.t4g.micro"
}

variable "log_retention_days" {
  description = "CloudWatch application log retention period."
  type        = number
  default     = 30
}

variable "alarm_email" {
  description = "Optional email subscribed to operational alarms. Confirmation is required."
  type        = string
  default     = null
  nullable    = true
}

variable "secret_values" {
  description = "Initial runtime secrets. Supply through a local ignored tfvars file only."
  type        = map(string)
  sensitive   = true
  default     = {}
}
