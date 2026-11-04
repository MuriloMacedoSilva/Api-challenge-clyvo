# PKG_CLYVO_RELATORIOS

## Estado

Os scripts desta pasta definem `PKG_CLYVO_RELATORIOS` para Oracle Database 19c. O usuario confirmou a execucao manual da specification, do body e do validador no Oracle institucional: `PACKAGE` e `PACKAGE BODY` ficaram `VALID`, sem linhas em `USER_ERRORS`, e as verificacoes somente leitura foram concluidas com sucesso.

O backend Java usa `DashboardMetricsReader`: `JpaDashboardMetricsReader` permanece ativo em `local`, `prod`, `render` e `test`, enquanto `OracleDashboardMetricsReader` chama somente `PR_INDICADORES_TUTOR` e `PR_INDICADORES_VETERINARIO` no profile `oracle`. O usuario confirmou posteriormente testes manuais bem-sucedidos dos dashboards no profile `oracle`. As procedures de consultas por periodo continuam sem consumidor Java.

Arquivos:

- `pkg_clyvo_relatorios_spec.sql`: API publica e codigos de erro;
- `pkg_clyvo_relatorios_body.sql`: consultas somente leitura;
- `validate_pkg_clyvo_relatorios.sql`: verificacao manual somente leitura;
- `README-oracle-pkg-relatorios.md`: contratos, instalacao, validacao e limites.

O relatorio da Fase 3A nao foi localizado no workspace. As definicoes foram confrontadas diretamente com `TutorDashboardService`, `VeterinarianDashboardService`, repositories, DTOs, Entities e `script_bd_oracle.sql`.

## Objetivo e limites

O package fornece somente:

- tres contagens publicas reutilizaveis;
- os contadores atuais do dashboard do Tutor;
- os contadores atuais do dashboard do Veterinario;
- consultas de Appointment por periodo para Tutor e Veterinario.

Ele nao monta os DTOs completos dos dashboards. Proxima consulta, serie mensal, distribuicao por especie, atividades recentes e demais listas continuam fora deste contrato.

O package nao executa DML, nao possui `COMMIT`, `ROLLBACK`, autonomous transaction, SQL dinamico ou trigger. Ele nao cria tabelas e nao altera as 14 tabelas operacionais. Os CPFs retornados nos cursores sao dados de apresentacao, nunca prova de autenticacao ou autorizacao. CRMV nao faz parte do cursor atual.

## Fontes operacionais

| Tabela | Uso |
|---|---|
| `TUTOR` | Existencia do Tutor e campos resumidos do cursor |
| `VETERINARIAN` | Existencia do Veterinario e campos resumidos do cursor |
| `ANIMAL` | Propriedade, pacientes da carteira e campos do cursor |
| `TB_VETERINARIAN_TUTOR_LINKS` | Vinculos atuais `ACCEPTED` |
| `TB_APPOINTMENTS` | Contadores e consultas por periodo |
| `TB_MEDICAL_RECORDS` | Responsabilidade clinica e propriedade para exames |
| `TB_EXAMS` | Exames `REQUESTED` |

A atencao vacinal nao duplica SQL: `PR_INDICADORES_TUTOR` chama `PKG_CLYVO_VACINACAO.FN_QTD_ATENCOES_TUTOR`, que consulta `TB_VACCINATIONS` e `ANIMAL`.

`TB_FATO_ATENDIMENTO`, `TB_AUDITORIA_VACINACAO` e as tabelas academicas legadas nao fazem parte deste package.

## Contratos publicos

### Functions

```sql
FUNCTION fn_qtd_animais_tutor(
    p_tutor_id IN NUMBER
) RETURN NUMBER;
```

Conta linhas de `ANIMAL` cujo `TUTOR_ID` corresponde ao Tutor informado.

```sql
FUNCTION fn_qtd_pacientes_veterinario(
    p_veterinarian_id IN NUMBER
) RETURN NUMBER;
```

Conta cada Animal uma vez quando existe vinculo atual `ACCEPTED` entre seu Tutor e o Veterinario. A consulta usa `EXISTS`, igual a semantica de `AnimalRepository.countByAcceptedVeterinarianLink`, evitando multiplicacao por joins.

```sql
FUNCTION fn_qtd_exames_pendentes_veterinario(
    p_veterinarian_id IN NUMBER
) RETURN NUMBER;
```

Conta `TB_EXAMS.STATUS = 'REQUESTED'` pela cadeia `TB_EXAMS -> TB_MEDICAL_RECORDS.VETERINARIAN_ID`. Nao usa a carteira atual nem presume que exames de Animals vinculados pertencam ao mesmo profissional.

As functions retornam zero para um ator existente sem registros. ID invalido ou ator inexistente gera erro documentado.

### Indicadores do Tutor

```sql
PROCEDURE pr_indicadores_tutor(
    p_tutor_id                 IN NUMBER,
    p_instante_referencia      IN TIMESTAMP,
    p_total_pets               OUT NUMBER,
    p_consultas_hoje           OUT NUMBER,
    p_consultas_pendentes      OUT NUMBER,
    p_veterinarios_vinculados  OUT NUMBER,
    p_exames_pendentes         OUT NUMBER,
    p_atencoes_vacinais        OUT NUMBER
);
```

| Saida | Semantica |
|---|---|
| `P_TOTAL_PETS` | Animals do Tutor |
| `P_CONSULTAS_HOJE` | Appointments do Tutor no dia local da referencia, exceto `CANCELLED` |
| `P_CONSULTAS_PENDENTES` | Todos os Appointments `PENDING` do Tutor, sem filtro de data |
| `P_VETERINARIOS_VINCULADOS` | Links do Tutor com status `ACCEPTED` |
| `P_EXAMES_PENDENTES` | Exams `REQUESTED` de MedicalRecords cujos Animals pertencem ao Tutor |
| `P_ATENCOES_VACINAIS` | Resultado de `PKG_CLYVO_VACINACAO.FN_QTD_ATENCOES_TUTOR` para a data da referencia |

### Indicadores do Veterinario

```sql
PROCEDURE pr_indicadores_veterinario(
    p_veterinarian_id       IN NUMBER,
    p_instante_referencia   IN TIMESTAMP,
    p_tutores_vinculados    OUT NUMBER,
    p_pacientes_ativos      OUT NUMBER,
    p_consultas_hoje        OUT NUMBER,
    p_consultas_pendentes   OUT NUMBER,
    p_consultas_confirmadas OUT NUMBER,
    p_consultas_concluidas  OUT NUMBER,
    p_exames_pendentes      OUT NUMBER
);
```

| Saida | Semantica |
|---|---|
| `P_TUTORES_VINCULADOS` | Links atuais do Veterinario com status `ACCEPTED` |
| `P_PACIENTES_ATIVOS` | Animals dos Tutores atualmente vinculados por `ACCEPTED` |
| `P_CONSULTAS_HOJE` | Appointments atribuidos ao Veterinario no dia da referencia, exceto `CANCELLED` |
| `P_CONSULTAS_PENDENTES` | Todos os Appointments atribuidos com status `PENDING` |
| `P_CONSULTAS_CONFIRMADAS` | Todos os Appointments atribuidos com status `CONFIRMED` |
| `P_CONSULTAS_CONCLUIDAS` | Todos os Appointments atribuidos com status `COMPLETED` |
| `P_EXAMES_PENDENTES` | Exams `REQUESTED` de MedicalRecords atribuidos ao Veterinario |

Carteira atual e atividade clinica possuem escopos diferentes. Somente Tutores vinculados e pacientes ativos dependem do link atual `ACCEPTED`. Consultas e exames usam a FK historica de responsabilidade do Veterinario, mesmo se o link mudar depois.

## Datas e periodos

`P_INSTANTE_REFERENCIA`, `P_INICIO` e `P_FIM` sao `TIMESTAMP` sem timezone, coerentes com `LocalDateTime` e `TB_APPOINTMENTS.SCHEDULED_AT TIMESTAMP(6)`.

Consultas do dia usam:

```text
[00:00:00 do dia da referencia, 00:00:00 do dia seguinte)
```

Os cursores usam o intervalo semiaberto:

```text
SCHEDULED_AT >= P_INICIO AND SCHEDULED_AT < P_FIM
```

O package nao substitui o instante recebido por `SYSDATE` ou `SYSTIMESTAMP`. A aplicacao futura devera enviar o mesmo instante usado como `generatedAt`. Como o modelo atual nao persiste timezone, JVM e Oracle devem continuar alinhados ao horario local assumido pelo projeto.

## Cursores de Appointment

```sql
PROCEDURE pr_consultas_tutor_periodo(
    p_tutor_id  IN NUMBER,
    p_inicio    IN TIMESTAMP,
    p_fim       IN TIMESTAMP,
    p_resultado OUT SYS_REFCURSOR
);

PROCEDURE pr_consultas_veterinario_periodo(
    p_veterinarian_id IN NUMBER,
    p_inicio          IN TIMESTAMP,
    p_fim             IN TIMESTAMP,
    p_resultado       OUT SYS_REFCURSOR
);
```

As procedures retornam todos os status reais no periodo, sem impor link atual ou filtro de autorizacao. A primeira filtra `TB_APPOINTMENTS.TUTOR_ID`; a segunda filtra `TB_APPOINTMENTS.VETERINARIAN_ID`.

Contrato posicional compartilhado:

| # | Alias | Tipo Oracle | Nulo | Destino existente |
|---:|---|---|---|---|
| 1 | `APPOINTMENT_ID` | `NUMBER(19,0)` | Nao | `AppointmentResponseDTO.id` |
| 2 | `SCHEDULED_AT` | `TIMESTAMP(6)` | Nao | `scheduledAt` |
| 3 | `REASON` | `VARCHAR2(1000 CHAR)` | Nao | `reason` |
| 4 | `STATUS` | `VARCHAR2(255 CHAR)` | Nao | `AppointmentStatus` |
| 5 | `CREATED_AT` | `TIMESTAMP(6)` | Nao | `createdAt` |
| 6 | `UPDATED_AT` | `TIMESTAMP(6)` | Sim | `updatedAt` |
| 7 | `ANIMAL_ID` | `NUMBER(19,0)` | Nao | `animalId` |
| 8 | `ANIMAL_NAME` | `VARCHAR2(150 CHAR)` | Nao | `animalName` |
| 9 | `TUTOR_CPF` | `VARCHAR2(11 CHAR)` | Nao | `tutorCpf` |
| 10 | `TUTOR_NAME` | `VARCHAR2(200 CHAR)` | Nao | `tutorName` |
| 11 | `VETERINARIAN_CPF` | `VARCHAR2(11 CHAR)` | Nao | `veterinarianCpf` |
| 12 | `VETERINARIAN_NAME` | `VARCHAR2(200 CHAR)` | Nao | `veterinarianName` |

Ordenacao:

```text
SCHEDULED_AT ASC, APPOINTMENT_ID ASC
```

Ator e intervalo validos sem consultas retornam cursor vazio, nao erro. Senha, telefone, e-mail, CNPJ, CRMV, `Animal.history` e grafos completos nao sao retornados.

## Codigos de erro

| Codigo | Constante | Condicao |
|---:|---|---|
| `-20021` | `C_ERR_PARAMETRO_INVALIDO` | ID nulo, nao positivo ou fracionario |
| `-20022` | `C_ERR_PARAMETRO_OBRIGATORIO` | Instante de referencia ou limite do periodo nulo |
| `-20023` | `C_ERR_TUTOR_INEXISTENTE` | Tutor nao encontrado pelo ID |
| `-20024` | `C_ERR_VETERINARIO_INEXISTENTE` | Veterinario nao encontrado pelo ID |
| `-20025` | `C_ERR_INTERVALO_INVALIDO` | Inicio maior ou igual ao fim |

Falhas inesperadas nao sao capturadas nem convertidas em zero ou cursor vazio. O reader Java traduz `-20021`, `-20022` e `-20025` para `400`, `-20023` e `-20024` para `404`, e oculta detalhes JDBC/Oracle nas demais falhas com resposta `500` segura.

## Dependencia entre packages

O body depende de `PKG_CLYVO_VACINACAO.FN_QTD_ATENCOES_TUTOR`. A ordem segura e:

1. Confirmar `PKG_CLYVO_VACINACAO` specification/body como `VALID` e sem `USER_ERRORS`.
2. Compilar `pkg_clyvo_relatorios_spec.sql`.
3. Compilar `pkg_clyvo_relatorios_body.sql`.
4. Confirmar ambos os objetos de relatorios como `VALID` e sem `USER_ERRORS`.
5. Executar manualmente `validate_pkg_clyvo_relatorios.sql` somente apos autorizacao.

No mesmo schema proprietario, nenhuma grant adicional entre packages e tabelas e necessaria. Em schemas diferentes, privilegios de objetos usados por PL/SQL devem ser concedidos diretamente ao owner; privilegios recebidos apenas por role nao bastam para compilacao. Consumidores externos precisam de `EXECUTE` no package.

Substituir a specification de `PKG_CLYVO_VACINACAO` pode invalidar o body de relatorios. Depois de qualquer recompilacao da dependencia, consultar `USER_OBJECTS`, `USER_ERRORS` e `USER_DEPENDENCIES` e recompilar o body de relatorios quando necessario.

O profile Oracle chama as duas procedures de indicadores quando os endpoints de dashboard sao acessados. O startup continua validando apenas o schema das Entities, portanto um package invalido pode nao impedir a inicializacao e deve ser detectado pela validacao manual ou por uma chamada controlada aos dashboards.

## Pre-verificacao manual

Executar somente no schema autorizado:

```sql
SELECT object_name, object_type, status
  FROM user_objects
 WHERE object_name IN ('PKG_CLYVO_VACINACAO', 'PKG_CLYVO_RELATORIOS')
 ORDER BY object_name, object_type;

SELECT privilege
  FROM session_privs
 WHERE privilege = 'CREATE PROCEDURE';

SELECT table_name
  FROM user_tables
 WHERE table_name IN (
       'TUTOR', 'VETERINARIAN', 'ANIMAL',
       'TB_VETERINARIAN_TUTOR_LINKS', 'TB_APPOINTMENTS',
       'TB_MEDICAL_RECORDS', 'TB_EXAMS', 'TB_VACCINATIONS'
 )
 ORDER BY table_name;
```

Se `PKG_CLYVO_RELATORIOS` ja existir, interromper e comparar `USER_SOURCE` antes de usar `CREATE OR REPLACE`. DDL Oracle possui commit implicito e a instalacao nao tem rollback atomico. Nao executar `script_bd_oracle.sql` novamente.

## Instalacao manual

Depois das pre-verificacoes e somente com autorizacao:

```sql
@pkg_clyvo_relatorios_spec.sql
@pkg_clyvo_relatorios_body.sql
```

Validar compilacao:

```sql
SELECT object_name, object_type, status, last_ddl_time
  FROM user_objects
 WHERE object_name = 'PKG_CLYVO_RELATORIOS'
 ORDER BY object_type;

SELECT name, type, line, position, text
  FROM user_errors
 WHERE name = 'PKG_CLYVO_RELATORIOS'
 ORDER BY type, sequence;
```

Resultado esperado: `PACKAGE` e `PACKAGE BODY` como `VALID`, sem linhas em `USER_ERRORS`.

## Validacao somente leitura

```sql
@validate_pkg_clyvo_relatorios.sql
```

O script:

- verifica objetos e dependencia vacinal e lista erros e assinaturas para conferencia;
- compara functions e indicadores com consultas independentes nas tabelas;
- valida os 12 aliases dos cursores com `DBMS_SQL.DESCRIBE_COLUMNS2`;
- valida escopo, quantidade e ordenacao dos cursores;
- valida cursores vazios;
- testa IDs nulos, zero, fracionarios e inexistentes;
- testa referencia nula, limites nulos e intervalos iguais/invertidos;
- compara a atencao vacinal com `PKG_CLYVO_VACINACAO`;
- procura comandos mutaveis literais no source compilado;
- compara a soma de linhas das tabelas consultadas antes e depois como guarda adicional.

Casos positivos dependentes de massa sao informados como ignorados quando nao existe Tutor ou Veterinario; a mensagem final exige revisar esses avisos e nao os apresenta como cobertura executada. Erros esperados aceitam somente o `SQLCODE` documentado; qualquer outra falha e propagada. O script nao cria massa e nao executa DML ou controle transacional. A busca textual e a soma de linhas sao guardas, nao prova geral contra efeitos indiretos ou updates que preservem contagens; a garantia principal vem da revisao estatica do body e da ausencia de chamadas mutaveis.

## Exemplos

```sql
SELECT pkg_clyvo_relatorios.fn_qtd_animais_tutor(1) AS total_pets
  FROM dual;
```

```sql
VARIABLE total_pets NUMBER;
VARIABLE consultas_hoje NUMBER;
VARIABLE consultas_pendentes NUMBER;
VARIABLE veterinarios_vinculados NUMBER;
VARIABLE exames_pendentes NUMBER;
VARIABLE atencoes_vacinais NUMBER;

BEGIN
    pkg_clyvo_relatorios.pr_indicadores_tutor(
        1,
        TIMESTAMP '2026-11-04 12:00:00',
        :total_pets,
        :consultas_hoje,
        :consultas_pendentes,
        :veterinarios_vinculados,
        :exames_pendentes,
        :atencoes_vacinais
    );
END;
/
```

```sql
VARIABLE resultado REFCURSOR;
BEGIN
    pkg_clyvo_relatorios.pr_consultas_veterinario_periodo(
        1,
        TIMESTAMP '2026-11-01 00:00:00',
        TIMESTAMP '2026-12-01 00:00:00',
        :resultado
    );
END;
/
PRINT resultado;
```

Substituir IDs somente depois de consultas read-only. Nao executar os exemplos em ambiente sem autorizacao.

## Integracao Java

Os contadores usam readers por profile:

```text
local/prod/render/test -> repositories JPA atuais
oracle                 -> SimpleJdbcCall explicito
```

Os Services resolvem o ator por CPF, preservam a transacao read-only e enviam ao reader o ID interno e o mesmo `LocalDateTime` retornado como `generatedAt`. O reader Oracle usa `withCatalogName("PKG_CLYVO_RELATORIOS")`, desabilita descoberta de metadata, declara IDs e OUT counters como `Types.NUMERIC` e o instante como `Types.TIMESTAMP`. OUT ausente, nulo ou nao numerico falha com `500` seguro em vez de produzir zero silenciosamente.

Proxima consulta, atividades recentes, serie mensal e distribuicao por especie continuam em JPA para preservar o contrato REST. As procedures de consultas por periodo nao foram integradas; uma eventual integracao futura de cursor devera declarar `Types.REF_CURSOR` e usar RowMapper compativel com os 12 aliases.

## Limitacoes e riscos

- A compilacao, o validador PL/SQL e chamadas manuais via `SimpleJdbcCall`/profile `oracle` foram confirmados pelo usuario.
- H2 e mocks nao validam tipos JDBC reais, privilegios de execucao ou participacao da conexao Oracle na transacao Spring.
- `TIMESTAMP(6)` e `LocalDateTime` nao carregam timezone.
- CPF nao possui unique constraint no schema, embora seja retornado para apresentacao.
- O banco nao garante que `TB_APPOINTMENTS.TUTOR_ID` corresponda ao Tutor do Animal; os relatorios seguem a mesma FK usada pelos repositories de Appointment.
- O banco nao garante que MedicalRecord repita Animal/Veterinario do Appointment; exames seguem diretamente `TB_MEDICAL_RECORDS`, como os repositories atuais.
- Os contadores sao estado corrente sob `READ COMMITTED`; mudancas concorrentes entre statements podem alterar o snapshot, assim como nas queries JPA atuais.
- A atencao vacinal conta registros com `NEXT_DOSE_DATE` ate a data de referencia; nao prova atraso clinico ou ausencia de dose posterior.
- Recompilar a specification vacinal pode invalidar o body deste package.
