data "azurerm_resource_group" "existing" {
  name = var.resource_group_name
}

locals {
  tags = merge(var.tags, {
    application = "clyvo-api"
    environment = var.environment
    managed_by  = "terraform"
  })
}

resource "azurerm_service_plan" "api" {
  name                = var.app_service_plan_name
  resource_group_name = data.azurerm_resource_group.existing.name
  location            = var.location
  os_type             = "Linux"
  sku_name            = var.plan_sku
  tags                = local.tags

  lifecycle {
    precondition {
      condition     = try(data.azurerm_resource_group.existing.tags["purpose"], "") == "clyvo-bootstrap" && try(data.azurerm_resource_group.existing.tags["environment"], "") == var.environment && lower(data.azurerm_resource_group.existing.location) == lower(var.location)
      error_message = "O Resource Group deve ser o RG de aplicacao criado pelo bootstrap no mesmo ambiente e regiao."
    }
  }
}

resource "azurerm_linux_web_app" "api" {
  name                = var.web_app_name
  resource_group_name = data.azurerm_resource_group.existing.name
  location            = var.location
  service_plan_id     = azurerm_service_plan.api.id
  https_only          = true
  tags                = local.tags

  site_config {
    always_on                         = true
    minimum_tls_version               = "1.2"
    ftps_state                        = "Disabled"
    health_check_path                 = "/tutor/ping"
    health_check_eviction_time_in_min = 10

    application_stack {
      java_version        = "21"
      java_server         = "JAVA"
      java_server_version = "21"
    }
  }

  app_settings = {
    SPRING_PROFILES_ACTIVE = "prod"
  }

  lifecycle {
    # DB_URL/DB_USERNAME/DB_PASSWORD são configurados fora do Terraform, após
    # a criação do app. Evita gravar valores de App Settings no state/plan.
    ignore_changes = [app_settings]
  }
}
