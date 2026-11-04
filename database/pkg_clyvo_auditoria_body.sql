-- CLYVO - implementacao Oracle 19c da auditoria vacinal.
-- Depende somente de TB_CLYVO_VACCINATION_AUDIT.

SET ECHO ON
SET FEEDBACK ON
SET DEFINE OFF
WHENEVER OSERROR EXIT FAILURE
WHENEVER SQLERROR EXIT SQL.SQLCODE

CREATE OR REPLACE PACKAGE BODY pkg_clyvo_auditoria AS

    FUNCTION fn_values_equal(
        p_left  IN NUMBER,
        p_right IN NUMBER
    ) RETURN BOOLEAN IS
    BEGIN
        IF p_left IS NULL OR p_right IS NULL THEN
            RETURN p_left IS NULL AND p_right IS NULL;
        END IF;

        RETURN p_left = p_right;
    END fn_values_equal;

    FUNCTION fn_values_equal(
        p_left  IN VARCHAR2,
        p_right IN VARCHAR2
    ) RETURN BOOLEAN IS
    BEGIN
        IF p_left IS NULL OR p_right IS NULL THEN
            RETURN p_left IS NULL AND p_right IS NULL;
        END IF;

        RETURN p_left = p_right;
    END fn_values_equal;

    FUNCTION fn_values_equal(
        p_left  IN DATE,
        p_right IN DATE
    ) RETURN BOOLEAN IS
    BEGIN
        IF p_left IS NULL OR p_right IS NULL THEN
            RETURN p_left IS NULL AND p_right IS NULL;
        END IF;

        RETURN p_left = p_right;
    END fn_values_equal;

    PROCEDURE pr_register_vaccination_event(
        p_vaccination_id      IN NUMBER,
        p_operation_type      IN VARCHAR2,
        p_old_animal_id       IN NUMBER,
        p_new_animal_id       IN NUMBER,
        p_old_veterinarian_id IN NUMBER,
        p_new_veterinarian_id IN NUMBER,
        p_old_vaccine_name    IN VARCHAR2,
        p_new_vaccine_name    IN VARCHAR2,
        p_old_application_date IN DATE,
        p_new_application_date IN DATE,
        p_old_next_dose_date  IN DATE,
        p_new_next_dose_date  IN DATE,
        p_old_batch_number    IN VARCHAR2,
        p_new_batch_number    IN VARCHAR2,
        p_old_manufacturer    IN VARCHAR2,
        p_new_manufacturer    IN VARCHAR2,
        p_observations_changed IN NUMBER
    ) IS
        v_normalized_operation VARCHAR2(32767);
        v_operation_type tb_clyvo_vaccination_audit.operation_type%TYPE;
        v_client_identifier tb_clyvo_vaccination_audit.client_identifier%TYPE;
    BEGIN
        v_normalized_operation := UPPER(TRIM(p_operation_type));

        IF v_normalized_operation IS NULL OR v_normalized_operation NOT IN (
            c_operation_insert,
            c_operation_update,
            c_operation_delete
        ) THEN
            RAISE_APPLICATION_ERROR(
                c_err_operation_invalid,
                'Operacao de auditoria deve ser INSERT, UPDATE ou DELETE.'
            );
        END IF;

        v_operation_type := v_normalized_operation;

        IF p_vaccination_id IS NULL
                OR p_vaccination_id <= 0
                OR p_vaccination_id <> TRUNC(p_vaccination_id) THEN
            RAISE_APPLICATION_ERROR(
                c_err_vaccination_id,
                'ID da vacinacao deve ser inteiro maior que zero.'
            );
        END IF;

        IF p_observations_changed IS NULL OR p_observations_changed NOT IN (0, 1) THEN
            RAISE_APPLICATION_ERROR(
                c_err_snapshot_invalid,
                'Indicador de alteracao das observacoes deve ser zero ou um.'
            );
        END IF;

        IF v_operation_type = c_operation_insert THEN
            IF p_old_animal_id IS NOT NULL
                    OR p_old_veterinarian_id IS NOT NULL
                    OR p_old_vaccine_name IS NOT NULL
                    OR p_old_application_date IS NOT NULL
                    OR p_old_next_dose_date IS NOT NULL
                    OR p_old_batch_number IS NOT NULL
                    OR p_old_manufacturer IS NOT NULL
                    OR p_new_animal_id IS NULL
                    OR p_new_veterinarian_id IS NULL
                    OR p_new_vaccine_name IS NULL
                    OR p_new_application_date IS NULL
                    OR p_observations_changed <> 0 THEN
                RAISE_APPLICATION_ERROR(
                    c_err_snapshot_invalid,
                    'Snapshot de INSERT inconsistente.'
                );
            END IF;
        ELSIF v_operation_type = c_operation_delete THEN
            IF p_old_animal_id IS NULL
                    OR p_old_veterinarian_id IS NULL
                    OR p_old_vaccine_name IS NULL
                    OR p_old_application_date IS NULL
                    OR p_new_animal_id IS NOT NULL
                    OR p_new_veterinarian_id IS NOT NULL
                    OR p_new_vaccine_name IS NOT NULL
                    OR p_new_application_date IS NOT NULL
                    OR p_new_next_dose_date IS NOT NULL
                    OR p_new_batch_number IS NOT NULL
                    OR p_new_manufacturer IS NOT NULL
                    OR p_observations_changed <> 0 THEN
                RAISE_APPLICATION_ERROR(
                    c_err_snapshot_invalid,
                    'Snapshot de DELETE inconsistente.'
                );
            END IF;
        ELSE
            IF p_old_animal_id IS NULL
                    OR p_new_animal_id IS NULL
                    OR p_old_veterinarian_id IS NULL
                    OR p_new_veterinarian_id IS NULL
                    OR p_old_vaccine_name IS NULL
                    OR p_new_vaccine_name IS NULL
                    OR p_old_application_date IS NULL
                    OR p_new_application_date IS NULL THEN
                RAISE_APPLICATION_ERROR(
                    c_err_snapshot_invalid,
                    'Snapshot de UPDATE inconsistente.'
                );
            END IF;

            IF fn_values_equal(p_old_animal_id, p_new_animal_id)
                    AND fn_values_equal(p_old_veterinarian_id, p_new_veterinarian_id)
                    AND fn_values_equal(p_old_vaccine_name, p_new_vaccine_name)
                    AND fn_values_equal(p_old_application_date, p_new_application_date)
                    AND fn_values_equal(p_old_next_dose_date, p_new_next_dose_date)
                    AND fn_values_equal(p_old_batch_number, p_new_batch_number)
                    AND fn_values_equal(p_old_manufacturer, p_new_manufacturer)
                    AND p_observations_changed = 0 THEN
                RAISE_APPLICATION_ERROR(
                    c_err_no_change,
                    'UPDATE sem alteracao auditavel.'
                );
            END IF;
        END IF;

        v_client_identifier := NULLIF(
            TRIM(SYS_CONTEXT('USERENV', 'CLIENT_IDENTIFIER', 64)),
            ''
        );

        INSERT INTO tb_clyvo_vaccination_audit (
            vaccination_id,
            operation_type,
            old_animal_id,
            new_animal_id,
            old_veterinarian_id,
            new_veterinarian_id,
            old_vaccine_name,
            new_vaccine_name,
            old_application_date,
            new_application_date,
            old_next_dose_date,
            new_next_dose_date,
            old_batch_number,
            new_batch_number,
            old_manufacturer,
            new_manufacturer,
            observations_changed,
            database_user,
            client_identifier
        ) VALUES (
            p_vaccination_id,
            v_operation_type,
            p_old_animal_id,
            p_new_animal_id,
            p_old_veterinarian_id,
            p_new_veterinarian_id,
            p_old_vaccine_name,
            p_new_vaccine_name,
            p_old_application_date,
            p_new_application_date,
            p_old_next_dose_date,
            p_new_next_dose_date,
            p_old_batch_number,
            p_new_batch_number,
            p_old_manufacturer,
            p_new_manufacturer,
            p_observations_changed,
            SYS_CONTEXT('USERENV', 'SESSION_USER', 128),
            v_client_identifier
        );
    END pr_register_vaccination_event;

END pkg_clyvo_auditoria;
/

SHOW ERRORS PACKAGE BODY pkg_clyvo_auditoria

DECLARE
    v_status user_objects.status%TYPE;
BEGIN
    SELECT status
      INTO v_status
      FROM user_objects
     WHERE object_name = 'PKG_CLYVO_AUDITORIA'
       AND object_type = 'PACKAGE BODY';

    IF v_status <> 'VALID' THEN
        RAISE_APPLICATION_ERROR(-20941, 'Package body ficou INVALID.');
    END IF;
END;
/
