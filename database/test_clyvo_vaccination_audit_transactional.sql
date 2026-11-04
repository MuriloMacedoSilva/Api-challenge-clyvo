-- CLYVO - TESTE MUTAVEL E DESCARTAVEL da auditoria vacinal Oracle 19c.
-- Executa INSERT, UPDATE e DELETE em uma vacinacao marcada CV_AUD_TEST_*.
-- Todas as alteracoes, inclusive os eventos de auditoria, terminam em ROLLBACK.
-- Requer autorizacao explicita, janela controlada e dados base descartaveis/aprovados.
-- NUNCA executar como validacao somente leitura ou junto do deploy.
-- Executar como script em sessao exclusiva, sem DML pendente e com autocommit OFF.
-- Em interrupcao, executar ROLLBACK na mesma sessao; nao deixar sessao aberta.

SET SERVEROUTPUT ON SIZE UNLIMITED
SET ECHO ON
SET FEEDBACK ON
SET VERIFY OFF
SET AUTOCOMMIT OFF
WHENEVER OSERROR EXIT FAILURE ROLLBACK
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

ACCEPT cv_aud_animal_id NUMBER PROMPT 'ID aprovado do Animal descartavel: '
ACCEPT cv_aud_veterinarian_id NUMBER PROMPT 'ID aprovado do Veterinario descartavel: '

DECLARE
v_animal_id NUMBER := 1;
    v_veterinarian_id NUMBER := 1;
    v_vaccination_id tb_vaccinations.id%TYPE;
    v_marker tb_vaccinations.vaccine_name%TYPE;
    v_count NUMBER;
    v_original_application_date CONSTANT DATE := TRUNC(SYSDATE) - 10;
    v_updated_application_date CONSTANT DATE := TRUNC(SYSDATE) - 9;
BEGIN
SAVEPOINT cv_audit_transactional_test;

IF v_animal_id IS NULL OR v_animal_id <= 0
            OR v_animal_id <> TRUNC(v_animal_id)
            OR v_veterinarian_id IS NULL OR v_veterinarian_id <= 0
            OR v_veterinarian_id <> TRUNC(v_veterinarian_id) THEN
        RAISE_APPLICATION_ERROR(
            -20970,
            'IDs aprovados devem ser inteiros positivos.'
        );
END IF;

SELECT COUNT(*) INTO v_count FROM animal WHERE id = v_animal_id;
IF v_count <> 1 THEN
        RAISE_APPLICATION_ERROR(-20970, 'Animal aprovado nao existe.');
END IF;

SELECT COUNT(*) INTO v_count FROM veterinarian WHERE id = v_veterinarian_id;
IF v_count <> 1 THEN
        RAISE_APPLICATION_ERROR(-20970, 'Veterinario aprovado nao existe.');
END IF;

    v_marker := 'CV_AUD_TEST_' || RAWTOHEX(SYS_GUID());

SELECT COUNT(*) INTO v_count FROM tb_vaccinations WHERE vaccine_name = v_marker;
IF v_count <> 0 THEN
        RAISE_APPLICATION_ERROR(-20971, 'Marcador descartavel ja existe em vacinacoes.');
END IF;

SELECT COUNT(*)
INTO v_count
FROM tb_clyvo_vaccination_audit
WHERE old_vaccine_name = v_marker OR new_vaccine_name = v_marker;

IF v_count <> 0 THEN
        RAISE_APPLICATION_ERROR(-20971, 'Marcador descartavel ja existe na auditoria.');
END IF;

    DBMS_OUTPUT.PUT_LINE('Marcador do teste: ' || v_marker);

INSERT INTO tb_vaccinations (
    animal_id,
    veterinarian_id,
    vaccine_name,
    application_date,
    next_dose_date,
    batch_number,
    manufacturer,
    observations,
    created_at,
    updated_at
) VALUES (
             v_animal_id,
             v_veterinarian_id,
             v_marker,
             v_original_application_date,
             v_original_application_date + 30,
             'CV-AUD-OLD',
             'CV Audit Test Lab',
             'Conteudo descartavel para provar que observacoes nao sao copiadas.',
             SYSTIMESTAMP,
             NULL
         ) RETURNING id INTO v_vaccination_id;

DBMS_OUTPUT.PUT_LINE('ID da vacinacao de teste: ' || v_vaccination_id);

SELECT COUNT(*)
INTO v_count
FROM tb_clyvo_vaccination_audit
WHERE vaccination_id = v_vaccination_id
  AND operation_type = 'INSERT'
  AND old_animal_id IS NULL
  AND new_animal_id = v_animal_id
  AND new_veterinarian_id = v_veterinarian_id
  AND new_vaccine_name = v_marker
  AND new_application_date = v_original_application_date
  AND new_next_dose_date = v_original_application_date + 30
  AND new_batch_number = 'CV-AUD-OLD'
  AND new_manufacturer = 'CV Audit Test Lab'
  AND old_veterinarian_id IS NULL
  AND old_vaccine_name IS NULL
  AND old_application_date IS NULL
  AND old_next_dose_date IS NULL
  AND old_batch_number IS NULL
  AND old_manufacturer IS NULL
  AND observations_changed = 0;

IF v_count <> 1 THEN
        RAISE_APPLICATION_ERROR(-20972, 'INSERT nao produziu o snapshot esperado.');
END IF;

UPDATE tb_vaccinations
SET application_date = v_updated_application_date,
    next_dose_date = v_updated_application_date + 45,
    batch_number = 'CV-AUD-NEW',
    manufacturer = 'CV Audit Updated Lab',
    updated_at = SYSTIMESTAMP
WHERE id = v_vaccination_id;

IF SQL%ROWCOUNT <> 1 THEN
        RAISE_APPLICATION_ERROR(-20980, 'UPDATE relevante nao afetou exatamente uma vacinacao.');
END IF;

SELECT COUNT(*)
INTO v_count
FROM tb_clyvo_vaccination_audit
WHERE vaccination_id = v_vaccination_id
  AND operation_type = 'UPDATE'
  AND old_application_date = v_original_application_date
  AND new_application_date = v_updated_application_date
  AND old_next_dose_date = v_original_application_date + 30
  AND new_next_dose_date = v_updated_application_date + 45
  AND old_batch_number = 'CV-AUD-OLD'
  AND new_batch_number = 'CV-AUD-NEW'
  AND old_manufacturer = 'CV Audit Test Lab'
  AND new_manufacturer = 'CV Audit Updated Lab'
  AND old_animal_id = v_animal_id
  AND new_animal_id = v_animal_id
  AND old_veterinarian_id = v_veterinarian_id
  AND new_veterinarian_id = v_veterinarian_id
  AND old_vaccine_name = v_marker
  AND new_vaccine_name = v_marker
  AND observations_changed = 0;

IF v_count <> 1 THEN
        RAISE_APPLICATION_ERROR(-20973, 'UPDATE relevante nao produziu o snapshot esperado.');
END IF;

SELECT COUNT(*)
INTO v_count
FROM tb_clyvo_vaccination_audit
WHERE vaccination_id = v_vaccination_id;

UPDATE tb_vaccinations
SET animal_id = animal_id,
    veterinarian_id = veterinarian_id,
    vaccine_name = vaccine_name,
    application_date = application_date,
    next_dose_date = next_dose_date,
    batch_number = batch_number,
    manufacturer = manufacturer,
    observations = observations,
    updated_at = SYSTIMESTAMP
WHERE id = v_vaccination_id;

IF SQL%ROWCOUNT <> 1 THEN
        RAISE_APPLICATION_ERROR(-20981, 'UPDATE sem mudanca nao afetou exatamente uma vacinacao.');
END IF;

    DECLARE
v_count_after_noop NUMBER;
BEGIN
SELECT COUNT(*)
INTO v_count_after_noop
FROM tb_clyvo_vaccination_audit
WHERE vaccination_id = v_vaccination_id;

IF v_count_after_noop <> v_count THEN
            RAISE_APPLICATION_ERROR(-20974, 'UPDATE sem mudanca auditavel criou evento.');
END IF;
END;

UPDATE tb_vaccinations
SET observations = 'Observacao descartavel alterada; conteudo nao deve ser auditado.',
    updated_at = SYSTIMESTAMP
WHERE id = v_vaccination_id;

IF SQL%ROWCOUNT <> 1 THEN
        RAISE_APPLICATION_ERROR(-20982, 'UPDATE de observacoes nao afetou exatamente uma vacinacao.');
END IF;

SELECT COUNT(*)
INTO v_count
FROM tb_clyvo_vaccination_audit
WHERE vaccination_id = v_vaccination_id
  AND operation_type = 'UPDATE'
  AND old_animal_id = v_animal_id
  AND new_animal_id = v_animal_id
  AND old_veterinarian_id = v_veterinarian_id
  AND new_veterinarian_id = v_veterinarian_id
  AND old_vaccine_name = v_marker
  AND new_vaccine_name = v_marker
  AND old_application_date = v_updated_application_date
  AND new_application_date = v_updated_application_date
  AND old_next_dose_date = v_updated_application_date + 45
  AND new_next_dose_date = v_updated_application_date + 45
  AND old_batch_number = 'CV-AUD-NEW'
  AND new_batch_number = 'CV-AUD-NEW'
  AND old_manufacturer = 'CV Audit Updated Lab'
  AND new_manufacturer = 'CV Audit Updated Lab'
  AND observations_changed = 1;

IF v_count <> 1 THEN
        RAISE_APPLICATION_ERROR(-20975, 'Mudanca de observacoes nao gerou indicador seguro.');
END IF;

DELETE FROM tb_vaccinations WHERE id = v_vaccination_id;

IF SQL%ROWCOUNT <> 1 THEN
        RAISE_APPLICATION_ERROR(-20983, 'DELETE nao afetou exatamente uma vacinacao.');
END IF;

SELECT COUNT(*)
INTO v_count
FROM tb_clyvo_vaccination_audit
WHERE vaccination_id = v_vaccination_id
  AND operation_type = 'DELETE'
  AND old_animal_id = v_animal_id
  AND old_veterinarian_id = v_veterinarian_id
  AND old_vaccine_name = v_marker
  AND old_application_date = v_updated_application_date
  AND old_next_dose_date = v_updated_application_date + 45
  AND old_batch_number = 'CV-AUD-NEW'
  AND old_manufacturer = 'CV Audit Updated Lab'
  AND new_animal_id IS NULL
  AND new_veterinarian_id IS NULL
  AND new_vaccine_name IS NULL
  AND new_application_date IS NULL
  AND new_next_dose_date IS NULL
  AND new_batch_number IS NULL
  AND new_manufacturer IS NULL
  AND observations_changed = 0;

IF v_count <> 1 THEN
        RAISE_APPLICATION_ERROR(-20976, 'DELETE nao preservou o snapshot esperado.');
END IF;

SELECT COUNT(*) INTO v_count
FROM tb_vaccinations
WHERE id = v_vaccination_id;

IF v_count <> 0 THEN
        RAISE_APPLICATION_ERROR(-20977, 'Vacinacao descartavel nao foi excluida no ensaio.');
END IF;

SELECT COUNT(*) INTO v_count
FROM tb_clyvo_vaccination_audit
WHERE vaccination_id = v_vaccination_id;

IF v_count <> 4 THEN
        RAISE_APPLICATION_ERROR(-20984, 'Quantidade de eventos antes do ROLLBACK difere de quatro.');
END IF;

ROLLBACK TO cv_audit_transactional_test;

SELECT COUNT(*)
INTO v_count
FROM tb_clyvo_vaccination_audit
WHERE old_vaccine_name = v_marker OR new_vaccine_name = v_marker;

IF v_count <> 0 THEN
        RAISE_APPLICATION_ERROR(-20978, 'ROLLBACK nao removeu os eventos nao confirmados.');
END IF;

SELECT COUNT(*)
INTO v_count
FROM tb_vaccinations
WHERE vaccine_name = v_marker;

IF v_count <> 0 THEN
        RAISE_APPLICATION_ERROR(-20979, 'ROLLBACK nao removeu a vacinacao descartavel.');
END IF;

    DBMS_OUTPUT.PUT_LINE(
        'TESTE TRANSACIONAL OK: INSERT, UPDATE, NO-OP, OBSERVACOES, DELETE E ROLLBACK.'
    );
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK TO cv_audit_transactional_test;
        RAISE;
END;
/

ROLLBACK;

UNDEFINE cv_aud_animal_id
UNDEFINE cv_aud_veterinarian_id

PROMPT TESTE MUTAVEL CONCLUIDO COM ROLLBACK. IDENTITIES PODEM TER LACUNAS.
