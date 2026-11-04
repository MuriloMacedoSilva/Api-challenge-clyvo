-- CLYVO - validacao SOMENTE LEITURA da auditoria vacinal Oracle 19c.
-- Nao inicia DML ou DDL. Em erro, SQL*Plus executa ROLLBACK defensivo;
-- usar sessao dedicada, sem transacao anterior pendente.

SET SERVEROUTPUT ON SIZE UNLIMITED
SET ECHO ON
SET FEEDBACK ON
SET VERIFY OFF
WHENEVER OSERROR EXIT FAILURE ROLLBACK
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT ============================================================
PROMPT 1. OBJETOS, STATUS E ERROS DE COMPILACAO
PROMPT ============================================================

SELECT object_name, object_type, status, last_ddl_time
  FROM user_objects
 WHERE object_name IN (
       'TB_CLYVO_VACCINATION_AUDIT',
       'PKG_CLYVO_AUDITORIA',
       'TRG_CV_AUD_VACCINATION'
 )
 ORDER BY object_name, object_type;

SELECT name, type, line, position, text
  FROM user_errors
 WHERE name IN ('PKG_CLYVO_AUDITORIA', 'TRG_CV_AUD_VACCINATION')
 ORDER BY name, type, sequence;

DECLARE
    v_valid_objects PLS_INTEGER;
    v_errors PLS_INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO v_valid_objects
      FROM user_objects
     WHERE (
            object_name = 'TB_CLYVO_VACCINATION_AUDIT'
            AND object_type = 'TABLE'
         )
        OR (
            object_name = 'PKG_CLYVO_AUDITORIA'
            AND object_type IN ('PACKAGE', 'PACKAGE BODY')
            AND status = 'VALID'
         )
        OR (
            object_name = 'TRG_CV_AUD_VACCINATION'
            AND object_type = 'TRIGGER'
            AND status = 'VALID'
         );

    IF v_valid_objects <> 4 THEN
        RAISE_APPLICATION_ERROR(-20943, 'Objetos esperados ausentes ou INVALID.');
    END IF;

    SELECT COUNT(*)
      INTO v_errors
      FROM user_errors
     WHERE name IN ('PKG_CLYVO_AUDITORIA', 'TRG_CV_AUD_VACCINATION');

    IF v_errors <> 0 THEN
        RAISE_APPLICATION_ERROR(-20944, 'Existem erros de compilacao na auditoria.');
    END IF;
END;
/

PROMPT ============================================================
PROMPT 2. COLUNAS, IDENTITY, CONSTRAINTS E INDICES
PROMPT ============================================================

SELECT column_id, column_name, data_type, data_length,
       char_length, char_used, data_precision, data_scale,
       nullable, data_default
  FROM user_tab_columns
 WHERE table_name = 'TB_CLYVO_VACCINATION_AUDIT'
 ORDER BY column_id;

SELECT table_name, column_name, generation_type, identity_options
  FROM user_tab_identity_cols
 WHERE table_name = 'TB_CLYVO_VACCINATION_AUDIT';

SELECT constraint_name, constraint_type, status, validated, search_condition_vc
  FROM user_constraints
 WHERE table_name = 'TB_CLYVO_VACCINATION_AUDIT'
 ORDER BY constraint_type, constraint_name;

SELECT index_name, uniqueness, status, column_name, column_position, descend
  FROM user_ind_columns
  JOIN user_indexes USING (index_name, table_name)
 WHERE table_name = 'TB_CLYVO_VACCINATION_AUDIT'
 ORDER BY index_name, column_position;

DECLARE
    v_columns PLS_INTEGER;
    v_identity PLS_INTEGER;
    v_expected_constraints PLS_INTEGER;
    v_constraint_expressions PLS_INTEGER;
    v_foreign_keys PLS_INTEGER;
    v_audit_index PLS_INTEGER;
    v_audit_index_total_columns PLS_INTEGER;
    v_audit_index_columns PLS_INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO v_columns
      FROM user_tab_columns
     WHERE table_name = 'TB_CLYVO_VACCINATION_AUDIT';

    IF v_columns <> 21 THEN
        RAISE_APPLICATION_ERROR(-20945, 'Quantidade inesperada de colunas de auditoria.');
    END IF;

    SELECT COUNT(*)
      INTO v_identity
      FROM user_tab_identity_cols
     WHERE table_name = 'TB_CLYVO_VACCINATION_AUDIT'
       AND column_name = 'ID';

    IF v_identity <> 1 THEN
        RAISE_APPLICATION_ERROR(-20946, 'ID da auditoria nao esta configurado como identity.');
    END IF;

    SELECT COUNT(*)
      INTO v_expected_constraints
      FROM user_constraints
     WHERE table_name = 'TB_CLYVO_VACCINATION_AUDIT'
       AND status = 'ENABLED'
       AND validated = 'VALIDATED'
       AND (
            (constraint_name = 'PK_CV_VACCINATION_AUDIT' AND constraint_type = 'P')
            OR (constraint_name = 'CK_CV_VACC_AUD_OPERATION' AND constraint_type = 'C')
            OR (constraint_name = 'CK_CV_VACC_AUD_VACC_ID' AND constraint_type = 'C')
            OR (constraint_name = 'CK_CV_VACC_AUD_OBS_CHANGED' AND constraint_type = 'C')
            OR (constraint_name = 'CK_CV_VACC_AUD_SNAPSHOT' AND constraint_type = 'C')
       );

    IF v_expected_constraints <> 5 THEN
        RAISE_APPLICATION_ERROR(-20947, 'PK ou checks nomeados estao incompletos.');
    END IF;

    SELECT COUNT(*)
      INTO v_constraint_expressions
      FROM (
          SELECT constraint_name,
                 REGEXP_REPLACE(UPPER(search_condition_vc), '[[:space:]]', '') AS condition_text
            FROM user_constraints
           WHERE table_name = 'TB_CLYVO_VACCINATION_AUDIT'
      )
     WHERE (
            (
                constraint_name = 'CK_CV_VACC_AUD_OPERATION'
                AND REGEXP_LIKE(condition_text, 'OPERATION_TYPE.*INSERT.*UPDATE.*DELETE')
            )
            OR (
                constraint_name = 'CK_CV_VACC_AUD_VACC_ID'
                AND REGEXP_LIKE(condition_text, 'VACCINATION_ID.*>.*0')
                AND REGEXP_LIKE(condition_text, 'TRUNC.*VACCINATION_ID')
            )
            OR (
                constraint_name = 'CK_CV_VACC_AUD_OBS_CHANGED'
                AND REGEXP_LIKE(condition_text, 'OBSERVATIONS_CHANGED.*IN.*0.*1')
            )
            OR (
                constraint_name = 'CK_CV_VACC_AUD_SNAPSHOT'
                AND REGEXP_LIKE(condition_text, 'OPERATION_TYPE.*INSERT')
                AND REGEXP_LIKE(condition_text, 'OPERATION_TYPE.*UPDATE')
                AND REGEXP_LIKE(condition_text, 'OPERATION_TYPE.*DELETE')
                AND REGEXP_LIKE(condition_text, 'OLD_ANIMAL_ID')
                AND REGEXP_LIKE(condition_text, 'NEW_ANIMAL_ID')
                AND REGEXP_LIKE(condition_text, 'OLD_VACCINE_NAME')
                AND REGEXP_LIKE(condition_text, 'NEW_VACCINE_NAME')
            )
       );

    IF v_constraint_expressions <> 4 THEN
        RAISE_APPLICATION_ERROR(-20954, 'Expressoes dos checks estao divergentes.');
    END IF;

    SELECT COUNT(*)
      INTO v_foreign_keys
      FROM user_constraints
     WHERE table_name = 'TB_CLYVO_VACCINATION_AUDIT'
       AND constraint_type = 'R';

    IF v_foreign_keys <> 0 THEN
        RAISE_APPLICATION_ERROR(-20948, 'Auditoria nao deve possuir foreign keys.');
    END IF;

    SELECT COUNT(*)
      INTO v_audit_index
      FROM user_indexes
     WHERE table_name = 'TB_CLYVO_VACCINATION_AUDIT'
       AND index_name = 'IX_CV_VACC_AUD_VACC_EVENT'
       AND status = 'VALID'
       AND uniqueness = 'NONUNIQUE';

    SELECT COUNT(*)
      INTO v_audit_index_total_columns
      FROM user_ind_columns
     WHERE table_name = 'TB_CLYVO_VACCINATION_AUDIT'
       AND index_name = 'IX_CV_VACC_AUD_VACC_EVENT';

    SELECT COUNT(*)
      INTO v_audit_index_columns
      FROM user_ind_columns
     WHERE table_name = 'TB_CLYVO_VACCINATION_AUDIT'
       AND index_name = 'IX_CV_VACC_AUD_VACC_EVENT'
       AND (
            (column_name = 'VACCINATION_ID' AND column_position = 1)
            OR (column_name = 'EVENT_AT' AND column_position = 2)
            OR (column_name = 'ID' AND column_position = 3)
       )
       AND descend = 'ASC';

    IF v_audit_index <> 1
            OR v_audit_index_total_columns <> 3
            OR v_audit_index_columns <> 3 THEN
        RAISE_APPLICATION_ERROR(-20949, 'Indice de consulta por vacinacao esta divergente.');
    END IF;
END;
/

PROMPT ============================================================
PROMPT 3. TRIGGER, DEPENDENCIAS E ESCOPO
PROMPT ============================================================

SELECT trigger_name, trigger_type, triggering_event, table_name, status
  FROM user_triggers
 WHERE trigger_name = 'TRG_CV_AUD_VACCINATION';

SELECT name, type, referenced_name, referenced_type, dependency_type
  FROM user_dependencies
 WHERE name IN ('PKG_CLYVO_AUDITORIA', 'TRG_CV_AUD_VACCINATION')
 ORDER BY name, type, referenced_name;

SELECT line, text
  FROM user_source
 WHERE name IN ('PKG_CLYVO_AUDITORIA', 'TRG_CV_AUD_VACCINATION')
 ORDER BY name, type, line;

DECLARE
    v_trigger_ok PLS_INTEGER;
    v_forbidden_transaction_control PLS_INTEGER;
    v_forbidden_vaccination_dml PLS_INTEGER;
    v_sensitive_columns PLS_INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO v_trigger_ok
      FROM user_triggers
     WHERE trigger_name = 'TRG_CV_AUD_VACCINATION'
       AND table_name = 'TB_VACCINATIONS'
       AND trigger_type = 'AFTER EACH ROW'
       AND status = 'ENABLED'
       AND triggering_event LIKE '%INSERT%'
       AND triggering_event LIKE '%UPDATE%'
       AND triggering_event LIKE '%DELETE%';

    IF v_trigger_ok <> 1 THEN
        RAISE_APPLICATION_ERROR(-20950, 'Trigger nao esta habilitada com o contrato esperado.');
    END IF;

    SELECT COUNT(*)
      INTO v_forbidden_transaction_control
      FROM user_source
     WHERE name IN ('PKG_CLYVO_AUDITORIA', 'TRG_CV_AUD_VACCINATION')
       AND INSTR(LTRIM(text), '--') <> 1
       AND REGEXP_LIKE(
           text,
           '(^|[^A-Z_])(COMMIT|ROLLBACK|AUTONOMOUS_TRANSACTION|EXECUTE|IMMEDIATE)([^A-Z_]|$)',
           'i'
       );

    IF v_forbidden_transaction_control <> 0 THEN
        RAISE_APPLICATION_ERROR(-20951, 'Controle transacional ou SQL dinamico indevido encontrado.');
    END IF;

    SELECT COUNT(*)
      INTO v_forbidden_vaccination_dml
      FROM user_source
     WHERE name = 'TRG_CV_AUD_VACCINATION'
       AND INSTR(LTRIM(text), '--') <> 1
       AND REGEXP_LIKE(text, '(^|[^A-Z_])(INSERT|UPDATE|DELETE|MERGE)([^A-Z_]|$)', 'i')
       AND NOT REGEXP_LIKE(text, 'AFTER.*INSERT.*UPDATE.*DELETE.*ON', 'i');

    IF v_forbidden_vaccination_dml <> 0 THEN
        RAISE_APPLICATION_ERROR(-20952, 'Trigger tenta modificar TB_VACCINATIONS.');
    END IF;

    SELECT COUNT(*)
      INTO v_sensitive_columns
      FROM user_tab_columns
     WHERE table_name = 'TB_CLYVO_VACCINATION_AUDIT'
       AND (
            column_name LIKE '%PASSWORD%'
            OR column_name LIKE '%TOKEN%'
            OR column_name LIKE '%CPF%'
            OR column_name LIKE '%EMAIL%'
            OR column_name LIKE '%PHONE%'
            OR column_name LIKE '%CNPJ%'
            OR column_name LIKE '%CRMV%'
            OR column_name IN ('OBSERVATIONS', 'OLD_OBSERVATIONS', 'NEW_OBSERVATIONS')
       );

    IF v_sensitive_columns <> 0 THEN
        RAISE_APPLICATION_ERROR(-20953, 'Coluna sensivel indevida encontrada na auditoria.');
    END IF;
END;
/

PROMPT ============================================================
PROMPT 4. INVENTARIO SOMENTE LEITURA DOS EVENTOS
PROMPT ============================================================

SELECT operation_type, COUNT(*) AS event_count,
       MIN(event_at) AS first_event_at,
       MAX(event_at) AS last_event_at
  FROM tb_clyvo_vaccination_audit
 GROUP BY operation_type
 ORDER BY operation_type;

SELECT COUNT(*) AS invalid_snapshot_count
  FROM tb_clyvo_vaccination_audit
 WHERE database_user IS NULL
    OR vaccination_id IS NULL
    OR operation_type NOT IN ('INSERT', 'UPDATE', 'DELETE');

PROMPT VALIDACAO SOMENTE LEITURA CONCLUIDA. NENHUM DADO FOI ALTERADO.
