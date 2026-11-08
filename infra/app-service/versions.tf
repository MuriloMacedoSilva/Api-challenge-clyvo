terraform {
  required_version = ">= 1.8.0, < 2.0.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
  }

  # Configuração parcial: storage account/container/key e autenticação OIDC são
  # fornecidos em terraform init; nenhum estado local é usado na pipeline.
  backend "azurerm" {}
}

provider "azurerm" {
  features {}
  resource_provider_registrations = "none"
}
