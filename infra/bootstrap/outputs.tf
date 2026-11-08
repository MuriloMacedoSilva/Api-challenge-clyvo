output "state_resource_group_name" {
  value = module.state_storage.resource_group_name
}
output "state_storage_account_name" {
  value = module.state_storage.storage_account_name
}
output "state_container_name" {
  value = module.state_storage.container_name
}
output "app_state_container_name" {
  value = module.state_storage.app_container_name
}
output "app_resource_group_name" {
  value = module.app_resource_group.resource_group_name
}
output "app_service_state_key" {
  value = "app-service/${var.environment}.tfstate"
}
output "bootstrap_state_key" {
  value = "bootstrap/${var.environment}.tfstate"
}
