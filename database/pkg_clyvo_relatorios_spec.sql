-- CLYVO - package Oracle 19c de indicadores e consultas por periodo.
-- Somente leitura: nao executa DML, COMMIT ou ROLLBACK.
-- Autenticacao e autorizacao permanecem na aplicacao Java.

SET ECHO ON
SET FEEDBACK ON
SET DEFINE OFF
WHENEVER OSERROR EXIT FAILURE
WHENEVER SQLERROR EXIT SQL.SQLCODE

CREATE OR REPLACE PACKAGE pkg_clyvo_relatorios AUTHID DEFINER AS

    c_err_parametro_invalido     CONSTANT PLS_INTEGER := -20021;
    c_err_parametro_obrigatorio  CONSTANT PLS_INTEGER := -20022;
    c_err_tutor_inexistente      CONSTANT PLS_INTEGER := -20023;
    c_err_veterinario_inexistente CONSTANT PLS_INTEGER := -20024;
    c_err_intervalo_invalido     CONSTANT PLS_INTEGER := -20025;

    FUNCTION fn_qtd_animais_tutor(
        p_tutor_id IN NUMBER
    ) RETURN NUMBER;

    FUNCTION fn_qtd_pacientes_veterinario(
        p_veterinarian_id IN NUMBER
    ) RETURN NUMBER;

    FUNCTION fn_qtd_exames_pendentes_veterinario(
        p_veterinarian_id IN NUMBER
    ) RETURN NUMBER;

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

END pkg_clyvo_relatorios;
/

SHOW ERRORS PACKAGE pkg_clyvo_relatorios

DECLARE
    v_status user_objects.status%TYPE;
BEGIN
    SELECT status
      INTO v_status
      FROM user_objects
     WHERE object_name = 'PKG_CLYVO_RELATORIOS'
       AND object_type = 'PACKAGE';

    IF v_status <> 'VALID' THEN
        RAISE_APPLICATION_ERROR(-20960, 'Package specification ficou INVALID.');
    END IF;
END;
/
