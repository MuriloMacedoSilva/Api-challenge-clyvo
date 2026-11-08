# RBAC pós-bootstrap (execução manual, independente)

Este Terraform **não cria Resource Groups, Storage Accounts, containers, Service Connections ou aplicações**. Depois de migrar o state do bootstrap e executar [infra/devops](../devops/README.md), o operador concede à service principal da pipeline:

- `Storage Blob Data Contributor` **somente** no container `appstate`, nunca no container `tfstate` do bootstrap;
- `Reader`, `Website Contributor` e `Web Plan Contributor` no Resource Group da API. Os roles de website/plan permitem que a pipeline administre Web App e Plan sem receber permissão para deletar Resource Groups ou alterar o bootstrap. Rever políticas customizadas da organização antes do primeiro plan.

O próprio `infra/access` usa state remoto `tfstate`, key `access/<ambiente>.tfstate`, acessível **somente ao operador**; é separado da key `bootstrap/<ambiente>.tfstate` e do container `appstate`. O `prevent_destroy` protege assignments administrados aqui contra remoção acidental. Alterações de identidade exigem plano e revisão específicos.

## Preparação manual

1. Concluir `infra/bootstrap/scripts/bootstrap.sh --apply` e confirmar que o state está remoto. Obter os outputs com `terraform -chdir=infra/bootstrap output -raw <nome>` no clone conectado (`--connect` em clone novo).
2. Executar `infra/devops` com aprovação explícita do operador para criar a Azure Resource Manager Service Connection **WIF** e a service principal Entra ID. Obter seu output `pipeline_principal_object_id` (não client ID nem ID da connection). Conceder ao operador direito de criar role assignments no RG da API e no container `appstate`. Não conceder esses papéis via portal/CLI fora do Terraform.
3. Com Azure CLI autenticado na subscription correta (`az login`, `az account show`), configurar `ARM_SUBSCRIPTION_ID` com o ID real da subscription e as variáveis `TF_VAR_environment`, `TF_VAR_app_resource_group_name`, `TF_VAR_state_resource_group_name`, `TF_VAR_state_storage_account_name`, `TF_VAR_state_container_name=appstate`, `TF_VAR_pipeline_principal_object_id` com valores verificados dos outputs e do Entra ID. Não versionar tfvars reais.
4. No diretório `infra/access`, inicializar o backend **usando o container `tfstate`**, não `appstate`:

   ```bash
   terraform init -input=false \
     -backend-config="resource_group_name=<RG_STATE>" \
     -backend-config="storage_account_name=<STORAGE>" \
     -backend-config="container_name=tfstate" \
     -backend-config="key=access/<AMBIENTE>.tfstate" \
     -backend-config="use_azuread_auth=true" -backend-config="use_cli=true"
   terraform fmt -check -recursive
   terraform validate
   terraform plan -out=access.tfplan
   # Após revisar escopos, identidade e actions: terraform apply access.tfplan
   ```

   A configuração do backend fica na pasta `.terraform/`, ignorada pelo Git. Conferir RG/Storage/ambiente e que nenhuma ação de destruição está presente; não executar `apply` automaticamente. A RBAC pode demorar a propagar. A pipeline só deve ser habilitada depois de validar acesso ao Blob em `appstate` e permissão no RG da aplicação. Guardar state e plan com acesso restrito.

Service Endpoint ARM, federação Entra ID e variable group são geridos por `infra/devops` em state separado. Autorização por pipeline exige seu definition ID e é aplicada lá em uma segunda revisão; a criação da pipeline GitHub e as credenciais/permissões iniciais do operador continuam externas ao Terraform, pois a Service Connection não pode criar a si própria.
