variable "subscription_id" {
  description = "Azure subscription ID; fornecido via TF_VAR_subscription_id pelo executor."
  type        = string
  validation {
    condition     = can(regex("^[0-9a-fA-F-]{36}$", var.subscription_id))
    error_message = "Informe uma subscription_id valida."
  }
}

variable "project" {
  description = "Prefixo curto de recursos e tags."
  type        = string
  default     = "clyvo"
  validation {
    condition     = can(regex("^[a-z][a-z0-9]{1,7}$", var.project))
    error_message = "Use de 2 a 8 caracteres minusculos alfanumericos, comecando por letra."
  }
}

variable "environment" {
  description = "Ambiente da aplicacao; use um bootstrap por ambiente."
  type        = string
  default     = "dev"
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Use dev, staging ou prod."
  }
}

variable "location" {
  description = "Regiao Azure; padrao coerente com os recursos ja observados."
  type        = string
  default     = "eastus2"
}
