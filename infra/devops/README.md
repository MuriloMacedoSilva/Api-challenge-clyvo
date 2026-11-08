# Azure DevOps / Entra ID — Terraform de configuração inicial

Este diretório é executado **pelo operador, fora da pipeline Java**, depois do bootstrap e antes de `infra/access`. Seu state fica em `tfstate`, key `devops/<ambiente>.tfstate` (separada de `bootstrap/` e `access/`); a identidade da pipeline só recebe acesso ao container `appstate` via `infra/access`. Nenhum secret, PAT ou credencial persistente é definido em HCL/tfvars.

## Recursos e sequência

O módulo consulta o **projeto Azure DevOps existente**, cria uma aplicação e service principal Entra ID dedicadas ao ambiente, uma **ARM Service Connection manual com WIF**, a credencial federada usando o `issuer`/`subject` retornados pelo endpoint, e um variable group **somente com nomes e IDs não sensíveis**. A conexão é criada com `features.validate=false`: a validação não pode anteceder a credencial federada. Após o apply, testar a conexão com a permissão da identidade já propagada. A Service Connection é subscription-scoped no Azure DevOps para compartilhar a mesma subscription com o backend; o RBAC efetivo fica limitado em `infra/access` ao RG da aplicação e ao container `appstate`.

Os recursos de identidade, federação, connection e variable group usam `prevent_destroy`. Alterar nomes/identidade pode exigir substituição e deve ser tratado como migração controlada. **Não** autorizar acesso da conexão nem do group a todas as pipelines: `allow_access=false` e `azuredevops_pipeline_authorization` somente quando o ID de uma pipeline específica for informado (`pipeline_id` é omitido pelo provider para autorização global). Não há provisionamento de GitHub Service Connection, repositório, projeto Azure DevOps ou da pipeline YAML: primeiro o operador cria/conecta a pipeline GitHub no projeto existente e obtém seu *definition ID*. Uma segunda execução revisada deste mesmo Terraform com `pipeline_definition_id` autoriza apenas essa pipeline. Se as autorizações antigas já estiverem no state, revisar a migração dos endereços `azuredevops_resource_authorization` para o novo tipo antes de qualquer plan/apply; a validação local não consulta o state remoto. Proteger a branch `main` e os aprovadores de deploy no Azure DevOps (políticas de organização/projeto), pois alterações no YAML podem enfraquecer gates versionados.

## Credenciais iniciais inevitáveis

- Operador com `az login` na subscription aprovada e RBAC para o bootstrap e `infra/access` (incluindo `Microsoft.Authorization/roleAssignments/write`), além de Reader no storage/RGs; os resource providers Azure necessários devem estar registrados pelo administrador.
- Permissões no Microsoft Entra ID para criar aplicações/service principals e federated credentials, por exemplo função Application Administrator atribuída ao operador (ou papel equivalente autorizado). Não criar senhas para a service principal da pipeline.
- Permissão administrativa no projeto Azure DevOps para criar Service Connections, variable groups e autorizar recursos por pipeline. O provider `microsoft/azuredevops` requer autenticação **prévia do operador**: usar `AZDO_ORG_SERVICE_URL`/`AZDO_PERSONAL_ACCESS_TOKEN` em ambiente privado com PAT de curta duração e escopos mínimos necessários, ou identidade Entra já habilitada no Azure DevOps se suportada pelo operador. Não usar o token da Service Connection recém-criada para criar a si mesma. Revogar/expirar o PAT após configuração; não salvar em arquivos, shell history, outputs, logs ou variable group. Se a organização bloquear PAT, obter identidade administrativa existente para o provider; não há bootstrap autônomo sem confiança inicial.

Confirmar permissões exatas de criação de Service Endpoints, Library/variable groups e Build authorization na organização. Não abrir acesso da conexão ao projeto inteiro. Verificar nomes/IDs em cada plan; este Terraform **não importa** recursos já existentes.

## Variáveis e state

Definir localmente, sem tfvars versionados: `TF_VAR_environment`, `TF_VAR_azuredevops_organization_url`, `TF_VAR_azuredevops_project_name`, `TF_VAR_service_connection_name`, `TF_VAR_variable_group_name`, `TF_VAR_identity_display_name`, `TF_VAR_subscription_id`, `TF_VAR_subscription_name`, `TF_VAR_state_resource_group_name`, `TF_VAR_state_storage_account_name`, `TF_VAR_app_resource_group_name`, `TF_VAR_location`, `TF_VAR_app_service_plan_name`, `TF_VAR_web_app_name`. Os RG/storage são outputs do bootstrap; nomes de connection, group, plan e app são decisões do operador. Configurar `ARM_SUBSCRIPTION_ID` para o provider/backend e `AZDO_PERSONAL_ACCESS_TOKEN` **somente** na sessão privada do operador; o provider AzureAD pode usar a sessão Azure CLI do operador. `TF_VAR_pipeline_definition_id` é opcional e deve conter somente o ID da pipeline GitHub já criada.

No diretório `infra/devops`, após confirmar que o state do bootstrap está remoto:

```bash
terraform fmt -check -recursive
terraform init -input=false \
  -backend-config="resource_group_name=<RG_STATE>" \
  -backend-config="storage_account_name=<STORAGE>" \
  -backend-config="container_name=tfstate" \
  -backend-config="key=devops/<AMBIENTE>.tfstate" \
  -backend-config="use_azuread_auth=true" -backend-config="use_cli=true"
terraform validate
terraform plan -out=devops.tfplan
# Após revisão explícita pelo operador: terraform apply devops.tfplan
```

`terraform output -raw pipeline_principal_object_id` fornece o **object ID**, não o client ID, necessário para `infra/access`. `service_connection_name` e `variable_group_name` devem substituir os defaults placeholders dos parâmetros da pipeline antes de ativar triggers automáticos. Depois de criar a pipeline a partir do GitHub (sem executá-la), preencher `pipeline_definition_id` e repetir `plan`/revisão para autorizar precisamente essa definição. A autorização Azure DevOps e a propagação da credencial federada podem demorar; testar a conexão antes do primeiro plan da aplicação. Guarde states/plans com permissões restritas mesmo sem senha intencional: respostas de APIs e metadados podem conter valores inesperados.
