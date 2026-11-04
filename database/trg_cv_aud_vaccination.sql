-- CLYVO - trigger Oracle 19c de auditoria de TB_VACCINATIONS.
-- A persistencia e delegada ao package na mesma transacao do DML principal.

SET ECHO ON
SET FEEDBACK ON
SET DEFINE OFF
WHENEVER OSERROR EXIT FAILURE
WHENEVER SQLERROR EXIT SQL.SQLCODE

CREATE OR REPLACE TRIGGER trg_cv_aud_vaccination
AFTER INSERT OR UPDATE OR DELETE ON tb_vaccinations
FOR EACH ROW
DECLARE
    v_operation_type VARCHAR2(6);
    v_observations_changed NUMBER(1,0) := 0;

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
BEGIN
    IF INSERTING THEN
        v_operation_type := pkg_clyvo_auditoria.c_operation_insert;
    ELSIF DELETING THEN
        v_operation_type := pkg_clyvo_auditoria.c_operation_delete;
    ELSE
        v_operation_type := pkg_clyvo_auditoria.c_operation_update;
        IF NOT fn_values_equal(:OLD.observations, :NEW.observations) THEN
            v_observations_changed := 1;
        END IF;

        IF fn_values_equal(:OLD.animal_id, :NEW.animal_id)
                AND fn_values_equal(:OLD.veterinarian_id, :NEW.veterinarian_id)
                AND fn_values_equal(:OLD.vaccine_name, :NEW.vaccine_name)
                AND fn_values_equal(:OLD.application_date, :NEW.application_date)
                AND fn_values_equal(:OLD.next_dose_date, :NEW.next_dose_date)
                AND fn_values_equal(:OLD.batch_number, :NEW.batch_number)
                AND fn_values_equal(:OLD.manufacturer, :NEW.manufacturer)
                AND v_observations_changed = 0 THEN
            RETURN;
        END IF;
    END IF;

    pkg_clyvo_auditoria.pr_register_vaccination_event(
        p_vaccination_id       => NVL(:NEW.id, :OLD.id),
        p_operation_type       => v_operation_type,
        p_old_animal_id        => :OLD.animal_id,
        p_new_animal_id        => :NEW.animal_id,
        p_old_veterinarian_id  => :OLD.veterinarian_id,
        p_new_veterinarian_id  => :NEW.veterinarian_id,
        p_old_vaccine_name     => :OLD.vaccine_name,
        p_new_vaccine_name     => :NEW.vaccine_name,
        p_old_application_date => :OLD.application_date,
        p_new_application_date => :NEW.application_date,
        p_old_next_dose_date   => :OLD.next_dose_date,
        p_new_next_dose_date   => :NEW.next_dose_date,
        p_old_batch_number     => :OLD.batch_number,
        p_new_batch_number     => :NEW.batch_number,
        p_old_manufacturer     => :OLD.manufacturer,
        p_new_manufacturer     => :NEW.manufacturer,
        p_observations_changed => v_observations_changed
    );
END;
/

SHOW ERRORS TRIGGER trg_cv_aud_vaccination

DECLARE
    v_status user_objects.status%TYPE;
BEGIN
    SELECT status
      INTO v_status
      FROM user_objects
     WHERE object_name = 'TRG_CV_AUD_VACCINATION'
       AND object_type = 'TRIGGER';

    IF v_status <> 'VALID' THEN
        RAISE_APPLICATION_ERROR(-20942, 'Trigger ficou INVALID.');
    END IF;
END;
/
