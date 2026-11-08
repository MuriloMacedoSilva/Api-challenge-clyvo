output "resource_group_name" {
  value = data.azurerm_resource_group.existing.name
}

output "web_app_name" {
  value = azurerm_linux_web_app.api.name
}

output "web_app_id" {
  value = azurerm_linux_web_app.api.id
}

output "default_hostname" {
  value = azurerm_linux_web_app.api.default_hostname
}
