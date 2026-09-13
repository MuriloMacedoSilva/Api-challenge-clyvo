#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_common.sh
source "${SCRIPT_DIR}/_common.sh"

require_commands az
load_config
require_vars AZURE_LOCATION RESOURCE_GROUP ACR_NAME
validate_azure_names
verify_subscription

echo "Criando/atualizando Resource Group ${RESOURCE_GROUP}..."
az group create --name "$RESOURCE_GROUP" --location "$AZURE_LOCATION" --output table

if az acr show --resource-group "$RESOURCE_GROUP" --name "$ACR_NAME" >/dev/null 2>&1; then
    echo "ACR ${ACR_NAME} ja existe."
else
    echo "Criando ACR Basic ${ACR_NAME}..."
    az acr create \
        --resource-group "$RESOURCE_GROUP" \
        --name "$ACR_NAME" \
        --sku Basic \
        --admin-enabled true \
        --output table
fi
az acr update --resource-group "$RESOURCE_GROUP" --name "$ACR_NAME" --admin-enabled true --output none

echo "Recursos base preparados."
