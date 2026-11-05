# Database — CLYVO Vet | Sprint 4

Ponto de entrada para o banco operacional da [API Java](../README.md). A validação acadêmica usou **Oracle Database 19c**; PostgreSQL 16 (`local`/`prod`) e H2 (`render`/`test`) permanecem disponíveis por profile. Esta documentação não executa scripts nem conecta a bancos.

## Modelo relacional e DDL

O modelo lógico representa Tutor 1:N Animal, Veterinário N:N Tutor por vínculo, consultas de Animal/Tutor/Veterinário, prontuário 1:1 consulta, prescrição 1:1 prontuário com itens ordenados, exames por prontuário, vacinação por Animal e Veterinário, notificações, tokens de dispositivo e conversas com mensagens. O modelo físico canônico corresponde às **14 entidades JPA** (`model/`) e às 14 tabelas de [script_bd_oracle.sql](script_bd_oracle.sql); o DDL PostgreSQL equivalente está em [script_bd.sql](script_bd.sql). A correspondência completa Entity/tabela está em [README-oracle.md](README-oracle.md#tabelas-e-entidades). Os modelos acadêmicos originais estão em [../../../database/](../../../database/README.md); conferir os diagramas/PDFs de entrega separadamente, pois não há PDFs de modelo versionados nesta pasta.

O DDL Oracle define PKs identity, 22 FKs, 6 `UNIQUE`, checks para enums/booleanos e 17 índices explícitos de consulta, além de `NOT NULL`. Os nomes `CV` evitam conflitos com o SQL acadêmico legado. [README-oracle.md](README-oracle.md) detalha tipos, constraints, índices, verificações e limitações de `VARCHAR2` no Oracle. `TB_FATO_ATENDIMENTO` e `TB_AUDITORIA_VACINACAO` do SQL acadêmico não integram as 14 tabelas operacionais; a auditoria operacional usa **outra** tabela, `TB_CLYVO_VACCINATION_AUDIT`.

**Ordem em instalação nova, somente com autorização e pré-verificação de `USER_OBJECTS`, `USER_CONSTRAINTS`, `USER_INDEXES`, privilégios e quota:**

1. [script_bd_oracle.sql](script_bd_oracle.sql) — DDL das 14 entidades; não contém seed. Ver [procedimento Oracle](README-oracle.md).
2. [pkg_clyvo_vacinacao_spec.sql](pkg_clyvo_vacinacao_spec.sql), [pkg_clyvo_vacinacao_body.sql](pkg_clyvo_vacinacao_body.sql), depois [validate_pkg_clyvo_vacinacao.sql](validate_pkg_clyvo_vacinacao.sql) (somente leitura).
3. [pkg_clyvo_relatorios_spec.sql](pkg_clyvo_relatorios_spec.sql), [pkg_clyvo_relatorios_body.sql](pkg_clyvo_relatorios_body.sql), depois [validate_pkg_clyvo_relatorios.sql](validate_pkg_clyvo_relatorios.sql) (somente leitura); depende do package de vacinação.
4. [create_clyvo_vaccination_audit.sql](create_clyvo_vaccination_audit.sql), [pkg_clyvo_auditoria_spec.sql](pkg_clyvo_auditoria_spec.sql), [pkg_clyvo_auditoria_body.sql](pkg_clyvo_auditoria_body.sql), [trg_cv_aud_vaccination.sql](trg_cv_aud_vaccination.sql), depois [validate_clyvo_vaccination_audit.sql](validate_clyvo_vaccination_audit.sql) (somente leitura). O [teste transacional](README-oracle-vaccination-audit.md#teste-transacional-mutavel) é mutável e exige autorização separada.

**No schema institucional, estes objetos já foram instalados segundo confirmação manual do usuário: não repetir DDL ou `CREATE OR REPLACE` sem inventário e decisão explícita.** DDL Oracle faz commit implícito; validação do Hibernate (`ddl-auto=validate`) não cria objetos. Os seeds H2/PostgreSQL não são scripts Oracle.

## PL/SQL e Java

| Objeto | Responsabilidade e integração |
|---|---|
| [PKG_CLYVO_VACINACAO](README-oracle-pkg-vacinacao.md) | `FN_SITUACAO_DATA_DOSE`, `FN_QTD_ATENCOES_TUTOR`, `PR_HISTORICO_ANIMAL`, `PR_AGENDA_DOSES_TUTOR`. Apenas o histórico é chamado diretamente pelo Java (`OracleVaccinationHistoryReader`). Erros previstos `-20001` a `-20004`. |
| [PKG_CLYVO_RELATORIOS](README-oracle-pkg-relatorios.md) | Contagens de pets/pacientes/exames, `PR_INDICADORES_TUTOR`, `PR_INDICADORES_VETERINARIO` e consultas por período. O Java chama somente os dois indicadores (`OracleDashboardMetricsReader`); as consultas por período não têm consumidor Java. Reutiliza a função vacinal. Erros previstos `-20021` a `-20025`. |
| [PKG_CLYVO_AUDITORIA](README-oracle-vaccination-audit.md) | `PR_REGISTER_VACCINATION_EVENT` valida e grava snapshots OLD/NEW, com erros `-20041` a `-20044`; não há chamada direta Java. |
| [TRG_CV_AUD_VACCINATION](README-oracle-vaccination-audit.md) | Trigger Oracle independente, `AFTER INSERT OR UPDATE OR DELETE FOR EACH ROW` em `TB_VACCINATIONS`; filtra UPDATE sem mudança auditável e delega ao package. Não grava textos de observações. |

As leituras de vacinação e indicadores são somente leitura; o Java autoriza Tutor/Animal/vínculo antes das chamadas JDBC. Erros PL/SQL previstos são traduzidos pelos readers; falhas inesperadas não são mascaradas. Trigger e package de auditoria participam da mesma transação do DML original, sem commit/autonomous transaction: rollback remove também o evento. O usuário confirmou compilação/validação manual no Oracle 19c e teste transacional com **quatro eventos antes do rollback**: INSERT, UPDATE relevante, UPDATE sem mudança auditável (sem evento), UPDATE de observações (indicador sem copiar texto) e DELETE. O ensaio confirmou ausência das linhas após reversão. A [validação estrutural](README-oracle-vaccination-audit.md#validacao-somente-leitura) é distinta desse ensaio.

## Oracle → JSON → MongoDB

O fluxo implementado em [oracle-mongo/README.md](oracle-mongo/README.md) usa três exports SQL/JSON `RETURNING CLOB`, spool Base64 intermediário, `exports.py convert` para JSON array + JSONL, `exports.py validate` e `import.sh` com `mongoimport --mode upsert --upsertFields _id`. Collections: `animal_profiles`, `tutor_summaries`, `veterinary_appointments`. `_id` combina tipo e ID Oracle decimal em string; reimportar o mesmo snapshot substitui documentos, sem sincronizar exclusões.

Segundo validação **manual informada pelo usuário**, três arquivos JSON foram exportados e validados, um documento foi importado em cada collection sem falhas na primeira importação e uma reimportação não criou duplicatas, usando MongoDB 7 local via Docker. Esta revisão não repetiu exportação/importação nem acessou os bancos. Os documentos exportados não são o histórico de IA do .NET (`clyvo_ai`); Java e .NET não consomem essas collections.

## Entrega acadêmica e demonstração

| Requisito Mastering Relational and Non-Relational Database | Entregável/evidência |
|---|---|
| Modelo lógico e físico | Relações acima, DDL canônico e modelos acadêmicos preparados separadamente; conferir PDFs finais e correspondência dos diagramas. |
| DDL Oracle | `script_bd_oracle.sql`, inventário das 14 tabelas, PK/FK/constraints/índices via consultas do README Oracle. |
| Packages, functions, procedures, trigger e exceções | Scripts e validadores acima; capturar `USER_OBJECTS`, `USER_ERRORS` e teste de auditoria com rollback sem dados privados. |
| Chamadas Java | Endpoints de histórico vacinal e dashboards no profile `oracle`; demonstrar autorização e `SimpleJdbcCall` (ver [README Java](../README.md)). |
| Dataset relacional → JSON → MongoDB | Spools privados, conversão, validação, importação, contagens das três collections e reimportação por `_id` (ver [roteiro](oracle-mongo/README.md)). |
| Vídeo | Roteiro: modelo → objetos Oracle → chamada HTTP Java → export/validate → Mongo counts → reimportação sem duplicatas → rollback auditável. Mostrar somente dados fictícios e ocultar credenciais. |

Scripts e integrações estão preparados; execução institucional, teste transacional e importação foram **confirmados pelo usuário**, não por esta revisão. Permanecem para a entrega a conferência/empacotamento dos PDFs dos modelos e das evidências e a gravação do vídeo. As orientações específicas do professor sobre correções da Sprint 3 não estão disponíveis aqui para atestar atendimento.
