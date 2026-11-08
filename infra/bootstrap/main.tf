data "azurerm_client_config" "current" {}

locals {
  # Storage Account precisa ser globalmente unica, sem hifens e <=24 caracteres.
  name_prefix     = "${var.project}-${var.environment}"
  storage_name    = "st${var.project}${var.environment}${substr(md5(var.subscription_id), 0, min(8, 22 - length(var.project) - length(var.environment)))}"
  state_rg_name   = "rg-${var.project}-tfstate-${var.environment}"
  app_rg_name     = "rg-${local.name_prefix}"
  state_container = "tfstate"
  app_container   = "appstate"
  common_tags = {
    project     = var.project
    environment = var.environment
    managed_by  = "terraform"
    purpose     = "clyvo-bootstrap"
  }
}

module "state_storage" {
  source               = "./modules/state_storage"
  location             = var.location
  resource_group_name  = local.state_rg_name
  storage_account_name = local.storage_name
  container_name       = local.state_container
  app_container_name   = local.app_container
  operator_object_id   = data.azurerm_client_config.current.object_id
  tags                 = local.common_tags
}

module "app_resource_group" {
  source              = "./modules/app_resource_group"
  location            = var.location
  resource_group_name = local.app_rg_name
  tags                = local.common_tags
}
