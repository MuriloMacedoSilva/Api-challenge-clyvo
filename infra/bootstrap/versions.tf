terraform {
  required_version = ">= 1.8.0, < 2.0.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 4.0, < 5.0"
    }
    azapi = {
      source  = "Azure/azapi"
      version = ">= 2.0, < 3.0"
    }
  }
  # O bootstrap começa com state LOCAL somente na primeira execução.
  # scripts/bootstrap.sh gera um backend.tf local e migra para Azure Blob após o apply.
}

provider "azurerm" {
  features {}
  subscription_id                 = var.subscription_id
  resource_provider_registrations = "none"
}

provider "azapi" {
  subscription_id = var.subscription_id
}
