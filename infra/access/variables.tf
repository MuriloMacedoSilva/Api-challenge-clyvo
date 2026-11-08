variable "environment" {
  type = string
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment deve ser dev, staging ou prod."
  }
}

variable "app_resource_group_name" {
  description = "Output app_resource_group_name do bootstrap."
  type        = string
}

variable "state_resource_group_name" {
  description = "Output state_resource_group_name do bootstrap."
  type        = string
}

variable "state_storage_account_name" {
  description = "Output state_storage_account_name do bootstrap."
  type        = string
}

variable "state_container_name" {
  description = "Output state_container_name do bootstrap."
  type        = string
}

variable "pipeline_principal_object_id" {
  description = "Object ID da service principal Entra ID da Service Connection WIF (não client ID)."
  type        = string
  validation {
    condition     = can(regex("^[0-9a-fA-F-]{36}$", var.pipeline_principal_object_id))
    error_message = "Informe o object ID UUID da service principal."
  }
}
