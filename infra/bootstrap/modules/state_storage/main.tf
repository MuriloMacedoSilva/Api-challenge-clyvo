resource "azurerm_resource_group" "state" {
  name     = var.resource_group_name
  location = var.location
  tags     = var.tags

  lifecycle {
    prevent_destroy = true
  }
}

resource "azurerm_storage_account" "state" {
  name                            = var.storage_account_name
  resource_group_name             = azurerm_resource_group.state.name
  location                        = azurerm_resource_group.state.location
  account_tier                    = "Standard"
  account_replication_type        = "LRS"
  account_kind                    = "StorageV2"
  min_tls_version                 = "TLS1_2"
  https_traffic_only_enabled      = true
  allow_nested_items_to_be_public = false
  shared_access_key_enabled       = false
  local_user_enabled              = false
  default_to_oauth_authentication = true
  public_network_access_enabled   = true # Agentes hospedados precisam alcancar o endpoint; proteger via Entra ID/RBAC.
  tags                            = var.tags

  blob_properties {
    versioning_enabled = true
    delete_retention_policy {
      days = 30
    }
    container_delete_retention_policy {
      days = 30
    }
  }

  lifecycle {
    prevent_destroy = true
  }
}

# Container criado pelo ARM management plane via AzAPI; sem precisar liberar Shared Key.
resource "azapi_resource" "state_container" {
  type      = "Microsoft.Storage/storageAccounts/blobServices/containers@2022-09-01"
  parent_id = "${azurerm_storage_account.state.id}/blobServices/default"
  name      = var.container_name
  body = {
    properties = {
      publicAccess = "None"
    }
  }
  lifecycle {
    prevent_destroy = true
  }
}

resource "azapi_resource" "app_container" {
  type      = "Microsoft.Storage/storageAccounts/blobServices/containers@2022-09-01"
  parent_id = "${azurerm_storage_account.state.id}/blobServices/default"
  name      = var.app_container_name
  body = {
    properties = {
      publicAccess = "None"
    }
  }
  lifecycle {
    prevent_destroy = true
  }
}

resource "azurerm_role_assignment" "operator_state" {
  # O operador administra o bootstrap, o RBAC pós-bootstrap e a migração.
  scope                = azurerm_storage_account.state.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = var.operator_object_id
}
