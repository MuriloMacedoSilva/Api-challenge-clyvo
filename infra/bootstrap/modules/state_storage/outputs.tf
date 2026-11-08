output "resource_group_name" {
  value = azurerm_resource_group.state.name
}
output "storage_account_name" {
  value = azurerm_storage_account.state.name
}
output "container_name" {
  value = azapi_resource.state_container.name
}
output "app_container_name" {
  value = azapi_resource.app_container.name
}
