output "service_connection_name" {
  value = azuredevops_serviceendpoint_azurerm.pipeline.service_endpoint_name
}

output "service_connection_id" {
  value = azuredevops_serviceendpoint_azurerm.pipeline.id
}

output "pipeline_principal_object_id" {
  description = "Passar a infra/access; diferente do client ID."
  value       = azuread_service_principal.pipeline.object_id
}

output "variable_group_name" {
  value = azuredevops_variable_group.pipeline.name
}
