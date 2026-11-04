# Auditoria vacinal Oracle 19c

## Estado

Esta fase prepara localmente a tabela `TB_CLYVO_VACCINATION_AUDIT`, o package `PKG_CLYVO_AUDITORIA`, a trigger `TRG_CV_AUD_VACCINATION` e os roteiros de validacao. Nenhum desses scripts foi executado no Oracle institucional. Os objetos nao devem ser descritos como instalados, compilados, habilitados ou validados antes de uma execucao manual autorizada.

Uma tentativa posterior de compilacao do body encontrou `PLS-00204` pelo uso de `DECODE` diretamente no `IF` de `PR_REGISTER_VACCINATION_EVENT`. O body e o trigger foram corrigidos localmente com funcoes privadas sobrecarregadas para `NUMBER`, `VARCHAR2` e `DATE`, preservando comparacao null-safe, o filtro de UPDATE sem mudanca e `C_ERR_NO_CHANGE`. A compilacao Oracle de ambos ainda precisa ser repetida antes de qualquer instalacao ou validacao.

Nenhum codigo Java, endpoint, Entity, repository ou DTO foi alterado. A auditoria e exclusiva do Oracle e fica fora das 14 tabelas operacionais validadas pelo Hibernate.

Arquivos:

- `create_clyvo_vaccination_audit.sql`: tabela, constraints, indice e comentarios;
- `pkg_clyvo_auditoria_spec.sql`: contrato publico e codigos de erro;
- `pkg_clyvo_auditoria_body.sql`: validacao e persistencia dos eventos;
- `trg_cv_aud_vaccination.sql`: trigger row-level de `TB_VACCINATIONS`;
- `validate_clyvo_vaccination_audit.sql`: validacao estritamente somente leitura;
- `test_clyvo_vaccination_audit_transactional.sql`: ensaio mutavel, marcado e encerrado com rollback.

## Diagnostico

`TB_VACCINATIONS` aceita INSERT, UPDATE e DELETE fisico. O Java controla autoria e vinculo antes das mutacoes, mas, depois de um DELETE, a linha original deixa de existir e nao ha trilha persistente para distinguir criacao, correcao ou exclusao.

O SQL academico antigo possui `TB_AUDITORIA_VACINACAO` e `TRG_AUDITORIA_VACINACAO`, ambos ligados a `TB_VACINACAO`, nao a tabela operacional canonica. A implementacao antiga serializa snapshots em `VARCHAR2(2000)`, omite fabricante, usa sequencia separada e escreve mensagens com `DBMS_OUTPUT`. Ela foi usada apenas como referencia historica e nao e reutilizada nem substituida.

A nova estrutura usa colunas tipadas, preserva OLD/NEW, nao copia observacoes clinicas e deixa falhas propagarem para a transacao principal.

## Modelo da tabela

`TB_CLYVO_VACCINATION_AUDIT` e uma extensao Oracle separada. Ela nao altera `TB_VACCINATIONS` nem qualquer outra tabela operacional.

| Campo | Tipo | Nulo | Semantica |
|---|---|---:|---|
| `ID` | `NUMBER(19,0) IDENTITY` | nao | Identificador do evento |
| `VACCINATION_ID` | `NUMBER(19,0)` | nao | ID historico da vacinacao afetada |
| `OPERATION_TYPE` | `VARCHAR2(6 CHAR)` | nao | `INSERT`, `UPDATE` ou `DELETE` |
| `EVENT_AT` | `TIMESTAMP(6) WITH TIME ZONE` | nao | Instante gerado pelo Oracle com `SYSTIMESTAMP` |
| `OLD_ANIMAL_ID` / `NEW_ANIMAL_ID` | `NUMBER(19,0)` | conforme operacao | Snapshot do Animal |
| `OLD_VETERINARIAN_ID` / `NEW_VETERINARIAN_ID` | `NUMBER(19,0)` | conforme operacao | Snapshot do autor persistido |
| `OLD_VACCINE_NAME` / `NEW_VACCINE_NAME` | `VARCHAR2(200 CHAR)` | conforme operacao | Snapshot do nome da vacina |
| `OLD_APPLICATION_DATE` / `NEW_APPLICATION_DATE` | `DATE` | conforme operacao | Data de aplicacao |
| `OLD_NEXT_DOSE_DATE` / `NEW_NEXT_DOSE_DATE` | `DATE` | sim | Proxima dose informada |
| `OLD_BATCH_NUMBER` / `NEW_BATCH_NUMBER` | `VARCHAR2(100 CHAR)` | sim | Lote |
| `OLD_MANUFACTURER` / `NEW_MANUFACTURER` | `VARCHAR2(200 CHAR)` | sim | Fabricante |
| `OBSERVATIONS_CHANGED` | `NUMBER(1,0)` | nao | `1` somente quando UPDATE alterou observacoes |
| `DATABASE_USER` | `VARCHAR2(128 BYTE)` | nao | `SESSION_USER` tecnico |
| `CLIENT_IDENTIFIER` | `VARCHAR2(64 BYTE)` | sim | Contexto opcional da sessao |

`OBSERVATIONS_CHANGED` preserva a evidencia de que o campo mudou sem copiar seu conteudo clinico. Senha, token, CPF, e-mail, telefone, CNPJ, CRMV e observacoes nao entram na tabela.

As constraints exigem operacao valida, ID positivo inteiro, indicador booleano e lados OLD/NEW coerentes. A PK cria seu indice. `IX_CV_VACC_AUD_VACC_EVENT` suporta a leitura cronologica por `VACCINATION_ID`.

Nao existe FK para `TB_VACCINATIONS`: ela impediria a retencao do evento de DELETE. Tambem nao existem FKs para Animal ou Veterinario, pois exclusoes futuras desses registros nao devem apagar nem bloquear snapshots historicos. Os IDs sao referencias historicas, nao garantias de que o registro relacionado ainda existe.

## Fluxo

```text
Spring/JPA ou DML autorizado
-> INSERT, UPDATE ou DELETE em TB_VACCINATIONS
-> TRG_CV_AUD_VACCINATION AFTER EACH ROW
-> filtro de UPDATE sem mudanca auditavel
-> PKG_CLYVO_AUDITORIA.PR_REGISTER_VACCINATION_EVENT
-> validacao do snapshot
-> INSERT em TB_CLYVO_VACCINATION_AUDIT
-> commit ou rollback da transacao original
```

A trigger nao consulta nem modifica `TB_VACCINATIONS`, portanto nao cria mutating table. Ela nao atua sobre a tabela de auditoria, portanto nao ha recursao. Toda persistencia fica encapsulada no package.

## Campos auditaveis

INSERT e DELETE sempre geram evento. UPDATE gera evento quando pelo menos um destes valores muda de fato, com comparacao null-safe:

- `ANIMAL_ID`;
- `VETERINARIAN_ID`;
- `VACCINE_NAME`;
- `APPLICATION_DATE`;
- `NEXT_DOSE_DATE`;
- `BATCH_NUMBER`;
- `MANUFACTURER`;
- `OBSERVATIONS`, somente como indicador de mudanca.

`CREATED_AT` e `UPDATED_AT` nao sao snapshots de negocio nesta auditoria. O callback `@PreUpdate` da Entity atualiza `UPDATED_AT` em toda atualizacao JPA, mas uma mudanca isolada desse timestamp nao cria evento. Um request que reenvia os mesmos valores tambem nao cria evento.

## Identidade e contexto

`DATABASE_USER` registra `SYS_CONTEXT('USERENV', 'SESSION_USER')`. Esse valor identifica a conta tecnica Oracle usada pelo datasource e nunca deve ser apresentado como Tutor ou Veterinario.

`CLIENT_IDENTIFIER` e armazenado somente quando a sessao o fornece. O backend atual nao configura esse contexto de modo confiavel, portanto o valor normal esperado e `NULL`. A coluna prepara rastreabilidade futura sem inventar uma identidade de aplicacao. `MODULE` e `ACTION` nao foram persistidos porque o datasource atual nao garante valores estaveis ou semanticamente confiaveis.

O Veterinario responsavel continua representado por `OLD_VETERINARIAN_ID`/`NEW_VETERINARIAN_ID`, copiado da propria vacinacao. Isso nao substitui autenticacao real nem prova quem operou a sessao HTTP.

## Package e erros

`PKG_CLYVO_AUDITORIA` usa `AUTHID DEFINER` e expoe `PR_REGISTER_VACCINATION_EVENT`, constantes das tres operacoes e os quatro codigos de erro publicos. Ele valida operacao, ID, coerencia OLD/NEW, indicador de observacoes e UPDATE sem mudanca. Comparacoes internas usam funcoes privadas sobrecarregadas para `NUMBER`, `VARCHAR2` e `DATE`, considerando dois valores nulos iguais e apenas um valor nulo diferente.

| Codigo | Constante | Condicao |
|---:|---|---|
| `-20041` | `C_ERR_OPERATION_INVALID` | Operacao diferente de INSERT/UPDATE/DELETE |
| `-20042` | `C_ERR_VACCINATION_ID` | ID nulo, fracionario ou nao positivo |
| `-20043` | `C_ERR_SNAPSHOT_INVALID` | Snapshot ou indicador incoerente |
| `-20044` | `C_ERR_NO_CHANGE` | Chamada direta de UPDATE sem mudanca auditavel |

O package nao executa `COMMIT`, `ROLLBACK`, autonomous transaction ou SQL dinamico. Falhas inesperadas nao sao capturadas nem convertidas em sucesso; elas propagam para a trigger e para o DML original.

## Semantica transacional e falhas

A auditoria usa a mesma transacao e conexao da operacao em `TB_VACCINATIONS`:

- commit da mutacao confirma o evento;
- rollback da mutacao remove o evento;
- falha ao gravar o evento aborta a mutacao principal;
- nao existe `WHEN OTHERS THEN NULL`;
- nao existe transacao autonoma capaz de deixar auditoria orfa ou confirmar evento de operacao revertida.

Essa politica e fail-closed. Falta de quota, objeto invalido, constraint divergente, lock, indisponibilidade ou qualquer falha no INSERT de auditoria pode fazer os endpoints de criacao, edicao ou exclusao de vacinacao falharem e sofrerem rollback. Esse impacto e deliberado: nao e permitido confirmar silenciosamente uma alteracao que deveria ser auditada.

No fluxo Java atual, `saveAndFlush`, `flush` e `@Transactional` fazem a trigger executar antes do encerramento do caso de uso. Em criacao, uma falha no `saveAndFlush` reverte a Vaccination e impede que a Notification posterior seja criada; se outra falha ocorrer depois, a transacao continua preservando a atomicidade entre os dados ja persistidos. Nenhum contrato REST de sucesso muda; uma falha Oracle inesperada continua chegando ao tratamento global como erro de persistencia.

## Pre-verificacao obrigatoria

Antes de qualquer instalacao, executar somente consultas read-only e interromper se algum nome planejado ja existir:

```sql
SELECT object_name, object_type, status
  FROM user_objects
 WHERE object_name IN (
       'TB_CLYVO_VACCINATION_AUDIT',
       'PKG_CLYVO_AUDITORIA',
       'TRG_CV_AUD_VACCINATION',
       'IX_CV_VACC_AUD_VACC_EVENT'
 )
 ORDER BY object_name, object_type;

SELECT constraint_name, table_name, constraint_type
  FROM user_constraints
 WHERE constraint_name IN (
       'PK_CV_VACCINATION_AUDIT',
       'CK_CV_VACC_AUD_OPERATION',
       'CK_CV_VACC_AUD_VACC_ID',
       'CK_CV_VACC_AUD_OBS_CHANGED',
       'CK_CV_VACC_AUD_SNAPSHOT'
 );

SELECT privilege
  FROM session_privs
 WHERE privilege IN ('CREATE TABLE', 'CREATE PROCEDURE', 'CREATE TRIGGER');

SELECT table_name
  FROM user_tables
 WHERE table_name IN ('TB_VACCINATIONS', 'TB_AUDITORIA_VACINACAO');
```

`TB_AUDITORIA_VACINACAO` academica pode coexistir porque possui nome e origem diferentes. Ela nao deve ser renomeada, removida ou usada como destino da nova trigger.

Confirmar ainda quota no tablespace, Oracle 19c, schema autorizado, `TB_VACCINATIONS` canonica e permissao de criar os tres tipos de objeto. DDL Oracle possui commit implicito e a instalacao nao e atomicamente reversivel. Nao executar `script_bd_oracle.sql` novamente.

## Ordem de instalacao

Somente depois de autorizacao e pre-verificacao sem conflitos:

```sql
@create_clyvo_vaccination_audit.sql
@pkg_clyvo_auditoria_spec.sql
@pkg_clyvo_auditoria_body.sql
@trg_cv_aud_vaccination.sql
@validate_clyvo_vaccination_audit.sql
```

A ordem e obrigatoria: o package depende da tabela e a trigger depende do package e de `TB_VACCINATIONS`. O validador read-only deve ser executado antes de qualquer ensaio mutavel.

Nao ha script de uninstall nesta fase. Remover tabela, package ou trigger seria destrutivo e exige plano separado, revisao de retencao e autorizacao explicita.

## Validacao somente leitura

`validate_clyvo_vaccination_audit.sql` nao inicia DML nem altera objetos. Deve rodar em sessao dedicada, sem transacao pendente; em caso de erro, a diretiva SQL*Plus executa rollback defensivo em vez de confirmar trabalho anterior. Ele verifica:

1. existencia e status de tabela, package, body e trigger;
2. ausencia de `USER_ERRORS`;
3. 21 colunas, identity, PK, checks e ausencia de FKs;
4. ordem do indice por vacinacao e instante;
5. trigger AFTER EACH ROW habilitada para as tres operacoes;
6. dependencias compiladas;
7. ausencia de controle transacional, autonomous transaction e SQL dinamico no codigo compilado;
8. ausencia de DML da trigger sobre `TB_VACCINATIONS`;
9. ausencia de colunas sensiveis;
10. inventario agregado dos eventos existentes.

Esse roteiro valida metadata e fonte compilada, mas nao prova o comportamento DML.

## Teste transacional mutavel

`test_clyvo_vaccination_audit_transactional.sql` e explicitamente mutavel. Ele nao deve ser executado durante instalacao ou validacao read-only.

Pre-condicoes:

- autorizacao explicita para DML;
- janela sem concorrencia sobre a massa de teste;
- pelo menos um Animal e um Veterinario aprovados como referencias descartaveis;
- captura do estado anterior e monitoramento da sessao;
- revisao integral do script antes da execucao.

O script cria somente uma Vaccination marcada `CV_AUD_TEST_<timestamp>`, sem modificar Animal ou Veterinario. Na mesma transacao ele valida INSERT, UPDATE relevante, UPDATE sem mudanca auditavel, mudanca de observacoes sem copiar conteudo, DELETE, snapshot apos DELETE e rollback dos eventos. O bloco executa `ROLLBACK TO SAVEPOINT` e o arquivo termina com `ROLLBACK`.

Identity Oracle usa sequencia interna nao transacional. O rollback remove as linhas, mas pode deixar lacunas nos IDs de Vaccination e auditoria; isso e esperado e nao representa falha.

## Teste controlado da politica de falha

Provar fail-closed exige induzir uma falha real de infraestrutura. Nao foi criado flag de teste no package, `WHEN OTHERS` artificial ou DDL temporario porque qualquer um deles enfraqueceria a implementacao.

Em janela autorizada, duas sessoes podem validar o bloqueio:

1. Sessao A inicia transacao e executa `LOCK TABLE TB_CLYVO_VACCINATION_AUDIT IN EXCLUSIVE MODE` sem commit.
2. Sessao B define `ALTER SESSION SET DML_LOCK_TIMEOUT = 2`, cria um savepoint e tenta inserir uma Vaccination marcada `CV_AUD_FAIL_TEST_*`.
3. A trigger tenta inserir a auditoria, aguarda o lock e a operacao deve falhar por timeout; a excecao deve ser capturada apenas pelo roteiro de teste.
4. Ainda na Sessao B, consultas read-only devem confirmar zero Vaccination e zero evento com o marcador.
5. Sessao B executa `ROLLBACK`; Sessao A executa `ROLLBACK` para liberar o lock.

Esse ensaio e mutavel, concorrente e potencialmente bloqueante. Ele nao esta automatizado e nao deve ocorrer sem aprovacao, monitoramento e um timeout curto. Matar sessao, revogar privilegios ou invalidar objetos nao faz parte do protocolo.

## Compatibilidade com o CRUD Java

- `create`: INSERT gera um evento com lado NEW;
- `update`: UPDATE gera evento somente se o request mudar campo auditavel;
- `delete`: DELETE gera evento com lado OLD antes de a linha desaparecer;
- `saveAndFlush` e `flush` fazem erros aparecerem dentro do Service transacional;
- callbacks `@PrePersist` e `@PreUpdate` continuam inalterados;
- leituras por JPA e `PKG_CLYVO_VACINACAO` continuam consultando somente `TB_VACCINATIONS`;
- endpoints e DTOs nao expoem a tabela de auditoria;
- PostgreSQL, H2, Render, testes Java e frontend nao recebem essa extensao Oracle.

## Seguranca, retencao e operacao

- A tabela deve ser legivel somente pelo owner e por perfis administrativos explicitamente autorizados. Nenhum grant publico e preparado.
- Nao existe endpoint, repository JPA ou procedure publica de consulta nesta fase.
- Eventos sao append-only por convencao e privilegio; o schema owner ainda pode altera-los, portanto isso nao equivale a armazenamento imutavel externo.
- Nao existe politica automatica de purge, particionamento ou arquivamento. Retencao legal e prazo devem ser definidos antes de producao real.
- O crescimento e aproximadamente um evento por INSERT/DELETE e por UPDATE relevante. Monitorar quota, tamanho, taxa de DML e indice.
- Cada mutacao adiciona um INSERT e manutencao de PK/indice. Nao ha consulta adicional da tabela operacional nem lock deliberado alem dos locks normais do INSERT de auditoria.
- Concorrencia em Vaccinations diferentes gera IDs independentes; a ordem global e definida por `EVENT_AT` e `ID`, nao pelo momento de inicio da transacao.
- Rollback pode produzir lacunas de identity. IDs nao representam quantidade nem sequencia sem falhas.
- `EVENT_AT` e o instante de execucao da trigger, nao o instante do commit.
- Sem autenticacao principal e sem `CLIENT_IDENTIFIER` configurado, a auditoria prova o estado persistido e a conta de banco, mas nao prova a identidade HTTP de quem enviou a requisicao.

## Evidencias para o video FIAP

Depois de instalacao e ensaio realmente autorizados, registrar sem exibir credenciais ou dados reais:

1. pre-verificacao sem conflitos;
2. `USER_OBJECTS` com tabela, package, body e trigger validos;
3. `USER_ERRORS` vazio;
4. constraints, identity e indice;
5. INSERT com snapshot NEW;
6. UPDATE relevante com OLD/NEW;
7. no-op sem novo evento;
8. observacoes apenas com indicador de mudanca;
9. DELETE com snapshot OLD preservado;
10. rollback removendo Vaccination e eventos;
11. falha controlada impedindo a mutacao principal;
12. consulta demonstrando ausencia de senhas, tokens e observacoes na tabela.

No estado atual, somente os arquivos e a inspecao local podem ser apresentados. Nao afirmar implantacao Oracle antes das evidencias reais.
