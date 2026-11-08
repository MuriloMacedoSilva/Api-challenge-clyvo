# CLYVO Vet — Bootstrap de infraestrutura Azure (Terraform)

## Finalidade e regra de execucao

**EXECUTAR SOMENTE UMA VEZ POR AMBIENTE, ANTES DE HABILITAR A PIPELINE DE APLICACAO.**

Este *bootstrap* provisiona via Terraform, sem `az group create` e sem `az storage account create`:

- Resource Group do estado, `rg-clyvo-tfstate-dev` por padrao;
- Storage Account `Standard_LRS` com nome deterministico e globalmente unico;
- containers Blob privados `tfstate` (bootstrap, configuração DevOps e RBAC do operador) e `appstate` (pipeline da API), versionamento e soft delete (30 dias);
- Resource Group de execucao da API, `rg-clyvo-dev` por padrao;
- papel `Storage Blob Data Contributor` ao operador na Storage Account para migrar o state e configurar o RBAC posterior;
- nenhuma permissao da pipeline nesta etapa: `infra/access` concede posteriormente acesso **apenas** a `appstate` e ao App Service no RG da aplicacao.

Os nomes, regiao, projeto e ambiente sao parametrizaveis (variaveis Terraform). Os dois Resource Groups ficam separados; a aplicacao continua gerenciada por `infra/app-service` com **outro Terraform state**.

> Observacao: "uma vez" e uma politica operacional. O Terraform e declarativo, e pode ser executado novamente por um operador autorizado para reparar falhas ou administrar mudancas de forma controlada. **Nunca apague o estado e rode novamente como se fosse a primeira instalacao.**

## Dependencias

- Terraform CLI >= 1.8 e Azure CLI autenticado (`az login`), no ambiente do operador;
- assinatura Azure ativa com permissoes para criar RG/Storage e **atribuir roles RBAC** (`Microsoft.Authorization/roleAssignments/write`);
- provedores `Microsoft.Storage`, `Microsoft.Resources`, `Microsoft.Authorization` e `Microsoft.Web` registrados pela administracao da subscription; os providers Terraform nao registram automaticamente;
- revisao de custo antes de `--apply` (Storage Standard_LRS gera custo de armazenamento e operacoes; os Resource Groups nao tem custo direto).

Nao execute o bootstrap pela pipeline principal: o backend remoto ainda nao existe neste primeiro passo. Nao ha necessidade de JDK, Java ou Azure DevOps configurados para este bootstrap.

## Uso inicial (um unico fluxo automatizado)

Na raiz desta pasta:

```bash
./scripts/bootstrap.sh --plan
./scripts/bootstrap.sh --apply
```

- `--plan`: valida HCL e gera plano para revisao; nao provisiona recursos.
- `--apply`: executa **somente apos confirmacao** `BOOTSTRAP`, provisiona com Terraform, verifica acesso Entra ID ao Blob e migra o **proprio estado do bootstrap** do backend local temporario para `bootstrap/dev.tfstate` no Azure Blob.
- O programa nao executa `az group create`, `az storage account create`, `az role assignment create`, deploy nem `terraform destroy`.
- `az` e usado apenas para autenticacao, verificacoes e leitura de metadados.

Para outro ambiente, forneca variaveis antes dos comandos:

```bash
export TF_VAR_environment=dev
export TF_VAR_project=clyvo
export TF_VAR_location=eastus2
```

O script recupera `TF_VAR_subscription_id` da conta selecionada no Azure CLI. `environment` aceita `dev`, `staging` ou `prod`; `project` tem de 2 a 8 caracteres alfanumericos minusculos. A Storage Account usa prefixo + hash da subscription com comprimento limitado a 24 caracteres. Confirme se os dois nomes de RG e a Storage Account estao livres e aprovados antes do apply: recursos anteriores nao sao importados nem assumidos como nossos.

## Por que o bootstrap comeca com state local?

O Azure Storage que hospedara o state **ainda nao existe**. Nao ha como inicializar um backend `azurerm` nele antes de cria-lo. A primeira execucao usa state local temporario, **depois migra** o state com `terraform init -migrate-state` para o container criado. O armazenamento remoto oferece criptografia em repouso e bloqueio de state. ([Microsoft](https://learn.microsoft.com/en-us/azure/terraform/terraform-backend), [HashiCorp](https://developer.hashicorp.com/terraform/language/backend/azurerm))

O script cria `backend.tf` e `backend.hcl` localmente, ignorados pelo Git. A configuracao usa `use_azuread_auth=true`, `use_cli=true` e nao armazena access keys. O Storage tem `shared_access_key_enabled=false`; os containers sao criados atraves do ARM via AzAPI, sem Shared Key. Antes de migrar, o script confirma acesso ao Blob e recusa sobrescrever um state remoto existente; `terraform init -migrate-state` pede confirmacao interativa (sem `-force-copy`). O executor da Azure DevOps usa Workload Identity Federation no container separado `appstate`.

**Cuidado:** o `terraform.tfstate` local temporario e backups locais podem conter dados de infraestrutura. Proteja-os ate validar a migracao; nunca os versione ou apague no meio de uma falha. O programa interrompe antes da migracao caso falte acesso RBAC ao Blob.

## Recuperacao em outro clone

Depois que o state tiver sido migrado, num clone novo:

```bash
./scripts/bootstrap.sh --connect
```

Essa operacao apenas descobre a Storage Account existente pela tag, cria os arquivos locais de backend, conecta ao state remoto e lista os objetos. **Nao roda apply.**

Se o apply inicial teve falha, mantenha o `terraform.tfstate` original, resolva a falha e retome com cautela. Se o apply terminou mas a migracao foi interrompida, use `./scripts/bootstrap.sh --migrate` **somente com o state local original preservado**, apos verificar que nao ha state remoto concorrente. O comando nao aplica recursos e recusa substituir blob existente. Se houve migracao parcial ou blob remoto presente, interrompa e compare manualmente ambas as copias antes de qualquer nova acao; nao rode `--connect` enquanto houver state local nao migrado. Proteja backups locais gerados pelo Terraform, verifique `terraform state list` conectado ao Blob e somente entao descarte backups segundo politica interna.

## Integracao com infra/app-service

O backend da aplicacao deve usar:

- `resource_group_name`: output `state_resource_group_name`;
- `storage_account_name`: output `state_storage_account_name`;
- `container_name`: output `app_state_container_name` (`appstate`);
- `key`: `app-service/dev.tfstate` (ambiente parametrizado);
- `use_azuread_auth`: `true`;
- `use_oidc`: `true` para Azure DevOps WIF, conforme o task/Service Connection;
- sem credenciais hardcoded e sem acesso de pipeline ao container `tfstate` do bootstrap.

**O Terraform da aplicacao deve referenciar `rg-clyvo-dev` existente como data source**, como ja foi projetado. Ele nao pode recriar os Resource Groups do bootstrap.

A Service Connection Azure DevOps **nao e criada pelo modulo de bootstrap**. Ela e a identidade/federacao sao criadas pelo Terraform em [`infra/devops`](../devops/README.md), com state `tfstate/devops/<ambiente>.tfstate` e credenciais iniciais do operador. Depois use `infra/access` para vincular o **principal object ID** ao container `appstate` e conceder Reader, Website Contributor e Web Plan Contributor no RG da aplicacao. Nunca conceder RBAC da pipeline no container `tfstate` ou no RG de state.

A pipeline de aplicacao deve produzir `terraform plan` e exigir aprovacao explicita para `apply` e para deploy. Nunca aprovar destruicoes ou substituicoes de Storage de state.

## Seguranca e limites

- `prevent_destroy` nos dois Resource Groups, Storage Account e containers; protecao nao substitui controle de acesso.
- Container privado, sem acesso publico a blobs, TLS >=1.2, Shared Key desabilitado.
- Storage acessivel por endpoint publico para compatibilidade com agente MS-hosted, **mas autenticado por Entra ID**; para ambiente mais restrito, usar agentes privados e firewall/Private Endpoint.
- Mesmo com soft delete e versoes, mantenha estrategia de backup e controles de acesso ao state.
- Para validacao offline, instalar Terraform e rodar `terraform fmt -check -recursive`, `terraform init -backend=false -input=false` e `terraform validate`. `init` baixa providers; `validate` nao faz chamadas Azure. O script executa essas verificacoes antes do plan; `--plan` **consulta Azure** e nao e validacao offline.
- A unica conta com acesso a todos os states e o operador do bootstrap; restrinja e monitore essa conta. Nao executar o script ACI `azure/scripts/99-destroy.sh` contra nenhum dos RGs deste fluxo.
