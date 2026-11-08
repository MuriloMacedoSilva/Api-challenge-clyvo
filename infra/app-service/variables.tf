variable "environment" {
  description = "Ambiente isolado (por exemplo: dev, staging, prod)."
  type        = string

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment deve ser dev, staging ou prod."
  }
}

variable "resource_group_name" {
  description = "Resource Group existente, pré-aprovado; não é gerenciado por este módulo."
  type        = string
}

variable "location" {
  description = "Região dos novos recursos (compatível com o Resource Group)."
  type        = string
}

variable "app_service_plan_name" {
  description = "Nome do App Service Plan Linux a criar."
  type        = string
}

variable "web_app_name" {
  description = "Nome globalmente único do Linux Web App a criar."
  type        = string
}

variable "plan_sku" {
  description = "SKU inicial B1; dimensionar após medir memória/carga e consultar preços regionais."
  type        = string
  default     = "B1"

  validation {
    condition     = contains(["B1", "B2", "B3", "S1", "S2", "S3"], var.plan_sku)
    error_message = "Escolha um SKU Basic ou Standard suportado para Java SE Linux."
  }
}

variable "tags" {
  description = "Tags adicionais sem valores sensíveis."
  type        = map(string)
  default     = {}
}
