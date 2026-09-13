#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_common.sh
source "${SCRIPT_DIR}/_common.sh"

require_commands az curl
load_config
require_vars RESOURCE_GROUP ACI_NAME
verify_subscription

FQDN="$(az container show --resource-group "$RESOURCE_GROUP" --name "$ACI_NAME" --query ipAddress.fqdn --output tsv)"
if [[ -z "$FQDN" ]]; then
    echo "Container Group ainda nao possui FQDN." >&2
    exit 1
fi

BASE_URL="http://${FQDN}:8080"
echo "Testando ping em ${BASE_URL}/tutor/ping..."
curl --fail --show-error --silent --connect-timeout 5 --max-time 15 \
    --retry 12 --retry-delay 5 --retry-all-errors "${BASE_URL}/tutor/ping"
echo

echo "Testando contrato OpenAPI em ${BASE_URL}/v3/api-docs..."
curl --fail --show-error --silent --connect-timeout 5 --max-time 15 \
    --retry 12 --retry-delay 5 --retry-all-errors \
    --output /dev/null "${BASE_URL}/v3/api-docs"
echo "Smoke tests concluidos com sucesso."
