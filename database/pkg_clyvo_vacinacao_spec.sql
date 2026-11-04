-- CLYVO - package Oracle 19c de consultas de vacinacao.
-- Somente leitura: nao executa DML, COMMIT ou ROLLBACK.
-- A autorizacao do Tutor/Veterinario permanece na aplicacao Java.

SET ECHO ON
SET FEEDBACK ON
SET DEFINE OFF
WHENEVER OSERROR EXIT FAILURE
WHENEVER SQLERROR EXIT SQL.SQLCODE

CREATE OR REPLACE PACKAGE pkg_clyvo_vacinacao AUTHID DEFINER AS

    c_status_sem_data         CONSTANT VARCHAR2(20) := 'SEM_DATA';
    c_status_prevista_vencida CONSTANT VARCHAR2(20) := 'PREVISTA_VENCIDA';
    c_status_prevista_hoje    CONSTANT VARCHAR2(20) := 'PREVISTA_HOJE';
    c_status_prevista_futura  CONSTANT VARCHAR2(20) := 'PREVISTA_FUTURA';

    c_filtro_vencida CONSTANT VARCHAR2(10) := 'VENCIDA';
    c_filtro_hoje    CONSTANT VARCHAR2(10) := 'HOJE';
    c_filtro_futura  CONSTANT VARCHAR2(10) := 'FUTURA';
    c_filtro_todas   CONSTANT VARCHAR2(10) := 'TODAS';

    c_err_parametro_invalido CONSTANT PLS_INTEGER := -20001;
    c_err_tutor_inexistente  CONSTANT PLS_INTEGER := -20002;
    c_err_animal_inexistente CONSTANT PLS_INTEGER := -20003;
    c_err_filtro_invalido    CONSTANT PLS_INTEGER := -20004;

    FUNCTION fn_situacao_data_dose(
        p_next_dose_date  IN DATE,
        p_data_referencia IN DATE
    ) RETURN VARCHAR2;

    FUNCTION fn_qtd_atencoes_tutor(
        p_tutor_id        IN NUMBER,
        p_data_referencia IN DATE
    ) RETURN NUMBER;

    PROCEDURE pr_historico_animal(
        p_animal_id IN NUMBER,
        p_resultado OUT SYS_REFCURSOR
    );

    PROCEDURE pr_agenda_doses_tutor(
        p_tutor_id        IN NUMBER,
        p_data_referencia IN DATE,
        p_situacao        IN VARCHAR2,
        p_resultado       OUT SYS_REFCURSOR
    );

END pkg_clyvo_vacinacao;
/

SHOW ERRORS PACKAGE pkg_clyvo_vacinacao

DECLARE
    v_status user_objects.status%TYPE;
BEGIN
    SELECT status
      INTO v_status
      FROM user_objects
     WHERE object_name = 'PKG_CLYVO_VACINACAO'
       AND object_type = 'PACKAGE';

    IF v_status <> 'VALID' THEN
        RAISE_APPLICATION_ERROR(-20990, 'Package specification ficou INVALID.');
    END IF;
END;
/
