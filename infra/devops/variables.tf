variable "environment" {
  description = "Mesmo ambiente do bootstrap (dev, staging ou prod)."
  type        = string

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment deve ser dev, staging ou prod."
  }
}

variable "azuredevops_organization_url" {
  description = "URL real da organização Azure DevOps, sem token."
  type        = string
}

variable "azuredevops_project_name" {
  description = "Nome do projeto Azure DevOps já existente."
  type        = string
}

variable "service_connection_name" {
  description = "Nome acordado para a ARM Service Connection federada."
  type        = string
}

variable "variable_group_name" {
  description = "Nome acordado para o grupo de variáveis não sensíveis da pipeline."
  type        = string
}

variable "identity_display_name" {
  description = "Nome da aplicação Entra ID dedicada ao ambiente."
  type        = string
}

variable "subscription_id" {
  description = "Subscription do bootstrap e do App Service."
  type        = string
}

variable "subscription_name" {
  description = "Nome exibido da subscription no Azure DevOps."
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

variable "app_resource_group_name" {
  description = "Output app_resource_group_name do bootstrap."
  type        = string
}

variable "location" {
  description = "Região do bootstrap."
  type        = string
}

variable "app_service_plan_name" {
  description = "Nome ainda não utilizado do App Service Plan."
  type        = string
}

variable "web_app_name" {
  description = "Nome globalmente único ainda não utilizado do Web App."
  type        = string
}

variable "pipeline_definition_id" {
  description = "ID da pipeline criada a partir do GitHub; quando informado, autoriza somente esta pipeline a usar connection e variable group."
  type        = number
  default     = null
  nullable    = true

  validation {
    condition     = var.pipeline_definition_id == null ? true : var.pipeline_definition_id > 0
    error_message = "pipeline_definition_id deve ser um ID positivo."
  }
}
