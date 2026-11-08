terraform {
  required_version = ">= 1.8.0, < 2.0.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
  }

  # Configuração remota separada de bootstrap e app-service (access/<ambiente>.tfstate).
  backend "azurerm" {}
}

provider "azurerm" {
  features {}
  resource_provider_registrations = "none"
}
