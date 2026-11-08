variable "resource_group_name" {
  type = string
}
variable "location" {
  type = string
}
variable "storage_account_name" {
  type = string
}
variable "container_name" {
  type = string
}
variable "app_container_name" {
  type = string
}
variable "operator_object_id" {
  type = string
}
variable "tags" {
  type = map(string)
}
