#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_common.sh
source "${SCRIPT_DIR}/_common.sh"

echo "Verificando ferramentas locais..."
require_commands az docker curl python3
load_config
require_vars UNIQUE_SUFFIX AZURE_SUBSCRIPTION_ID AZURE_LOCATION RESOURCE_GROUP ACR_NAME ACI_NAME \
    DNS_NAME_LABEL IMAGE_TAG POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD
validate_azure_names

echo "Verificando autenticacao e subscription Azure..."
verify_subscription
az account show --query '{subscription:name, subscriptionId:id, tenantId:tenantId}' --output table
docker info >/dev/null

echo "Pre-requisitos atendidos. Nenhum recurso foi criado."
