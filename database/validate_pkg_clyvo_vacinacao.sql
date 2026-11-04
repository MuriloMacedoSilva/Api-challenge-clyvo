-- CLYVO - validacao somente leitura de PKG_CLYVO_VACINACAO.
-- Pre-requisito: specification e body compilados no schema operacional.
-- Nao executa INSERT, UPDATE, DELETE, MERGE, COMMIT ou ROLLBACK.

SET SERVEROUTPUT ON SIZE UNLIMITED
SET ECHO ON
SET FEEDBACK ON
SET VERIFY OFF
WHENEVER OSERROR EXIT FAILURE
WHENEVER SQLERROR EXIT SQL.SQLCODE

PROMPT ============================================================
PROMPT 1. OBJETOS E ERROS DE COMPILACAO
PROMPT ============================================================

SELECT object_name, object_type, status, last_ddl_time
  FROM user_objects
 WHERE object_name = 'PKG_CLYVO_VACINACAO'
 ORDER BY object_type;

SELECT name, type, line, position, text
  FROM user_errors
 WHERE name = 'PKG_CLYVO_VACINACAO'
 ORDER BY type, sequence;

DECLARE
    v_objetos_validos PLS_INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO v_objetos_validos
      FROM user_objects
     WHERE object_name = 'PKG_CLYVO_VACINACAO'
       AND object_type IN ('PACKAGE', 'PACKAGE BODY')
       AND status = 'VALID';

    IF v_objetos_validos <> 2 THEN
        RAISE_APPLICATION_ERROR(-20990, 'Package specification/body nao estao VALID.');
    END IF;
END;
/

PROMPT ============================================================
PROMPT 2. FN_SITUACAO_DATA_DOSE
PROMPT ============================================================

DECLARE
    v_referencia CONSTANT DATE := DATE '2026-11-04';

    PROCEDURE assert_status(
        p_data     IN DATE,
        p_esperado IN VARCHAR2
    ) IS
        v_atual VARCHAR2(20);
    BEGIN
        v_atual := pkg_clyvo_vacinacao.fn_situacao_data_dose(
            p_data,
            v_referencia
        );

        IF v_atual IS NULL OR v_atual <> p_esperado THEN
            RAISE_APPLICATION_ERROR(
                -20991,
                'Status esperado=' || p_esperado || ', atual=' || v_atual
            );
        END IF;
    END assert_status;
BEGIN
    assert_status(NULL, 'SEM_DATA');
    assert_status(DATE '2026-11-03', 'PREVISTA_VENCIDA');
    assert_status(DATE '2026-11-04', 'PREVISTA_HOJE');
    assert_status(DATE '2026-11-05', 'PREVISTA_FUTURA');

    BEGIN
        DECLARE
            v_resultado VARCHAR2(20);
        BEGIN
            v_resultado := pkg_clyvo_vacinacao.fn_situacao_data_dose(
                DATE '2026-11-04',
                NULL
            );
            RAISE_APPLICATION_ERROR(-20992, 'Data de referencia nula foi aceita.');
        END;
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE <> pkg_clyvo_vacinacao.c_err_parametro_invalido THEN
                RAISE;
            END IF;
    END;

    DBMS_OUTPUT.PUT_LINE('FN_SITUACAO_DATA_DOSE: OK');
END;
/

PROMPT ============================================================
PROMPT 3. FN_QTD_ATENCOES_TUTOR E ERROS DE TUTOR
PROMPT ============================================================

DECLARE
    v_tutor_id tutor.id%TYPE;
    v_tutor_inexistente tutor.id%TYPE;
    v_quantidade NUMBER;
    v_quantidade_esperada NUMBER;
    v_referencia CONSTANT DATE := TRUNC(SYSDATE);
BEGIN
    SELECT MIN(id), NVL(MAX(id), 0) + 1
      INTO v_tutor_id, v_tutor_inexistente
      FROM tutor;

    IF v_tutor_id IS NOT NULL THEN
        v_quantidade := pkg_clyvo_vacinacao.fn_qtd_atencoes_tutor(
            v_tutor_id,
            v_referencia
        );

        SELECT COUNT(*)
          INTO v_quantidade_esperada
          FROM tb_vaccinations vaccination
          JOIN animal animal_record
            ON animal_record.id = vaccination.animal_id
         WHERE animal_record.tutor_id = v_tutor_id
           AND vaccination.next_dose_date IS NOT NULL
           AND vaccination.next_dose_date < v_referencia + 1;

        IF v_quantidade IS NULL OR v_quantidade <> v_quantidade_esperada THEN
            RAISE_APPLICATION_ERROR(-20993, 'Contagem de atencoes inconsistente.');
        END IF;

        DBMS_OUTPUT.PUT_LINE(
            'FN_QTD_ATENCOES_TUTOR: tutor=' || v_tutor_id ||
            ', quantidade=' || v_quantidade
        );
    ELSE
        DBMS_OUTPUT.PUT_LINE('FN_QTD_ATENCOES_TUTOR: sem Tutor; caso valido ignorado.');
    END IF;

    BEGIN
        v_quantidade := pkg_clyvo_vacinacao.fn_qtd_atencoes_tutor(
            v_tutor_inexistente,
            TRUNC(SYSDATE)
        );
        RAISE_APPLICATION_ERROR(-20994, 'Tutor inexistente foi aceito.');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE <> pkg_clyvo_vacinacao.c_err_tutor_inexistente THEN
                RAISE;
            END IF;
    END;

    BEGIN
        v_quantidade := pkg_clyvo_vacinacao.fn_qtd_atencoes_tutor(
            NULL,
            TRUNC(SYSDATE)
        );
        RAISE_APPLICATION_ERROR(-20995, 'Tutor nulo foi aceito.');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE <> pkg_clyvo_vacinacao.c_err_parametro_invalido THEN
                RAISE;
            END IF;
    END;
END;
/

PROMPT ============================================================
PROMPT 4. PR_HISTORICO_ANIMAL
PROMPT ============================================================

DECLARE
    v_animal_id animal.id%TYPE;
    v_animal_sem_historico animal.id%TYPE;
    v_animal_inexistente animal.id%TYPE;
    v_resultado SYS_REFCURSOR;

    v_id tb_vaccinations.id%TYPE;
    v_row_animal_id animal.id%TYPE;
    v_animal_name animal.name%TYPE;
    v_veterinarian_cpf veterinarian.cpf%TYPE;
    v_veterinarian_name veterinarian.name%TYPE;
    v_crmv_number veterinarian.crmv_number%TYPE;
    v_crmv_state veterinarian.crmv_state%TYPE;
    v_vaccine_name tb_vaccinations.vaccine_name%TYPE;
    v_application_date tb_vaccinations.application_date%TYPE;
    v_next_dose_date tb_vaccinations.next_dose_date%TYPE;
    v_dose_date_status VARCHAR2(20);
    v_batch_number tb_vaccinations.batch_number%TYPE;
    v_manufacturer tb_vaccinations.manufacturer%TYPE;
    v_observations tb_vaccinations.observations%TYPE;
    v_created_at tb_vaccinations.created_at%TYPE;
    v_updated_at tb_vaccinations.updated_at%TYPE;

    v_data_anterior DATE;
    v_id_anterior tb_vaccinations.id%TYPE;
    v_quantidade PLS_INTEGER := 0;
BEGIN
    SELECT MIN(animal_id)
      INTO v_animal_id
      FROM tb_vaccinations;

    IF v_animal_id IS NOT NULL THEN
        pkg_clyvo_vacinacao.pr_historico_animal(v_animal_id, v_resultado);

        LOOP
            FETCH v_resultado INTO
                v_id,
                v_row_animal_id,
                v_animal_name,
                v_veterinarian_cpf,
                v_veterinarian_name,
                v_crmv_number,
                v_crmv_state,
                v_vaccine_name,
                v_application_date,
                v_next_dose_date,
                v_batch_number,
                v_manufacturer,
                v_observations,
                v_created_at,
                v_updated_at,
                v_dose_date_status;

            EXIT WHEN v_resultado%NOTFOUND;
            v_quantidade := v_quantidade + 1;

            IF v_row_animal_id <> v_animal_id THEN
                RAISE_APPLICATION_ERROR(-20996, 'Historico retornou outro Animal.');
            END IF;

            IF v_data_anterior IS NOT NULL
               AND (
                   v_application_date > v_data_anterior
                   OR (
                       v_application_date = v_data_anterior
                       AND v_id > v_id_anterior
                   )
               ) THEN
                RAISE_APPLICATION_ERROR(-20997, 'Historico fora da ordem decrescente.');
            END IF;

            IF v_dose_date_status IS NULL
               OR v_dose_date_status <>
                  pkg_clyvo_vacinacao.fn_situacao_data_dose(
                      v_next_dose_date,
                      TRUNC(SYSDATE)
                  ) THEN
                RAISE_APPLICATION_ERROR(-20998, 'Status de dose inconsistente.');
            END IF;

            v_data_anterior := v_application_date;
            v_id_anterior := v_id;
        END LOOP;

        CLOSE v_resultado;

        IF v_quantidade = 0 THEN
            RAISE_APPLICATION_ERROR(-20999, 'Historico existente retornou cursor vazio.');
        END IF;

        DBMS_OUTPUT.PUT_LINE(
            'PR_HISTORICO_ANIMAL: animal=' || v_animal_id ||
            ', registros=' || v_quantidade
        );
    ELSE
        DBMS_OUTPUT.PUT_LINE('PR_HISTORICO_ANIMAL: sem vacinacoes; caso preenchido ignorado.');
    END IF;

    SELECT MIN(animal_record.id)
      INTO v_animal_sem_historico
      FROM animal animal_record
     WHERE NOT EXISTS (
        SELECT 1
          FROM tb_vaccinations vaccination
         WHERE vaccination.animal_id = animal_record.id
     );

    IF v_animal_sem_historico IS NOT NULL THEN
        pkg_clyvo_vacinacao.pr_historico_animal(
            v_animal_sem_historico,
            v_resultado
        );

        FETCH v_resultado INTO
            v_id,
            v_row_animal_id,
            v_animal_name,
            v_veterinarian_cpf,
            v_veterinarian_name,
            v_crmv_number,
            v_crmv_state,
            v_vaccine_name,
            v_application_date,
            v_next_dose_date,
            v_batch_number,
            v_manufacturer,
            v_observations,
            v_created_at,
            v_updated_at,
            v_dose_date_status;

        IF v_resultado%FOUND THEN
            RAISE_APPLICATION_ERROR(-20989, 'Animal sem historico retornou linha.');
        END IF;

        CLOSE v_resultado;
        DBMS_OUTPUT.PUT_LINE('PR_HISTORICO_ANIMAL: cursor vazio: OK');
    ELSE
        DBMS_OUTPUT.PUT_LINE('PR_HISTORICO_ANIMAL: sem Animal vazio; caso ignorado.');
    END IF;

    SELECT NVL(MAX(id), 0) + 1
      INTO v_animal_inexistente
      FROM animal;

    BEGIN
        pkg_clyvo_vacinacao.pr_historico_animal(
            v_animal_inexistente,
            v_resultado
        );
        RAISE_APPLICATION_ERROR(-20988, 'Animal inexistente foi aceito.');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE <> pkg_clyvo_vacinacao.c_err_animal_inexistente THEN
                RAISE;
            END IF;
    END;

    BEGIN
        pkg_clyvo_vacinacao.pr_historico_animal(NULL, v_resultado);
        RAISE_APPLICATION_ERROR(-20987, 'Animal nulo foi aceito.');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE <> pkg_clyvo_vacinacao.c_err_parametro_invalido THEN
                RAISE;
            END IF;
    END;
END;
/

PROMPT ============================================================
PROMPT 5. PR_AGENDA_DOSES_TUTOR
PROMPT ============================================================

DECLARE
    v_tutor_id tutor.id%TYPE;
    v_resultado SYS_REFCURSOR;
    v_referencia CONSTANT DATE := TRUNC(SYSDATE);

    v_id tb_vaccinations.id%TYPE;
    v_animal_id animal.id%TYPE;
    v_animal_name animal.name%TYPE;
    v_veterinarian_cpf veterinarian.cpf%TYPE;
    v_veterinarian_name veterinarian.name%TYPE;
    v_crmv_number veterinarian.crmv_number%TYPE;
    v_crmv_state veterinarian.crmv_state%TYPE;
    v_vaccine_name tb_vaccinations.vaccine_name%TYPE;
    v_application_date tb_vaccinations.application_date%TYPE;
    v_next_dose_date tb_vaccinations.next_dose_date%TYPE;
    v_dose_date_status VARCHAR2(20);
    v_batch_number tb_vaccinations.batch_number%TYPE;
    v_manufacturer tb_vaccinations.manufacturer%TYPE;
    v_observations tb_vaccinations.observations%TYPE;
    v_created_at tb_vaccinations.created_at%TYPE;
    v_updated_at tb_vaccinations.updated_at%TYPE;
    v_doses_previstas PLS_INTEGER;

    PROCEDURE validar_agenda(
        p_filtro          IN VARCHAR2,
        p_status_esperado IN VARCHAR2,
        p_exige_registro  IN BOOLEAN
    ) IS
        v_data_anterior DATE;
        v_nome_anterior animal.name%TYPE;
        v_id_anterior tb_vaccinations.id%TYPE;
        v_quantidade PLS_INTEGER := 0;
    BEGIN
        pkg_clyvo_vacinacao.pr_agenda_doses_tutor(
            v_tutor_id,
            v_referencia,
            p_filtro,
            v_resultado
        );

        LOOP
            FETCH v_resultado INTO
                v_id,
                v_animal_id,
                v_animal_name,
                v_veterinarian_cpf,
                v_veterinarian_name,
                v_crmv_number,
                v_crmv_state,
                v_vaccine_name,
                v_application_date,
                v_next_dose_date,
                v_batch_number,
                v_manufacturer,
                v_observations,
                v_created_at,
                v_updated_at,
                v_dose_date_status;

            EXIT WHEN v_resultado%NOTFOUND;
            v_quantidade := v_quantidade + 1;

            IF v_next_dose_date IS NULL THEN
                RAISE_APPLICATION_ERROR(-20986, 'Agenda retornou dose sem data.');
            END IF;

            IF v_data_anterior IS NOT NULL
               AND (
                   v_next_dose_date < v_data_anterior
                   OR (
                       v_next_dose_date = v_data_anterior
                       AND v_animal_name < v_nome_anterior
                   )
                   OR (
                       v_next_dose_date = v_data_anterior
                       AND v_animal_name = v_nome_anterior
                       AND v_id < v_id_anterior
                   )
               ) THEN
                RAISE_APPLICATION_ERROR(-20985, 'Agenda fora da ordem crescente.');
            END IF;

            IF v_dose_date_status IS NULL
               OR v_dose_date_status <>
                  pkg_clyvo_vacinacao.fn_situacao_data_dose(
                      v_next_dose_date,
                      v_referencia
                  ) THEN
                RAISE_APPLICATION_ERROR(-20984, 'Status da agenda inconsistente.');
            END IF;

            IF p_status_esperado IS NOT NULL
               AND v_dose_date_status <> p_status_esperado THEN
                RAISE_APPLICATION_ERROR(-20980, 'Filtro retornou situacao incorreta.');
            END IF;

            v_data_anterior := v_next_dose_date;
            v_nome_anterior := v_animal_name;
            v_id_anterior := v_id;
        END LOOP;

        CLOSE v_resultado;

        IF p_exige_registro AND v_quantidade = 0 THEN
            RAISE_APPLICATION_ERROR(-20983, 'Agenda esperada retornou cursor vazio.');
        END IF;

        DBMS_OUTPUT.PUT_LINE(
            'PR_AGENDA_DOSES_TUTOR: filtro=' || UPPER(p_filtro) ||
            ', registros=' || v_quantidade
        );
    END validar_agenda;
BEGIN
    SELECT MIN(id)
      INTO v_tutor_id
      FROM tutor;

    IF v_tutor_id IS NOT NULL THEN
        SELECT COUNT(*)
          INTO v_doses_previstas
          FROM tb_vaccinations vaccination
          JOIN animal animal_record
            ON animal_record.id = vaccination.animal_id
         WHERE animal_record.tutor_id = v_tutor_id
           AND vaccination.next_dose_date IS NOT NULL;

        validar_agenda('TODAS', NULL, v_doses_previstas > 0);
        validar_agenda(
            'vencida',
            pkg_clyvo_vacinacao.c_status_prevista_vencida,
            FALSE
        );
        validar_agenda(
            'HOJE',
            pkg_clyvo_vacinacao.c_status_prevista_hoje,
            FALSE
        );
        validar_agenda(
            'FUTURA',
            pkg_clyvo_vacinacao.c_status_prevista_futura,
            FALSE
        );

        BEGIN
            pkg_clyvo_vacinacao.pr_agenda_doses_tutor(
                v_tutor_id,
                v_referencia,
                'INVALIDA',
                v_resultado
            );
            RAISE_APPLICATION_ERROR(-20982, 'Filtro invalido foi aceito.');
        EXCEPTION
            WHEN OTHERS THEN
                IF SQLCODE <> pkg_clyvo_vacinacao.c_err_filtro_invalido THEN
                    RAISE;
                END IF;
        END;

        BEGIN
            pkg_clyvo_vacinacao.pr_agenda_doses_tutor(
                v_tutor_id,
                NULL,
                'TODAS',
                v_resultado
            );
            RAISE_APPLICATION_ERROR(-20981, 'Data de referencia nula foi aceita.');
        EXCEPTION
            WHEN OTHERS THEN
                IF SQLCODE <> pkg_clyvo_vacinacao.c_err_parametro_invalido THEN
                    RAISE;
                END IF;
        END;
    ELSE
        DBMS_OUTPUT.PUT_LINE('PR_AGENDA_DOSES_TUTOR: sem Tutor; casos ignorados.');
    END IF;
END;
/

PROMPT PKG_CLYVO_VACINACAO: VALIDACAO SOMENTE LEITURA CONCLUIDA.
