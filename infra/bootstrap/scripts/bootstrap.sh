#!/usr/bin/env bash
# EXECUCAO INICIAL UMA VEZ POR AMBIENTE: prepara state remoto e RG da aplicacao via Terraform.
# Nenhum recurso Azure e criado via az CLI; az CLI serve somente para autenticacao/verificacao.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${1:---help}"
cd "$ROOT"

usage() {
  cat <<'TXT'
Uso:
  ./scripts/bootstrap.sh --plan     # valida e gera plano local, SEM recursos Azure
  ./scripts/bootstrap.sh --apply    # aplica com confirmacao e migra bootstrap state para Azure Blob
  ./scripts/bootstrap.sh --connect  # em outro clone, conecta ao bootstrap state existente, sem apply
  ./scripts/bootstrap.sh --migrate  # recupera migracao interrompida, sem apply

Requer: Azure CLI autenticado (az login), Terraform >=1.8, permissoes RBAC de
criar RG/Storage e atribuir Storage Blob Data Contributor. Sem secrets no Git.
Configuracao opcional: TF_VAR_project (padrao clyvo), TF_VAR_environment (padrao dev),
TF_VAR_location (padrao eastus2). Bootstrap nao concede acesso a pipeline.
TXT
}

case "$MODE" in --plan|--apply|--connect|--migrate) ;; *) usage; exit 0 ;; esac
for cmd in az terraform; do command -v "$cmd" >/dev/null || { echo "ERRO: instale $cmd" >&2; exit 1; }; done
export TF_VAR_subscription_id="${TF_VAR_subscription_id:-$(az account show --query id -o tsv)}"
current_sub="$(az account show --query id -o tsv)"
[[ "$TF_VAR_subscription_id" == "$current_sub" ]] || { echo "ERRO: Azure CLI aponta para outra subscription." >&2; exit 1; }
project="${TF_VAR_project:-clyvo}"
env="${TF_VAR_environment:-dev}"
[[ "$project" =~ ^[a-z][a-z0-9]{1,7}$ && ( "$env" == dev || "$env" == staging || "$env" == prod ) ]] || {
  echo "ERRO: project/environment invalidos; verifique as variaveis Terraform." >&2; exit 1;
}
state_rg="rg-${project}-tfstate-${env}"
app_rg="rg-${project}-${env}"
container="tfstate"
key="bootstrap/${env}.tfstate"

create_backend_files() {
  local storage="$1"
  cat > backend.tf <<'TF'
terraform {
  backend "azurerm" {}
}
TF
  cat > backend.hcl <<CFG
resource_group_name  = "$state_rg"
storage_account_name = "$storage"
container_name       = "$container"
key                  = "$key"
use_azuread_auth     = true
use_cli              = true
subscription_id      = "$TF_VAR_subscription_id"
CFG
}

find_storage() {
  local existing
  existing="$(az storage account list --resource-group "$state_rg" \
    --query "[?tags.purpose=='clyvo-bootstrap'].name" -o tsv)"
  [[ "$(printf '%s\n' "$existing" | sed '/^$/d' | wc -l)" -eq 1 ]] || {
    echo "ERRO: esperado exatamente 1 Storage Account gerenciado pelo bootstrap em $state_rg" >&2; exit 1;
  }
  printf '%s' "$existing"
}

remote_state_absent() {
  local storage="$1" exists
  exists="$(az storage blob exists --auth-mode login --account-name "$storage" --container-name "$container" --name "$key" --query exists -o tsv)"
  [[ "${exists,,}" == "false" ]] || {
    echo "ERRO: state remoto existe (ou consulta inesperada) em $key; nao sobrescrever." >&2
    exit 1
  }
}

if [[ "$MODE" == "--connect" ]]; then
  [[ ! -f terraform.tfstate && ! -f terraform.tfstate.backup ]] || {
    echo "ERRO: local state encontrado. Nao reconecte sem investigar/migrar o state local." >&2; exit 1;
  }
  storage="$(find_storage)"
  exists="$(az storage blob exists --auth-mode login --account-name "$storage" --container-name "$container" --name "$key" --query exists -o tsv)"
  [[ "${exists,,}" == "true" ]] || { echo "ERRO: state remoto nao encontrado em $key." >&2; exit 1; }
  create_backend_files "$storage"
  terraform init -input=false -reconfigure -backend-config=backend.hcl
  terraform state list
  echo "Conectado ao bootstrap state remoto, sem criar recursos."
  exit 0
fi

if [[ "$MODE" == "--migrate" ]]; then
  [[ -f terraform.tfstate ]] || { echo "ERRO: state local nao encontrado." >&2; exit 1; }
  storage="$(find_storage)"
  remote_state_absent "$storage"
  create_backend_files "$storage"
  terraform init -migrate-state -backend-config=backend.hcl
  terraform state list
  echo "State migrado; proteja eventuais backups locais."
  exit 0
fi

if [[ -f backend.tf ]]; then
  echo "ERRO: backend.tf existe: use --migrate se houver state local, ou --connect em clone limpo." >&2
  exit 1
fi

if [[ ! -f terraform.tfstate && "$(az group exists --name "$state_rg")" == "true" ]]; then
  echo "ERRO: RG do state ja existe mas nenhum state local foi encontrado." >&2
  echo "Use --connect ou recupere o state anterior; nao importe/crie por cima sem revisao." >&2
  exit 1
fi
if [[ -f terraform.tfstate && "$(az group exists --name "$state_rg")" == "true" ]]; then
  storage="$(find_storage)"
  remote_state_absent "$storage"
fi

terraform init -input=false
terraform fmt -check -recursive
terraform validate
terraform plan -input=false -out=bootstrap.tfplan

if [[ "$MODE" == "--plan" ]]; then
  echo "Plan criado. Nenhuma alteracao aplicada."
  exit 0
fi

printf '\nATENCAO: isto criara recursos Azure e podera gerar custos.\n'
printf 'Subscription: %s\nState RG: %s\nApp RG: %s\n' "$TF_VAR_subscription_id" "$state_rg" "$app_rg"
printf 'Digite BOOTSTRAP para autorizar o Terraform Apply: '
read -r approval
[[ "$approval" == "BOOTSTRAP" ]] || { echo "Cancelado."; exit 1; }
terraform apply -input=false bootstrap.tfplan

storage="$(terraform output -raw state_storage_account_name)"
# RBAC no data plane pode levar alguns minutos para propagar.
ready=0
for i in {1..18}; do
  if az storage blob list --auth-mode login --account-name "$storage" --container-name "$container" -o none 2>/dev/null; then
    ready=1
    break
  fi
  echo "Aguardando permissao Entra ID/RBAC de leitura do container (tentativa $i/18)..."
  sleep 10
done
if [[ "$ready" != 1 ]]; then
  echo "ERRO: sem acesso ao Blob Storage. STATE LOCAL PRESERVADO. Corrija RBAC e retome com o state atual." >&2
  exit 1
fi

remote_state_absent "$storage"
create_backend_files "$storage"
terraform init -migrate-state -backend-config=backend.hcl
terraform state list > /dev/null
echo "Bootstrap concluido e state migrado para Blob Storage. Preservar backups locais com seguranca."
echo "Backend da aplicacao: resource_group_name=$state_rg storage_account_name=$storage container_name=appstate key=app-service/${env}.tfstate"
