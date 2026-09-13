#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_common.sh
source "${SCRIPT_DIR}/_common.sh"

require_commands az docker
load_config
require_vars RESOURCE_GROUP ACR_NAME IMAGE_TAG
validate_azure_names
verify_subscription

ACR_SERVER="$(az acr show \
    --resource-group "$RESOURCE_GROUP" \
    --name "$ACR_NAME" \
    --query loginServer \
    --output tsv)"

echo "Autenticando Docker no ACR ${ACR_NAME}..."
az acr login --name "$ACR_NAME"

echo "Construindo imagem local da API..."
docker build --file "${PROJECT_ROOT}/Dockerfile" --tag "clyvo-api:${IMAGE_TAG}" "$PROJECT_ROOT"

echo "Construindo imagem local do PostgreSQL..."
docker build --file "${PROJECT_ROOT}/Dockerfile.postgres" --tag "clyvo-postgres:${IMAGE_TAG}" "$PROJECT_ROOT"

echo "Criando tags do ACR..."
docker tag "clyvo-api:${IMAGE_TAG}" "${ACR_SERVER}/clyvo-api:${IMAGE_TAG}"
docker tag "clyvo-postgres:${IMAGE_TAG}" "${ACR_SERVER}/clyvo-postgres:${IMAGE_TAG}"

echo "Enviando imagens ao ACR..."
docker push "${ACR_SERVER}/clyvo-api:${IMAGE_TAG}"
docker push "${ACR_SERVER}/clyvo-postgres:${IMAGE_TAG}"

echo "Confirmando tags publicadas no ACR..."
az acr repository show-tags \
    --name "$ACR_NAME" \
    --repository clyvo-api \
    --query "[?@=='${IMAGE_TAG}']" \
    --output table
az acr repository show-tags \
    --name "$ACR_NAME" \
    --repository clyvo-postgres \
    --query "[?@=='${IMAGE_TAG}']" \
    --output table

echo "Imagens publicadas com a tag ${IMAGE_TAG}."
