#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${ENV_FILE:-${PROJECT_ROOT}/.env}"

load_config() {
    if [[ ! -f "$ENV_FILE" ]]; then
        echo "Arquivo de configuracao nao encontrado: ${ENV_FILE}" >&2
        echo "Crie-o com: install -m 600 ${PROJECT_ROOT}/.env.example ${PROJECT_ROOT}/.env" >&2
        return 1
    fi
    local permissions
    permissions="$(stat -c '%a' "$ENV_FILE")"
    if [[ "${permissions: -2}" != "00" ]]; then
        echo "${ENV_FILE} deve ser legivel somente pelo proprietario (chmod 600)." >&2
        return 1
    fi

    local key value
    declare -A seen=()
    while IFS='=' read -r key value || [[ -n "$key" ]]; do
        key="${key%$'\r'}"
        value="${value%$'\r'}"
        [[ -z "$key" || "$key" == \#* ]] && continue
        case "$key" in
            POSTGRES_DB|POSTGRES_USER|POSTGRES_PASSWORD|DB_URL|DB_USERNAME|DB_PASSWORD|\
            UNIQUE_SUFFIX|AZURE_SUBSCRIPTION_ID|AZURE_LOCATION|RESOURCE_GROUP|ACR_NAME|\
            ACI_NAME|DNS_NAME_LABEL|IMAGE_TAG) ;;
            *)
                echo "Variavel desconhecida em ${ENV_FILE}: ${key}" >&2
                return 1
                ;;
        esac
        if [[ -n "${seen[$key]:-}" ]]; then
            echo "Variavel duplicada em ${ENV_FILE}: ${key}" >&2
            return 1
        fi
        seen[$key]=1
        printf -v "$key" '%s' "$value"
    done < "$ENV_FILE"

    : "${UNIQUE_SUFFIX:?Defina UNIQUE_SUFFIX em .env}"
    export AZURE_LOCATION="${AZURE_LOCATION:-brazilsouth}"
    export RESOURCE_GROUP="${RESOURCE_GROUP:-clyvo-rg-${UNIQUE_SUFFIX}}"
    export ACR_NAME="${ACR_NAME:-clyvoacr${UNIQUE_SUFFIX}}"
    export ACI_NAME="${ACI_NAME:-clyvo-aci-${UNIQUE_SUFFIX}}"
    export DNS_NAME_LABEL="${DNS_NAME_LABEL:-clyvo-${UNIQUE_SUFFIX}}"
    export IMAGE_TAG="${IMAGE_TAG:-v1}"
}

require_commands() {
    local command_name
    for command_name in "$@"; do
        if ! command -v "$command_name" >/dev/null 2>&1; then
            echo "Comando obrigatorio nao encontrado: ${command_name}" >&2
            return 1
        fi
    done
}

require_vars() {
    local variable_name
    for variable_name in "$@"; do
        if [[ -z "${!variable_name:-}" ]] || [[ "${!variable_name}" == SUBSTITUA_* ]]; then
            echo "Variavel obrigatoria ausente ou ainda com placeholder: ${variable_name}" >&2
            return 1
        fi
    done
}

validate_azure_names() {
    if [[ ! "$UNIQUE_SUFFIX" =~ ^[a-z0-9]+$ ]]; then
        echo "UNIQUE_SUFFIX deve usar somente letras minusculas e numeros." >&2
        return 1
    fi
    if [[ ! "$ACR_NAME" =~ ^[a-zA-Z0-9]{5,50}$ ]]; then
        echo "ACR_NAME deve ter 5-50 caracteres alfanumericos." >&2
        return 1
    fi
    if [[ ! "$DNS_NAME_LABEL" =~ ^[a-z0-9][a-z0-9-]{0,61}[a-z0-9]$ ]]; then
        echo "DNS_NAME_LABEL deve usar letras minusculas, numeros e hifens." >&2
        return 1
    fi
    if [[ ! "$IMAGE_TAG" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]]; then
        echo "IMAGE_TAG possui formato invalido." >&2
        return 1
    fi
    if [[ ! "${POSTGRES_DB:-}" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] \
        || [[ ! "${POSTGRES_USER:-}" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
        echo "POSTGRES_DB e POSTGRES_USER devem ser identificadores PostgreSQL simples." >&2
        return 1
    fi
}

verify_subscription() {
    local current_subscription
    require_vars AZURE_SUBSCRIPTION_ID
    current_subscription="$(az account show --query id --output tsv)"
    if [[ "$current_subscription" != "$AZURE_SUBSCRIPTION_ID" ]]; then
        echo "Subscription ativa (${current_subscription}) difere de AZURE_SUBSCRIPTION_ID (${AZURE_SUBSCRIPTION_ID})." >&2
        echo "Execute az account set --subscription \"${AZURE_SUBSCRIPTION_ID}\"." >&2
        return 1
    fi
}

show_endpoint() {
    local fqdn
    fqdn="$(az container show \
        --resource-group "$RESOURCE_GROUP" \
        --name "$ACI_NAME" \
        --query ipAddress.fqdn \
        --output tsv)"
    if [[ -n "$fqdn" ]]; then
        echo "Endpoint: http://${fqdn}:8080"
    else
        echo "FQDN ainda nao disponivel. Consulte novamente com 05-status.sh."
    fi
}
