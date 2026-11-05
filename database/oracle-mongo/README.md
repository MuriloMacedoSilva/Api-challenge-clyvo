# CLYVO — exportacao Oracle 19c para MongoDB (FIAP Sprint 4)

Projecao academica **somente leitura na origem**: cria snapshots operacionais resumidos. O Java e o microsservico .NET nao consultam estas collections. Voltar ao [indice Database](../README.md).

```text
Oracle 19c (14 tabelas operacionais)
  -> SQL/JSON RETURNING CLOB -> spool base64 enquadrado
  -> exports.py -> JSON array + JSONL validado
  -> mongoimport --mode upsert -> MongoDB (3 collections)
```

## Contratos (schemaVersion 1)

Campos comuns: `_id` estavel (`oracle:<tipo>:<ID>`), `schemaVersion: 1`, `sourceSystem: "CLYVO_ORACLE_19C"`, `exportedAt` ISO 8601 **UTC**. IDs Oracle `NUMBER(19,0)` sao **strings decimais** (sem perda de precisao em consumidores JSON/JavaScript); contadores e idade sao numeros inteiros; datas de dose sao `YYYY-MM-DD` ou `null`. `scheduledAtLocal` e timestamp sem timezone, espelhando o `TIMESTAMP(6)` Oracle: nao converter automaticamente para UTC.

| Collection | Chave de origem | Conteudo | Origem |
|---|---|---|---|
| `animal_profiles` | `animalId` | `tutorId`, `species`, `ageYears`, `vaccinations[]` com `vaccinationId`, `vaccineName`, `applicationDate`, `nextDoseDate` | `ANIMAL` + subconsulta correlacionada de `TB_VACCINATIONS` por `animal_id` |
| `tutor_summaries` | `tutorId` | `animalCount`, `vaccinationsAttentionCount`, `pendingAppointmentsCount`, `animals[]` com `animalId`, `species` | `TUTOR`, subconsultas por `ANIMAL`/`TB_VACCINATIONS`/`TB_APPOINTMENTS` |
| `veterinary_appointments` | `appointmentId` | `animalId`, `tutorId`, `veterinarianId`, `scheduledAtLocal`, `status` | FKs de `TB_APPOINTMENTS` |

Os arrays vazios sao `[]`, nao `null`; `nextDoseDate` sem valor e `null`. `vaccinationsAttentionCount` conta **registros** com `next_dose_date <= TRUNC(SYSDATE)` (nao cobertura/atraso clinico). `pendingAppointmentsCount` conta status `PENDING` sem filtro de periodo. O motivo da consulta e excluido. Cada documento pai e emitido uma vez: agregacoes correlacionadas evitam a multiplicacao de linhas causada por JOIN entre vacinas e consultas. Os arquivos `examples/*.json` usam apenas IDs e valores ficticios.

## Exportar (manual, Oracle institucional)

Pre-condicoes: autorizacao para SELECT nas tabelas operacionais; cliente SQL*Plus/SQLcl executando scripts `@`, privilegio de executar `DBMS_OUTPUT`, `DBMS_LOB`, `UTL_I18N` e `UTL_ENCODE`; diretoria local de saida ja criada e gravavel; sessao de leitura sem DML pendente. **Nao executar `script_bd_oracle.sql` ou outros scripts de instalacao.** Os tres arquivos de exportacao nao contem DDL, DML, COMMIT nem transacao autonoma.

1. Criar uma pasta privada fora do repositorio em `/tmp/clyvo-export-privado`, sem espacos no caminho; proteger acesso aos spools e arquivos finais. Abrir SQL*Plus/SQLcl no schema autorizado e trabalhar no diretorio `Java/api/database/oracle-mongo` (a partir da raiz do workspace). Os comandos abaixo pressupõem esse diretorio corrente e uma sessao SQL conectada. **Conferir o `SPOOL` nos tres SQL antes da execucao:** no estado atual do workspace, `export_animal_profiles.sql` usa `SPOOL &1`, mas `export_tutor_summaries.sql` e `export_veterinary_appointments.sql` usam caminhos fixos em `/tmp/clyvo-export-privado`, independentemente de argumentos. Os dois caminhos fixos requerem esse diretorio e devem ser revisados para outro destino antes de qualquer execucao autorizada. Nao versionar `SPOOL.lst` ou arquivos gerados.
2. Executar, um por vez, com o diretorio previsto nos scripts atuais:

```sql
@export_animal_profiles.sql /tmp/clyvo-export-privado/animal_profiles.b64
@export_tutor_summaries.sql
@export_veterinary_appointments.sql
```

3. De volta ao terminal **local**, no mesmo diretorio dos scripts, converter e validar:

```bash
python3 exports.py convert --input-dir /tmp/clyvo-export-privado --output-dir /tmp/clyvo-export-privado
python3 exports.py validate --input-dir /tmp/clyvo-export-privado
```

O spool contem apenas `DOC_BEGIN`, linhas base64 e `DOC_END`, nao JSON diretamente importavel. Cada chunk codifica ate 4000 caracteres Oracle em AL32UTF8; `JSON_OBJECT`/`JSON_ARRAYAGG RETURNING CLOB` suportam documento maior que o limite de uma linha do SQL*Plus. O conversor rejeita saida incompleta, invalida ou campos fora do contrato. Conferir as contagens esperadas com `SELECT COUNT(*)` somente leitura em `ANIMAL`, `TUTOR` e `TB_APPOINTMENTS` e revisar os arquivos antes de compartilhar/importar. Tabela sem linhas produz `.json` com `[]` e `.jsonl` vazio, ambos validos. Falha de spool/conversao: nao importar arquivo parcial; revisar o erro e exportar novamente em diretoria limpa, sem apagar collections.

## Importar (manual, MongoDB 7 local)

Requer MongoDB 7 iniciado localmente (na validacao manual, via Docker), Python 3, `mongosh`, `mongoimport` (MongoDB Database Tools), URI e banco definidos **no ambiente da sessao**, sem arquivo de credenciais versionado. O container/porta local devem estar acessiveis antes dos comandos; `import.sh` nao inicia MongoDB. Exemplo de URI local sem credenciais, no diretorio `Java/api/database/oracle-mongo`:

```bash
export MONGODB_URI='mongodb://127.0.0.1:27017'
export MONGODB_DB='clyvo_exports'
python3 exports.py validate --input-dir /tmp/clyvo-export-privado
bash import.sh /tmp/clyvo-export-privado
```

`import.sh` valida contratos e pares JSON/JSONL, cria indices por `indexes.mongosh.js` **antes** de escrever e executa `mongoimport --type json --mode upsert --upsertFields _id` para cada collection nao vazia. `_id` vem do tipo + ID Oracle, de modo que reimportar o mesmo snapshot substitui o documento sem duplicar; os indices unicos `animalId`, `tutorId` e `appointmentId` sao uma protecao adicional. Indices para `tutorId`, `veterinarianId + scheduledAtLocal` e `animalId` apoiam consultas. `counts.mongosh.js` imprime as contagens apos a importacao. Nao ha DROP, remocao automatica ou sincronizacao de deletes: documentos ausentes no novo snapshot **permanecem**, inclusive quando o JSONL esta vazio. Os tres imports nao sao atomicos entre si; se algum falhar, corrigir a causa e reexecutar o mesmo snapshot.

`*.json` e **array JSON unico** (`[]` quando vazio); para importacao manual, use `mongoimport --jsonArray --file arquivo.json --mode upsert --upsertFields _id`. `*.jsonl` e **um objeto por linha** (zero linhas quando vazio), importado pelo script sem `--jsonArray`. Nao misturar os formatos. Comparar as contagens impressas com a validacao local e com o total Oracle no momento da exportacao; numa collection ja populada, as contagens Mongo podem ser maiores devido a documentos antigos.

## Evidencia manual e verificacao offline

O usuario confirmou tres arquivos JSON exportados e validados, um documento em cada collection na primeira importacao sem falhas e reimportacao sem duplicacao usando MongoDB 7 em Docker local. Sao resultados manuais informados, nao execucoes desta revisao. `clyvo_exports` e separado do banco `clyvo_ai` do microsservico .NET.

Validacao de fixtures sem Oracle nem MongoDB (a partir deste diretorio):

```bash
python3 exports.py validate --input-dir examples
python3 -m unittest discover -s . -p 'test_exports.py'
bash -n import.sh
```

Testes usam apenas fixtures ficticios: JSON valido, campos/IDs, rejeicao de campos sensiveis, chaves duplicadas, simulacao de dois upserts por `_id`, documento Unicode longo particionado em blocos base64, spool vazio e erros de spool. Nao executam SQL nem acessam rede. A importacao manual confirmada acima nao foi repetida nesta revisao.

## Privacidade, limites e proximos passos

- Sem senha, CPF, email, telefone, token, CNPJ, CRMV, nome de Tutor/Animal/Veterinario, `Animal.history`, observacoes, diagnostico, motivo da consulta ou textos clinicos completos. `vaccineName` e um campo livre: revisar se a massa de origem nao contem dados pessoais inseridos indevidamente nesse campo antes de distribuir arquivos.
- A saida e snapshot operacional, nao origem transacional, auditoria, prontuario clinico nem substituto de autorizacao Java. O .NET nao consulta essas collections; sua persistencia opcional de sessoes/interacoes e suas metricas usam bancos independentes.
- Consultas separadas nao garantem snapshot unico entre as tres collections se houver escritas concorrentes. Planejar janela estavel e reconciliar as contagens; exportacoes repetidas atualizam `exportedAt`.
- MongoDB possui limite de 16 MiB **por documento**; `RETURNING CLOB` supera limites de VARCHAR2/linha SQL*Plus, mas nao esse limite. Spool via `DBMS_OUTPUT` e conversao exigem memoria proporcional aos dados; nao distribuir fixtures reais nem versionar os arquivos gerados.
