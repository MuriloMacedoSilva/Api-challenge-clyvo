data "azurerm_resource_group" "app" {
  name = var.app_resource_group_name
}

data "azurerm_resource_group" "state" {
  name = var.state_resource_group_name
}

locals {
  # O escopo e ARM (management plane): não requer Shared Key para ler o container.
  state_container_id = "${data.azurerm_resource_group.state.id}/providers/Microsoft.Storage/storageAccounts/${var.state_storage_account_name}/blobServices/default/containers/${var.state_container_name}"
}

resource "azurerm_role_assignment" "pipeline_state" {
  scope                = local.state_container_id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = var.pipeline_principal_object_id

  lifecycle {
    prevent_destroy = true
  }
}

resource "azurerm_role_assignment" "pipeline_app_reader" {
  scope                = data.azurerm_resource_group.app.id
  role_definition_name = "Reader"
  principal_id         = var.pipeline_principal_object_id

  lifecycle {
    prevent_destroy = true
  }
}

resource "azurerm_role_assignment" "pipeline_web_app" {
  scope                = data.azurerm_resource_group.app.id
  role_definition_name = "Website Contributor"
  principal_id         = var.pipeline_principal_object_id

  lifecycle {
    prevent_destroy = true
  }
}

resource "azurerm_role_assignment" "pipeline_web_plan" {
  scope                = data.azurerm_resource_group.app.id
  role_definition_name = "Web Plan Contributor"
  principal_id         = var.pipeline_principal_object_id

  lifecycle {
    prevent_destroy = true
  }
}
