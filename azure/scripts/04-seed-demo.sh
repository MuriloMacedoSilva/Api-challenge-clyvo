#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_common.sh
source "${SCRIPT_DIR}/_common.sh"

require_commands az
load_config
require_vars RESOURCE_GROUP ACI_NAME POSTGRES_DB POSTGRES_USER
verify_subscription

echo "ATENCAO: o seed limpa e recria todos os dados demonstrativos."
read -r -p "Digite o nome do Container Group (${ACI_NAME}) para confirmar: " confirmation
if [[ "$confirmation" != "$ACI_NAME" ]]; then
    echo "Confirmacao divergente. Seed cancelado."
    exit 1
fi

echo "Aguardando PostgreSQL aceitar conexoes..."
ready=false
for attempt in {1..30}; do
    if az container exec \
        --resource-group "$RESOURCE_GROUP" \
        --name "$ACI_NAME" \
        --container-name clyvo-postgres \
        --exec-command "pg_isready --username=${POSTGRES_USER} --dbname=${POSTGRES_DB}" \
        >/dev/null 2>&1; then
        ready=true
        break
    fi
    echo "PostgreSQL ainda indisponivel (${attempt}/30)."
    sleep 5
done
if [[ "$ready" != true ]]; then
    echo "PostgreSQL nao ficou pronto no tempo esperado." >&2
    exit 1
fi

echo "Executando demo-data-postgres.sql no container clyvo-postgres..."
seed_output="$(az container exec \
    --resource-group "$RESOURCE_GROUP" \
    --name "$ACI_NAME" \
    --container-name clyvo-postgres \
    --exec-command "psql --username=${POSTGRES_USER} --dbname=${POSTGRES_DB} --file=/seed/demo-data-postgres.sql")"
printf '%s\n' "$seed_output"
if [[ "$seed_output" != *COMMIT* ]] || [[ "$seed_output" == *"command terminated with non-zero exit code"* ]]; then
    echo "Seed nao concluiu com COMMIT." >&2
    exit 1
fi

echo "Seed de demonstracao concluido."
