#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_common.sh
source "${SCRIPT_DIR}/_common.sh"

require_commands az
load_config
require_vars RESOURCE_GROUP ACR_NAME ACI_NAME
verify_subscription

echo "Resource Group:"
az group show --name "$RESOURCE_GROUP" --query '{name:name,location:location,state:properties.provisioningState}' --output table
echo "Azure Container Registry:"
az acr show --resource-group "$RESOURCE_GROUP" --name "$ACR_NAME" --query '{name:name,server:loginServer,sku:sku.name,state:provisioningState}' --output table
echo "Container Group e containers:"
az container show \
    --resource-group "$RESOURCE_GROUP" \
    --name "$ACI_NAME" \
    --query '{group:name,state:instanceView.state,ip:ipAddress.ip,fqdn:ipAddress.fqdn,containers:containers[].{name:name,state:instanceView.currentState.state,detailStatus:instanceView.currentState.detailStatus,restartCount:instanceView.restartCount,exitCode:instanceView.currentState.exitCode}}' \
    --output yaml
show_endpoint
