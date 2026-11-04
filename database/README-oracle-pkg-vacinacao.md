# PKG_CLYVO_VACINACAO

## Estado

Os scripts desta pasta definem o package read-only `PKG_CLYVO_VACINACAO` para Oracle Database 19c. O usuario confirmou a execucao manual da specification e do body no Oracle institucional: `USER_OBJECTS` retornou `PACKAGE` e `PACKAGE BODY` com status `VALID`, e `USER_ERRORS` nao retornou erros.

O backend Java integra `PR_HISTORICO_ANIMAL` por `SimpleJdbcCall`. O usuario confirmou posteriormente um teste manual bem-sucedido do historico vacinal com o profile `oracle`; os testes locais com H2/mocks continuam sem substituir validacoes mais amplas de concorrencia, volume e falhas reais do driver.

Arquivos:

- `pkg_clyvo_vacinacao_spec.sql`: specification publica;
- `pkg_clyvo_vacinacao_body.sql`: implementacao;
- `validate_pkg_clyvo_vacinacao.sql`: verificacao somente leitura posterior a compilacao.

O package nao cria tabelas, nao modifica as 14 tabelas operacionais, nao executa DML e nao contem `COMMIT`, `ROLLBACK`, SQL dinamico ou trigger. `PKG_CLYVO_RELATORIOS` e a integracao Java das outras tres operacoes permanecem fora desta fase.

## Responsabilidade

O package consulta o historico longitudinal de vacinacao e a agenda objetiva de datas previstas. Ele usa somente:

- `TUTOR`;
- `ANIMAL`;
- `VETERINARIAN`;
- `TB_VACCINATIONS`.

Propriedade do Animal, vinculo `ACCEPTED`, autoria e identidade do ator continuam sendo regras da aplicacao Java. O package recebe somente IDs internos e nunca aceita CPF ou CRMV como prova de autorizacao.

## Contratos publicos

### `FN_SITUACAO_DATA_DOSE`

```sql
FUNCTION fn_situacao_data_dose(
    p_next_dose_date  IN DATE,
    p_data_referencia IN DATE
) RETURN VARCHAR2;
```

Retornos:

| Retorno | Semantica objetiva |
|---|---|
| `SEM_DATA` | Proxima dose nao informada |
| `PREVISTA_VENCIDA` | Data anterior ao dia de referencia |
| `PREVISTA_HOJE` | Mesmo dia da referencia |
| `PREVISTA_FUTURA` | Data posterior ao dia de referencia |

A funcao compara `TRUNC(DATE)`. Os codigos nao afirmam cobertura, atraso clinico ou ausencia de uma aplicacao posterior da mesma vacina.

### `FN_QTD_ATENCOES_TUTOR`

```sql
FUNCTION fn_qtd_atencoes_tutor(
    p_tutor_id        IN NUMBER,
    p_data_referencia IN DATE
) RETURN NUMBER;
```

Conta registros de vacinacao dos Animals do Tutor com `NEXT_DOSE_DATE` informada e prevista ate o fim do dia de referencia. Essa e a mesma semantica objetiva de `vaccinationsAttention` no dashboard atual.

### `PR_HISTORICO_ANIMAL`

```sql
PROCEDURE pr_historico_animal(
    p_animal_id IN NUMBER,
    p_resultado OUT SYS_REFCURSOR
);
```

Valida a existencia do Animal e abre o cursor mesmo quando nao ha vacinacoes. Historico vazio nao e erro. A ordenacao e:

```text
APPLICATION_DATE DESC, ID DESC
```

`ID` e apenas um desempate deterministico; a ordem principal permanece igual ao repository Java.

### `PR_AGENDA_DOSES_TUTOR`

```sql
PROCEDURE pr_agenda_doses_tutor(
    p_tutor_id        IN NUMBER,
    p_data_referencia IN DATE,
    p_situacao        IN VARCHAR2,
    p_resultado       OUT SYS_REFCURSOR
);
```

Filtros aceitos, sem diferenca entre maiusculas e minusculas:

- `VENCIDA`;
- `HOJE`;
- `FUTURA`;
- `TODAS`.

A agenda inclui somente linhas com `NEXT_DOSE_DATE` informada. A ordenacao e:

```text
NEXT_DOSE_DATE ASC, ANIMAL_NAME ASC, ID ASC
```

## Contrato dos cursors

As duas procedures retornam as mesmas colunas e tipos, nesta ordem:

| # | Alias | Tipo Oracle | Destino Java |
|---:|---|---|---|
| 1 | `ID` | `NUMBER(19,0)` | `VaccinationResponseDTO.id` |
| 2 | `ANIMAL_ID` | `NUMBER(19,0)` | `animalId` |
| 3 | `ANIMAL_NAME` | `VARCHAR2(150 CHAR)` | `animalName` |
| 4 | `VETERINARIAN_CPF` | `VARCHAR2(11 CHAR)` | `veterinarianCpf` |
| 5 | `VETERINARIAN_NAME` | `VARCHAR2(200 CHAR)` | `veterinarianName` |
| 6 | `VETERINARIAN_CRMV_NUMBER` | `VARCHAR2(6 CHAR)` | `veterinarianCrmvNumber` |
| 7 | `VETERINARIAN_CRMV_STATE` | `VARCHAR2(2 CHAR)` | `veterinarianCrmvState` |
| 8 | `VACCINE_NAME` | `VARCHAR2(200 CHAR)` | `vaccineName` |
| 9 | `APPLICATION_DATE` | `DATE` | `applicationDate` |
| 10 | `NEXT_DOSE_DATE` | `DATE` | `nextDoseDate` |
| 11 | `BATCH_NUMBER` | `VARCHAR2(100 CHAR)` | `batchNumber` |
| 12 | `MANUFACTURER` | `VARCHAR2(200 CHAR)` | `manufacturer` |
| 13 | `OBSERVATIONS` | `VARCHAR2(4000 CHAR)` | `observations` |
| 14 | `CREATED_AT` | `TIMESTAMP(6)` | `createdAt` |
| 15 | `UPDATED_AT` | `TIMESTAMP(6)` | `updatedAt` |
| 16 | `DOSE_DATE_STATUS` | `VARCHAR2(20)` | Campo derivado opcional; nao altera o DTO atual |

O cursor nao retorna senha, e-mail, telefone, CNPJ, Tutor completo, Veterinarian completo ou `Animal.history`.

## Codigos de erro

| Codigo | Constante | Significado |
|---:|---|---|
| `-20001` | `C_ERR_PARAMETRO_INVALIDO` | ID nulo/nao positivo/nao inteiro ou data de referencia nula |
| `-20002` | `C_ERR_TUTOR_INEXISTENTE` | Tutor nao encontrado pelo ID |
| `-20003` | `C_ERR_ANIMAL_INEXISTENTE` | Animal nao encontrado pelo ID |
| `-20004` | `C_ERR_FILTRO_INVALIDO` | Situacao diferente das quatro opcoes publicas |

Erros inesperados nao sao convertidos em resultados falsos. O reader Java traduz `-20001` e `-20004` para `400`, `-20002` e `-20003` para `404`, e demais falhas para `500` com mensagem segura. SQL, endereco JDBC e mensagens internas Oracle nao sao expostos.

## Pre-verificacao manual

Executar somente na conta proprietaria e em janela autorizada:

```sql
SELECT object_name, object_type, status
  FROM user_objects
 WHERE object_name = 'PKG_CLYVO_VACINACAO'
 ORDER BY object_type;

SELECT privilege
  FROM session_privs
 WHERE privilege = 'CREATE PROCEDURE';

SELECT table_name
  FROM user_tables
 WHERE table_name IN ('TUTOR', 'ANIMAL', 'VETERINARIAN', 'TB_VACCINATIONS')
 ORDER BY table_name;
```

Se o package ja existir, interromper e comparar `USER_SOURCE` com os arquivos versionados antes de usar `CREATE OR REPLACE`. Nao substituir um objeto desconhecido silenciosamente.

## Instalacao manual segura

Oracle confirma DDL implicitamente; a instalacao nao possui rollback atomico. Depois das pre-verificacoes:

```sql
@pkg_clyvo_vacinacao_spec.sql
@pkg_clyvo_vacinacao_body.sql
```

Nao executar `script_bd_oracle.sql` novamente. Os scripts do package dependem das quatro tabelas ja existentes e nao alteram sua estrutura ou seus dados.

## Validacao de compilacao

```sql
SELECT object_name, object_type, status, last_ddl_time
  FROM user_objects
 WHERE object_name = 'PKG_CLYVO_VACINACAO'
 ORDER BY object_type;

SELECT name, type, line, position, text
  FROM user_errors
 WHERE name = 'PKG_CLYVO_VACINACAO'
 ORDER BY type, sequence;
```

Resultado esperado: duas linhas `VALID`, uma `PACKAGE` e uma `PACKAGE BODY`, e zero linhas em `USER_ERRORS`.

Depois da compilacao, o script abaixo valida funcoes, erros, cursors e ordenacao usando somente leitura:

```sql
@validate_pkg_clyvo_vacinacao.sql
```

O script detecta automaticamente IDs existentes quando a massa permitir. Casos que exigem Animal com ou sem vacinacao sao informados como ignorados quando o schema nao possui a pre-condicao correspondente.

## Exemplos manuais

As datas sao explicitas para produzir resultados reproduziveis:

```sql
SELECT pkg_clyvo_vacinacao.fn_situacao_data_dose(
           DATE '2026-11-04',
           DATE '2026-11-04'
       ) AS situacao
  FROM dual;
```

```sql
SELECT pkg_clyvo_vacinacao.fn_qtd_atencoes_tutor(
           1,
           DATE '2026-11-04'
       ) AS atencoes
  FROM dual;
```

No SQL*Plus/SQLcl:

```sql
VARIABLE historico REFCURSOR;
EXEC pkg_clyvo_vacinacao.pr_historico_animal(1, :historico);
PRINT historico;
```

```sql
VARIABLE agenda REFCURSOR;
EXEC pkg_clyvo_vacinacao.pr_agenda_doses_tutor(1, DATE '2026-11-04', 'TODAS', :agenda);
PRINT agenda;
```

Substituir os IDs somente por IDs previamente conferidos com consultas read-only. Os exemplos nao devem ser executados em ambiente nao autorizado.

## Integracao Java

`VaccinationService` mantem as validacoes de Tutor, Animal, propriedade e vinculo `ACCEPTED` antes de delegar a leitura para `VaccinationHistoryReader`.

Selecao por profile:

```text
local/prod/render/test
-> JpaVaccinationHistoryReader
-> VaccinationRepository

oracle
-> OracleVaccinationHistoryReader
-> SimpleJdbcCall
-> PKG_CLYVO_VACINACAO.PR_HISTORICO_ANIMAL
```

O `SimpleJdbcCall` usa `withCatalogName("PKG_CLYVO_VACINACAO")`, `withProcedureName("PR_HISTORICO_ANIMAL")` e desabilita descoberta de metadata. Os parametros sao declarados explicitamente:

| Parametro | Direcao | JDBC |
|---|---|---|
| `P_ANIMAL_ID` | IN | `Types.NUMERIC` |
| `P_RESULTADO` | OUT | `Types.REF_CURSOR` |

O `RowMapper` consome o cursor dentro do `@Transactional(readOnly = true)` do Service. `NUMBER(19,0)` vira `Long`, `DATE` vira `LocalDate`, `TIMESTAMP(6)` vira `LocalDateTime` e colunas opcionais preservam `null`. Os campos 1 a 15 constroem o `VaccinationResponseDTO`; `DOSE_DATE_STATUS`, coluna 16, e deliberadamente ignorado para nao alterar o JSON publico.

O reader JPA usa `APPLICATION_DATE DESC, ID DESC`, igual ao package. Historico vazio continua retornando `200 []`. Controllers, endpoints e DTOs nao foram duplicados.

## Validacao manual da chamada Java

Esta etapa somente deve ser repetida pelo usuario em ambiente autorizado. O usuario informou que a chamada foi validada manualmente com sucesso depois da implementacao.

1. Confirme novamente `PACKAGE` e `PACKAGE BODY` como `VALID` e `USER_ERRORS` vazio.
2. Identifique por consultas read-only um `animalId` e o CPF de seu Tutor, sem criar massa nova.
3. Inicie a API com o profile `oracle` e as variaveis externas existentes. Para evidenciar a chamada sem imprimir payloads, habilite temporariamente `logging.level.org.springframework.jdbc.core=DEBUG` apenas nessa execucao.
4. Execute o endpoint existente:

```bash
curl -i "http://localhost:8080/v1/vaccinations/tutor/<TUTOR_CPF>/animals/<ANIMAL_ID>"
```

5. Confirme `200` com o mesmo JSON anterior, ou `[]` para Animal sem vacinacoes, e procure no log JDBC a chamada a `PKG_CLYVO_VACINACAO.PR_HISTORICO_ANIMAL`.
6. Repita com um Tutor sem propriedade ou Veterinario sem vinculo aceito e confirme `403`; a procedure nao deve ser chamada nesses casos.
7. Desabilite o log DEBUG depois da verificacao. Nao registrar URL, usuario, senha ou resposta com dados reais no repositorio.

Uma resposta HTTP isolada prova o contrato, mas o log JDBC e necessario para distinguir a nova rota Oracle da leitura JPA anterior. Falhas devem ser preservadas para diagnostico local seguro, sem publicar stack trace ou credenciais.

## Reversao sem schema

Para reverter a integracao, publique o JAR anterior ou reverta `VaccinationHistoryReader`, suas implementacoes e a delegacao no `VaccinationService`. Nenhuma alteracao no schema e necessaria: o package pode permanecer compilado e sem consumidores. Nao executar `DROP PACKAGE` como parte do rollback da aplicacao.

## Limitacoes

- A compilacao PL/SQL e uma chamada manual via JDBC/profile `oracle` foram confirmadas pelo usuario.
- H2 e mocks nao validam o registro real de `Types.REF_CURSOR`, o fechamento do cursor pelo driver ou a participacao da conexao Oracle na transacao Spring.
- `PR_HISTORICO_ANIMAL` usa `TRUNC(SYSDATE)` apenas para a coluna derivada `DOSE_DATE_STATUS`; os 15 campos do DTO nao dependem dessa classificacao.
- `PR_AGENDA_DOSES_TUTOR` usa a data fornecida pelo chamador e nao persiste status.
- Contar uma data prevista passada nao prova atraso clinico ou ausencia de dose posterior.
- O package nao autoriza acesso e nao substitui autenticacao.
- Apenas `PR_HISTORICO_ANIMAL` esta integrada ao Java; functions e agenda continuam sem consumidores Java.
