#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_common.sh
source "${SCRIPT_DIR}/_common.sh"

require_commands az python3
load_config
require_vars AZURE_LOCATION RESOURCE_GROUP ACR_NAME ACI_NAME DNS_NAME_LABEL \
    IMAGE_TAG POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD
validate_azure_names
verify_subscription

export ACR_SERVER
ACR_SERVER="$(az acr show --resource-group "$RESOURCE_GROUP" --name "$ACR_NAME" --query loginServer --output tsv)"
export ACR_USERNAME
ACR_USERNAME="$(az acr credential show --name "$ACR_NAME" --query username --output tsv)"
export ACR_PASSWORD
ACR_PASSWORD="$(az acr credential show --name "$ACR_NAME" --query 'passwords[0].value' --output tsv)"
require_vars ACR_SERVER ACR_USERNAME ACR_PASSWORD
export API_IMAGE="${ACR_SERVER}/clyvo-api:${IMAGE_TAG}"
export POSTGRES_IMAGE="${ACR_SERVER}/clyvo-postgres:${IMAGE_TAG}"
export DB_URL="jdbc:postgresql://localhost:5432/${POSTGRES_DB}"
export AZURE_LOCATION ACI_NAME POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD
export DNS_NAME_LABEL

TEMPLATE="${PROJECT_ROOT}/azure/container-group.template.yaml"
umask 077
TEMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/clyvo-container-group.XXXXXX")"
TEMP_YAML="${TEMP_DIR}/container-group.yaml"
trap 'rm -rf -- "$TEMP_DIR"' EXIT HUP INT TERM
export TEMPLATE TEMP_YAML

echo "Renderizando manifest temporario sem persistir secrets no repositorio..."
python3 <<'PY'
import json
import os
import re
from pathlib import Path

variables = (
    "AZURE_LOCATION", "ACI_NAME", "API_IMAGE", "POSTGRES_IMAGE", "DB_URL",
    "POSTGRES_DB", "POSTGRES_USER", "POSTGRES_PASSWORD", "ACR_SERVER",
    "ACR_USERNAME", "ACR_PASSWORD", "DNS_NAME_LABEL",
)
content = Path(os.environ["TEMPLATE"]).read_text(encoding="utf-8")
expected = set(variables)
found = set(re.findall(r"__([A-Z][A-Z0-9_]*)__", content))
if found != expected:
    raise SystemExit(f"Placeholders divergentes: encontrados={sorted(found)}, esperados={sorted(expected)}")
content = re.sub(
    r"__([A-Z][A-Z0-9_]*)__",
    lambda match: json.dumps(os.environ[match.group(1)]),
    content,
)
Path(os.environ["TEMP_YAML"]).write_text(content, encoding="utf-8")
PY
chmod 0600 "$TEMP_YAML"
unset ACR_PASSWORD POSTGRES_PASSWORD

echo "Criando Container Group ${ACI_NAME} com Azure CLI..."
az container create \
    --resource-group "$RESOURCE_GROUP" \
    --file "$TEMP_YAML" \
    --output none

echo "Estado atual do Container Group:"
az container show \
    --resource-group "$RESOURCE_GROUP" \
    --name "$ACI_NAME" \
    --query '{state:instanceView.state,ip:ipAddress.ip,fqdn:ipAddress.fqdn}' \
    --output table
show_endpoint
