-- CLYVO - body do package Oracle 19c de consultas de vacinacao.
-- Depende de TUTOR, ANIMAL, VETERINARIAN e TB_VACCINATIONS.

SET ECHO ON
SET FEEDBACK ON
SET DEFINE OFF
WHENEVER OSERROR EXIT FAILURE
WHENEVER SQLERROR EXIT SQL.SQLCODE

CREATE OR REPLACE PACKAGE BODY pkg_clyvo_vacinacao AS

    PROCEDURE validar_id(
        p_id   IN NUMBER,
        p_nome IN VARCHAR2
    ) IS
    BEGIN
        IF p_id IS NULL OR p_id <= 0 OR p_id <> TRUNC(p_id) THEN
            RAISE_APPLICATION_ERROR(
                c_err_parametro_invalido,
                p_nome || ' deve ser um ID inteiro maior que zero.'
            );
        END IF;
    END validar_id;

    PROCEDURE validar_data_referencia(p_data_referencia IN DATE) IS
    BEGIN
        IF p_data_referencia IS NULL THEN
            RAISE_APPLICATION_ERROR(
                c_err_parametro_invalido,
                'Data de referencia e obrigatoria.'
            );
        END IF;
    END validar_data_referencia;

    PROCEDURE validar_tutor_existente(p_tutor_id IN NUMBER) IS
        v_encontrado PLS_INTEGER;
    BEGIN
        validar_id(p_tutor_id, 'Tutor');

        SELECT 1
          INTO v_encontrado
          FROM tutor
         WHERE id = p_tutor_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(
                c_err_tutor_inexistente,
                'Tutor nao encontrado.'
            );
    END validar_tutor_existente;

    PROCEDURE validar_animal_existente(p_animal_id IN NUMBER) IS
        v_encontrado PLS_INTEGER;
    BEGIN
        validar_id(p_animal_id, 'Animal');

        SELECT 1
          INTO v_encontrado
          FROM animal
         WHERE id = p_animal_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(
                c_err_animal_inexistente,
                'Animal nao encontrado.'
            );
    END validar_animal_existente;

    FUNCTION normalizar_filtro(p_situacao IN VARCHAR2) RETURN VARCHAR2 IS
        v_situacao VARCHAR2(10);
    BEGIN
        v_situacao := UPPER(TRIM(p_situacao));

        IF v_situacao IS NULL OR v_situacao NOT IN (
            c_filtro_vencida,
            c_filtro_hoje,
            c_filtro_futura,
            c_filtro_todas
        ) THEN
            RAISE_APPLICATION_ERROR(
                c_err_filtro_invalido,
                'Situacao deve ser VENCIDA, HOJE, FUTURA ou TODAS.'
            );
        END IF;

        RETURN v_situacao;
    END normalizar_filtro;

    FUNCTION fn_situacao_data_dose(
        p_next_dose_date  IN DATE,
        p_data_referencia IN DATE
    ) RETURN VARCHAR2 IS
        v_data_dose DATE;
        v_referencia DATE;
    BEGIN
        validar_data_referencia(p_data_referencia);

        IF p_next_dose_date IS NULL THEN
            RETURN c_status_sem_data;
        END IF;

        v_data_dose := TRUNC(p_next_dose_date);
        v_referencia := TRUNC(p_data_referencia);

        IF v_data_dose < v_referencia THEN
            RETURN c_status_prevista_vencida;
        ELSIF v_data_dose = v_referencia THEN
            RETURN c_status_prevista_hoje;
        END IF;

        RETURN c_status_prevista_futura;
    END fn_situacao_data_dose;

    FUNCTION fn_qtd_atencoes_tutor(
        p_tutor_id        IN NUMBER,
        p_data_referencia IN DATE
    ) RETURN NUMBER IS
        v_quantidade NUMBER;
    BEGIN
        validar_tutor_existente(p_tutor_id);
        validar_data_referencia(p_data_referencia);

        SELECT COUNT(*)
          INTO v_quantidade
          FROM tb_vaccinations vaccination
          JOIN animal animal_record
            ON animal_record.id = vaccination.animal_id
         WHERE animal_record.tutor_id = p_tutor_id
           AND vaccination.next_dose_date IS NOT NULL
           AND vaccination.next_dose_date < TRUNC(p_data_referencia) + 1;

        RETURN v_quantidade;
    END fn_qtd_atencoes_tutor;

    PROCEDURE pr_historico_animal(
        p_animal_id IN NUMBER,
        p_resultado OUT SYS_REFCURSOR
    ) IS
    BEGIN
        validar_animal_existente(p_animal_id);

        OPEN p_resultado FOR
            SELECT vaccination.id AS id,
                   animal_record.id AS animal_id,
                   animal_record.name AS animal_name,
                   veterinarian.cpf AS veterinarian_cpf,
                   veterinarian.name AS veterinarian_name,
                   veterinarian.crmv_number AS veterinarian_crmv_number,
                   veterinarian.crmv_state AS veterinarian_crmv_state,
                   vaccination.vaccine_name AS vaccine_name,
                   vaccination.application_date AS application_date,
                   vaccination.next_dose_date AS next_dose_date,
                   vaccination.batch_number AS batch_number,
                   vaccination.manufacturer AS manufacturer,
                   vaccination.observations AS observations,
                   vaccination.created_at AS created_at,
                   vaccination.updated_at AS updated_at,
                   fn_situacao_data_dose(
                       vaccination.next_dose_date,
                       TRUNC(SYSDATE)
                   ) AS dose_date_status
              FROM tb_vaccinations vaccination
              JOIN animal animal_record
                ON animal_record.id = vaccination.animal_id
              JOIN veterinarian veterinarian
                ON veterinarian.id = vaccination.veterinarian_id
             WHERE vaccination.animal_id = p_animal_id
             ORDER BY vaccination.application_date DESC,
                      vaccination.id DESC;
    END pr_historico_animal;

    PROCEDURE pr_agenda_doses_tutor(
        p_tutor_id        IN NUMBER,
        p_data_referencia IN DATE,
        p_situacao        IN VARCHAR2,
        p_resultado       OUT SYS_REFCURSOR
    ) IS
        v_situacao VARCHAR2(10);
        v_referencia DATE;
    BEGIN
        validar_tutor_existente(p_tutor_id);
        validar_data_referencia(p_data_referencia);

        v_situacao := normalizar_filtro(p_situacao);
        v_referencia := TRUNC(p_data_referencia);

        OPEN p_resultado FOR
            SELECT vaccination.id AS id,
                   animal_record.id AS animal_id,
                   animal_record.name AS animal_name,
                   veterinarian.cpf AS veterinarian_cpf,
                   veterinarian.name AS veterinarian_name,
                   veterinarian.crmv_number AS veterinarian_crmv_number,
                   veterinarian.crmv_state AS veterinarian_crmv_state,
                   vaccination.vaccine_name AS vaccine_name,
                   vaccination.application_date AS application_date,
                   vaccination.next_dose_date AS next_dose_date,
                   vaccination.batch_number AS batch_number,
                   vaccination.manufacturer AS manufacturer,
                   vaccination.observations AS observations,
                   vaccination.created_at AS created_at,
                   vaccination.updated_at AS updated_at,
                   fn_situacao_data_dose(
                       vaccination.next_dose_date,
                       v_referencia
                   ) AS dose_date_status
              FROM tb_vaccinations vaccination
              JOIN animal animal_record
                ON animal_record.id = vaccination.animal_id
              JOIN veterinarian veterinarian
                ON veterinarian.id = vaccination.veterinarian_id
             WHERE animal_record.tutor_id = p_tutor_id
               AND vaccination.next_dose_date IS NOT NULL
               AND (
                   v_situacao = c_filtro_todas
                   OR (
                       v_situacao = c_filtro_vencida
                       AND vaccination.next_dose_date < v_referencia
                   )
                   OR (
                       v_situacao = c_filtro_hoje
                       AND vaccination.next_dose_date >= v_referencia
                       AND vaccination.next_dose_date < v_referencia + 1
                   )
                   OR (
                       v_situacao = c_filtro_futura
                       AND vaccination.next_dose_date >= v_referencia + 1
                   )
               )
             ORDER BY vaccination.next_dose_date ASC,
                      animal_record.name ASC,
                      vaccination.id ASC;
    END pr_agenda_doses_tutor;

END pkg_clyvo_vacinacao;
/

SHOW ERRORS PACKAGE BODY pkg_clyvo_vacinacao

DECLARE
    v_status user_objects.status%TYPE;
BEGIN
    SELECT status
      INTO v_status
      FROM user_objects
     WHERE object_name = 'PKG_CLYVO_VACINACAO'
       AND object_type = 'PACKAGE BODY';

    IF v_status <> 'VALID' THEN
        RAISE_APPLICATION_ERROR(-20990, 'Package body ficou INVALID.');
    END IF;
END;
/
