data "azuread_client_config" "operator" {}

data "azuredevops_project" "existing" {
  name = var.azuredevops_project_name
}

resource "azuread_application" "pipeline" {
  display_name     = var.identity_display_name
  sign_in_audience = "AzureADMyOrg"
  owners           = [data.azuread_client_config.operator.object_id]

  lifecycle {
    prevent_destroy = true
  }
}

resource "azuread_service_principal" "pipeline" {
  client_id = azuread_application.pipeline.client_id
  owners    = [data.azuread_client_config.operator.object_id]

  lifecycle {
    prevent_destroy = true
  }
}

# Manual WIF: endpoint criado antes da credencial federada; desabilitar
# validação na criação evita o ciclo. Verificar a conexão após o apply.
resource "azuredevops_serviceendpoint_azurerm" "pipeline" {
  project_id                             = data.azuredevops_project.existing.id
  service_endpoint_name                  = var.service_connection_name
  description                            = "CLYVO ${var.environment} - App Service (Workload Identity Federation)"
  service_endpoint_authentication_scheme = "WorkloadIdentityFederation"

  credentials {
    serviceprincipalid = azuread_application.pipeline.client_id
  }

  azurerm_spn_tenantid      = data.azuread_client_config.operator.tenant_id
  azurerm_subscription_id   = var.subscription_id
  azurerm_subscription_name = var.subscription_name

  features {
    validate = false
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "azuread_application_federated_identity_credential" "pipeline" {
  application_id = azuread_application.pipeline.id
  display_name   = "clyvo-${var.environment}-azuredevops"
  audiences      = ["api://AzureADTokenExchange"]
  issuer         = azuredevops_serviceendpoint_azurerm.pipeline.workload_identity_federation_issuer
  subject        = azuredevops_serviceendpoint_azurerm.pipeline.workload_identity_federation_subject

  lifecycle {
    prevent_destroy = true
  }
}

# Apenas nomes/identificadores: nenhuma senha, token ou string de conexão.
resource "azuredevops_variable_group" "pipeline" {
  project_id   = data.azuredevops_project.existing.id
  name         = var.variable_group_name
  description  = "CLYVO ${var.environment} - configurações não sensíveis do App Service"
  allow_access = false

  variable {
    name  = "TF_STATE_RESOURCE_GROUP"
    value = var.state_resource_group_name
  }
  variable {
    name  = "TF_STATE_STORAGE_ACCOUNT"
    value = var.state_storage_account_name
  }
  variable {
    name  = "TF_STATE_CONTAINER"
    value = "appstate"
  }
  variable {
    name  = "TF_STATE_KEY"
    value = "app-service/${var.environment}.tfstate"
  }
  variable {
    name  = "TF_RESOURCE_GROUP"
    value = var.app_resource_group_name
  }
  variable {
    name  = "TF_LOCATION"
    value = var.location
  }
  variable {
    name  = "TF_PLAN_NAME"
    value = var.app_service_plan_name
  }
  variable {
    name  = "TF_WEB_APP_NAME"
    value = var.web_app_name
  }
  variable {
    name  = "TF_ENVIRONMENT"
    value = var.environment
  }

  lifecycle {
    prevent_destroy = true
  }
}

# A pipeline GitHub precisa existir para se conhecer seu definition ID.
# Não usar autorização global (sem pipeline_id) para qualquer pipeline.
resource "azuredevops_pipeline_authorization" "service_connection" {
  count       = var.pipeline_definition_id == null ? 0 : 1
  project_id  = data.azuredevops_project.existing.id
  resource_id = azuredevops_serviceendpoint_azurerm.pipeline.id
  pipeline_id = var.pipeline_definition_id
  type        = "endpoint"
}

resource "azuredevops_pipeline_authorization" "variable_group" {
  count       = var.pipeline_definition_id == null ? 0 : 1
  project_id  = data.azuredevops_project.existing.id
  resource_id = azuredevops_variable_group.pipeline.id
  pipeline_id = var.pipeline_definition_id
  type        = "variablegroup"
}
