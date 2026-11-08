terraform {
  required_version = ">= 1.8.0, < 2.0.0"

  required_providers {
    azuread = {
      source  = "hashicorp/azuread"
      version = ">= 3.0, < 4.0"
    }
    azuredevops = {
      source  = "microsoft/azuredevops"
      version = ">= 1.0, < 2.0"
    }
  }

  # Operator-only container/key; never initialized by the application pipeline.
  backend "azurerm" {}
}

provider "azuread" {}

provider "azuredevops" {
  org_service_url = var.azuredevops_organization_url
}
