#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_common.sh
source "${SCRIPT_DIR}/_common.sh"

require_commands az
load_config
require_vars RESOURCE_GROUP
verify_subscription

echo "ATENCAO: todos os recursos CLYVO no Resource Group ${RESOURCE_GROUP} serao removidos."
echo "Subscription confirmada: ${AZURE_SUBSCRIPTION_ID}"
read -r -p "Digite o nome exato do Resource Group para confirmar: " confirmation
if [[ "$confirmation" != "$RESOURCE_GROUP" ]]; then
    echo "Confirmacao divergente. Nenhum recurso foi removido."
    exit 1
fi

az group delete --name "$RESOURCE_GROUP" --yes
group_exists="$(az group exists --name "$RESOURCE_GROUP" --output tsv)"
if [[ "$group_exists" != false ]]; then
    echo "Resource Group ainda existe apos a tentativa de remocao." >&2
    exit 1
fi
echo "Resource Group ${RESOURCE_GROUP} removido. az group exists: false"
