-- CLYVO - API Oracle 19c para persistencia da auditoria vacinal.
-- Compartilha a transacao do chamador e nao altera TB_VACCINATIONS.

SET ECHO ON
SET FEEDBACK ON
SET DEFINE OFF
WHENEVER OSERROR EXIT FAILURE
WHENEVER SQLERROR EXIT SQL.SQLCODE

CREATE OR REPLACE PACKAGE pkg_clyvo_auditoria AUTHID DEFINER AS

    c_operation_insert CONSTANT VARCHAR2(6) := 'INSERT';
    c_operation_update CONSTANT VARCHAR2(6) := 'UPDATE';
    c_operation_delete CONSTANT VARCHAR2(6) := 'DELETE';

    c_err_operation_invalid CONSTANT PLS_INTEGER := -20041;
    c_err_vaccination_id    CONSTANT PLS_INTEGER := -20042;
    c_err_snapshot_invalid  CONSTANT PLS_INTEGER := -20043;
    c_err_no_change         CONSTANT PLS_INTEGER := -20044;

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
    );

END pkg_clyvo_auditoria;
/

SHOW ERRORS PACKAGE pkg_clyvo_auditoria

DECLARE
    v_status user_objects.status%TYPE;
BEGIN
    SELECT status
      INTO v_status
      FROM user_objects
     WHERE object_name = 'PKG_CLYVO_AUDITORIA'
       AND object_type = 'PACKAGE';

    IF v_status <> 'VALID' THEN
        RAISE_APPLICATION_ERROR(-20940, 'Package specification ficou INVALID.');
    END IF;
END;
/
