variable "aws_region" {
  description = "AWS region for the deployment."
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Prefix used for AWS resource names."
  type        = string
  default     = "team08-backend"
}

variable "vpc_cidr" {
  description = "IPv4 CIDR block for the project VPC."
  type        = string
  default     = "10.0.0.0/16"

  validation {
    condition     = can(cidrhost(var.vpc_cidr, 0))
    error_message = "vpc_cidr must be a valid IPv4 CIDR."
  }
}

variable "public_subnet_cidr" {
  description = "IPv4 CIDR block for the public subnet."
  type        = string
  default     = "10.0.1.0/24"

  validation {
    condition     = can(cidrhost(var.public_subnet_cidr, 0))
    error_message = "public_subnet_cidr must be a valid IPv4 CIDR."
  }
}

variable "instance_type" {
  description = "EC2 instance type for the Docker Compose host."
  type        = string
  default     = "t3.small"
}

variable "admin_cidr" {
  description = "Single trusted CIDR allowed to connect over SSH, for example 203.0.113.10/32."
  type        = string

  validation {
    condition     = can(cidrhost(var.admin_cidr, 0))
    error_message = "admin_cidr must be a valid IPv4 or IPv6 CIDR."
  }
}

variable "public_key_path" {
  description = "Path to the local SSH public key registered as the EC2 key pair."
  type        = string
  default     = "~/.ssh/id_ed25519.pub"
}

variable "root_volume_size" {
  description = "Root EBS volume size in GiB."
  type        = number
  default     = 20
}
