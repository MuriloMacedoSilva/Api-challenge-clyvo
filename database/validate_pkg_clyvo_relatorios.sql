-- CLYVO - validacao somente leitura de PKG_CLYVO_RELATORIOS.
-- Pre-requisitos: PKG_CLYVO_VACINACAO e PKG_CLYVO_RELATORIOS compilados.
-- Nao executa INSERT, UPDATE, DELETE, MERGE, COMMIT ou ROLLBACK.

SET SERVEROUTPUT ON SIZE UNLIMITED
SET ECHO ON
SET FEEDBACK ON
SET VERIFY OFF
WHENEVER OSERROR EXIT FAILURE
WHENEVER SQLERROR EXIT SQL.SQLCODE

VARIABLE clyvo_rows_before NUMBER

BEGIN
    SELECT (SELECT COUNT(*) FROM tutor)
         + (SELECT COUNT(*) FROM veterinarian)
         + (SELECT COUNT(*) FROM animal)
         + (SELECT COUNT(*) FROM tb_veterinarian_tutor_links)
         + (SELECT COUNT(*) FROM tb_appointments)
         + (SELECT COUNT(*) FROM tb_medical_records)
         + (SELECT COUNT(*) FROM tb_exams)
         + (SELECT COUNT(*) FROM tb_vaccinations)
      INTO :clyvo_rows_before
      FROM dual;
END;
/

PROMPT ============================================================
PROMPT 1. OBJETOS, ERROS, ASSINATURAS E DEPENDENCIAS
PROMPT ============================================================

SELECT object_name, object_type, status, last_ddl_time
  FROM user_objects
 WHERE object_name IN ('PKG_CLYVO_VACINACAO', 'PKG_CLYVO_RELATORIOS')
 ORDER BY object_name, object_type;

SELECT name, type, line, position, text
  FROM user_errors
 WHERE name IN ('PKG_CLYVO_VACINACAO', 'PKG_CLYVO_RELATORIOS')
 ORDER BY name, type, sequence;

SELECT object_name, package_name, argument_name, position, sequence,
       in_out, data_type
  FROM user_arguments
 WHERE package_name = 'PKG_CLYVO_RELATORIOS'
 ORDER BY object_name, overload, sequence;

SELECT name, type, referenced_name, referenced_type, dependency_type
  FROM user_dependencies
 WHERE name = 'PKG_CLYVO_RELATORIOS'
 ORDER BY type, referenced_type, referenced_name;

DECLARE
    v_relatorios_validos PLS_INTEGER;
    v_vacinacao_validos PLS_INTEGER;
    v_dependencia_vacinacao PLS_INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO v_relatorios_validos
      FROM user_objects
     WHERE object_name = 'PKG_CLYVO_RELATORIOS'
       AND object_type IN ('PACKAGE', 'PACKAGE BODY')
       AND status = 'VALID';

    SELECT COUNT(*)
      INTO v_vacinacao_validos
      FROM user_objects
     WHERE object_name = 'PKG_CLYVO_VACINACAO'
       AND object_type IN ('PACKAGE', 'PACKAGE BODY')
       AND status = 'VALID';

    SELECT COUNT(*)
      INTO v_dependencia_vacinacao
      FROM user_dependencies
     WHERE name = 'PKG_CLYVO_RELATORIOS'
       AND type = 'PACKAGE BODY'
       AND referenced_name = 'PKG_CLYVO_VACINACAO'
       AND referenced_type = 'PACKAGE';

    IF v_relatorios_validos <> 2 THEN
        RAISE_APPLICATION_ERROR(-20962, 'Package de relatorios nao esta VALID.');
    END IF;

    IF v_vacinacao_validos <> 2 OR v_dependencia_vacinacao = 0 THEN
        RAISE_APPLICATION_ERROR(-20963, 'Dependencia vacinal ausente ou invalida.');
    END IF;
END;
/

PROMPT ============================================================
PROMPT 2. FUNCTIONS PUBLICAS
PROMPT ============================================================

DECLARE
    v_tutor_id tutor.id%TYPE;
    v_veterinarian_id veterinarian.id%TYPE;
    v_atual NUMBER;
    v_esperado NUMBER;
BEGIN
    SELECT MIN(id) INTO v_tutor_id FROM tutor;

    IF v_tutor_id IS NOT NULL THEN
        v_atual := pkg_clyvo_relatorios.fn_qtd_animais_tutor(v_tutor_id);

        SELECT COUNT(*)
          INTO v_esperado
          FROM animal
         WHERE tutor_id = v_tutor_id;

        IF v_atual IS NULL OR v_atual <> v_esperado THEN
            RAISE_APPLICATION_ERROR(-20964, 'Contagem de animais inconsistente.');
        END IF;

        DBMS_OUTPUT.PUT_LINE(
            'FN_QTD_ANIMAIS_TUTOR: tutor=' || v_tutor_id ||
            ', quantidade=' || v_atual
        );
    ELSE
        DBMS_OUTPUT.PUT_LINE('FN_QTD_ANIMAIS_TUTOR: sem Tutor; caso valido ignorado.');
    END IF;

    SELECT MIN(id) INTO v_veterinarian_id FROM veterinarian;

    IF v_veterinarian_id IS NOT NULL THEN
        v_atual := pkg_clyvo_relatorios.fn_qtd_pacientes_veterinario(
            v_veterinarian_id
        );

        SELECT COUNT(*)
          INTO v_esperado
          FROM animal animal_record
         WHERE EXISTS (
             SELECT 1
               FROM tb_veterinarian_tutor_links link_record
              WHERE link_record.tutor_id = animal_record.tutor_id
                AND link_record.veterinarian_id = v_veterinarian_id
                AND link_record.status = 'ACCEPTED'
         );

        IF v_atual IS NULL OR v_atual <> v_esperado THEN
            RAISE_APPLICATION_ERROR(-20964, 'Contagem de pacientes inconsistente.');
        END IF;

        v_atual := pkg_clyvo_relatorios.fn_qtd_exames_pendentes_veterinario(
            v_veterinarian_id
        );

        SELECT COUNT(*)
          INTO v_esperado
          FROM tb_exams exam
          JOIN tb_medical_records medical_record
            ON medical_record.id = exam.medical_record_id
         WHERE medical_record.veterinarian_id = v_veterinarian_id
           AND exam.status = 'REQUESTED';

        IF v_atual IS NULL OR v_atual <> v_esperado THEN
            RAISE_APPLICATION_ERROR(-20964, 'Contagem de exames inconsistente.');
        END IF;

        DBMS_OUTPUT.PUT_LINE(
            'FUNCTIONS DE VETERINARIO: veterinario=' || v_veterinarian_id ||
            ', exames_pendentes=' || v_atual
        );
    ELSE
        DBMS_OUTPUT.PUT_LINE('FUNCTIONS DE VETERINARIO: sem Veterinario; casos ignorados.');
    END IF;
END;
/

PROMPT ============================================================
PROMPT 3. INDICADORES CONSOLIDADOS DO TUTOR
PROMPT ============================================================

DECLARE
    v_tutor_id tutor.id%TYPE;
    v_referencia CONSTANT TIMESTAMP := TIMESTAMP '2026-11-04 12:34:56';
    v_inicio_dia CONSTANT TIMESTAMP := TIMESTAMP '2026-11-04 00:00:00';
    v_fim_dia CONSTANT TIMESTAMP := TIMESTAMP '2026-11-05 00:00:00';

    v_total_pets NUMBER;
    v_consultas_hoje NUMBER;
    v_consultas_pendentes NUMBER;
    v_veterinarios_vinculados NUMBER;
    v_exames_pendentes NUMBER;
    v_atencoes_vacinais NUMBER;
    v_esperado NUMBER;
BEGIN
    SELECT MIN(id) INTO v_tutor_id FROM tutor;

    IF v_tutor_id IS NULL THEN
        DBMS_OUTPUT.PUT_LINE('PR_INDICADORES_TUTOR: sem Tutor; caso valido ignorado.');
        RETURN;
    END IF;

    pkg_clyvo_relatorios.pr_indicadores_tutor(
        v_tutor_id,
        v_referencia,
        v_total_pets,
        v_consultas_hoje,
        v_consultas_pendentes,
        v_veterinarios_vinculados,
        v_exames_pendentes,
        v_atencoes_vacinais
    );

    SELECT COUNT(*) INTO v_esperado FROM animal WHERE tutor_id = v_tutor_id;
    IF v_total_pets <> v_esperado THEN
        RAISE_APPLICATION_ERROR(-20965, 'TOTAL_PETS inconsistente.');
    END IF;

    SELECT COUNT(*)
      INTO v_esperado
      FROM tb_appointments
     WHERE tutor_id = v_tutor_id
       AND scheduled_at >= v_inicio_dia
       AND scheduled_at < v_fim_dia
       AND status <> 'CANCELLED';
    IF v_consultas_hoje <> v_esperado THEN
        RAISE_APPLICATION_ERROR(-20965, 'CONSULTAS_HOJE do Tutor inconsistente.');
    END IF;

    SELECT COUNT(*)
      INTO v_esperado
      FROM tb_appointments
     WHERE tutor_id = v_tutor_id
       AND status = 'PENDING';
    IF v_consultas_pendentes <> v_esperado THEN
        RAISE_APPLICATION_ERROR(-20965, 'CONSULTAS_PENDENTES do Tutor inconsistente.');
    END IF;

    SELECT COUNT(*)
      INTO v_esperado
      FROM tb_veterinarian_tutor_links
     WHERE tutor_id = v_tutor_id
       AND status = 'ACCEPTED';
    IF v_veterinarios_vinculados <> v_esperado THEN
        RAISE_APPLICATION_ERROR(-20965, 'VETERINARIOS_VINCULADOS inconsistente.');
    END IF;

    SELECT COUNT(*)
      INTO v_esperado
      FROM tb_exams exam
      JOIN tb_medical_records medical_record
        ON medical_record.id = exam.medical_record_id
      JOIN animal animal_record
        ON animal_record.id = medical_record.animal_id
     WHERE animal_record.tutor_id = v_tutor_id
       AND exam.status = 'REQUESTED';
    IF v_exames_pendentes <> v_esperado THEN
        RAISE_APPLICATION_ERROR(-20965, 'EXAMES_PENDENTES do Tutor inconsistente.');
    END IF;

    SELECT COUNT(*)
      INTO v_esperado
      FROM tb_vaccinations vaccination
      JOIN animal animal_record
        ON animal_record.id = vaccination.animal_id
     WHERE animal_record.tutor_id = v_tutor_id
       AND vaccination.next_dose_date IS NOT NULL
       AND vaccination.next_dose_date < DATE '2026-11-05';
    IF v_atencoes_vacinais <> v_esperado THEN
        RAISE_APPLICATION_ERROR(-20965, 'ATENCOES_VACINAIS inconsistente.');
    END IF;

    DBMS_OUTPUT.PUT_LINE('PR_INDICADORES_TUTOR: OK para tutor=' || v_tutor_id);
END;
/

PROMPT ============================================================
PROMPT 4. INDICADORES CONSOLIDADOS DO VETERINARIO
PROMPT ============================================================

DECLARE
    v_veterinarian_id veterinarian.id%TYPE;
    v_referencia CONSTANT TIMESTAMP := TIMESTAMP '2026-11-04 12:34:56';
    v_inicio_dia CONSTANT TIMESTAMP := TIMESTAMP '2026-11-04 00:00:00';
    v_fim_dia CONSTANT TIMESTAMP := TIMESTAMP '2026-11-05 00:00:00';

    v_tutores_vinculados NUMBER;
    v_pacientes_ativos NUMBER;
    v_consultas_hoje NUMBER;
    v_consultas_pendentes NUMBER;
    v_consultas_confirmadas NUMBER;
    v_consultas_concluidas NUMBER;
    v_exames_pendentes NUMBER;
    v_esperado NUMBER;
BEGIN
    SELECT MIN(id) INTO v_veterinarian_id FROM veterinarian;

    IF v_veterinarian_id IS NULL THEN
        DBMS_OUTPUT.PUT_LINE('PR_INDICADORES_VETERINARIO: sem Veterinario; caso ignorado.');
        RETURN;
    END IF;

    pkg_clyvo_relatorios.pr_indicadores_veterinario(
        v_veterinarian_id,
        v_referencia,
        v_tutores_vinculados,
        v_pacientes_ativos,
        v_consultas_hoje,
        v_consultas_pendentes,
        v_consultas_confirmadas,
        v_consultas_concluidas,
        v_exames_pendentes
    );

    SELECT COUNT(*)
      INTO v_esperado
      FROM tb_veterinarian_tutor_links
     WHERE veterinarian_id = v_veterinarian_id
       AND status = 'ACCEPTED';
    IF v_tutores_vinculados <> v_esperado THEN
        RAISE_APPLICATION_ERROR(-20966, 'TUTORES_VINCULADOS inconsistente.');
    END IF;

    SELECT COUNT(*)
      INTO v_esperado
      FROM animal animal_record
     WHERE EXISTS (
         SELECT 1
           FROM tb_veterinarian_tutor_links link_record
          WHERE link_record.tutor_id = animal_record.tutor_id
            AND link_record.veterinarian_id = v_veterinarian_id
            AND link_record.status = 'ACCEPTED'
     );
    IF v_pacientes_ativos <> v_esperado THEN
        RAISE_APPLICATION_ERROR(-20966, 'PACIENTES_ATIVOS inconsistente.');
    END IF;

    SELECT COUNT(*)
      INTO v_esperado
      FROM tb_appointments
     WHERE veterinarian_id = v_veterinarian_id
       AND scheduled_at >= v_inicio_dia
       AND scheduled_at < v_fim_dia
       AND status <> 'CANCELLED';
    IF v_consultas_hoje <> v_esperado THEN
        RAISE_APPLICATION_ERROR(-20966, 'CONSULTAS_HOJE do Veterinario inconsistente.');
    END IF;

    SELECT COUNT(*) INTO v_esperado FROM tb_appointments
     WHERE veterinarian_id = v_veterinarian_id AND status = 'PENDING';
    IF v_consultas_pendentes <> v_esperado THEN
        RAISE_APPLICATION_ERROR(-20966, 'CONSULTAS_PENDENTES inconsistente.');
    END IF;

    SELECT COUNT(*) INTO v_esperado FROM tb_appointments
     WHERE veterinarian_id = v_veterinarian_id AND status = 'CONFIRMED';
    IF v_consultas_confirmadas <> v_esperado THEN
        RAISE_APPLICATION_ERROR(-20966, 'CONSULTAS_CONFIRMADAS inconsistente.');
    END IF;

    SELECT COUNT(*) INTO v_esperado FROM tb_appointments
     WHERE veterinarian_id = v_veterinarian_id AND status = 'COMPLETED';
    IF v_consultas_concluidas <> v_esperado THEN
        RAISE_APPLICATION_ERROR(-20966, 'CONSULTAS_CONCLUIDAS inconsistente.');
    END IF;

    SELECT COUNT(*)
      INTO v_esperado
      FROM tb_exams exam
      JOIN tb_medical_records medical_record
        ON medical_record.id = exam.medical_record_id
     WHERE medical_record.veterinarian_id = v_veterinarian_id
       AND exam.status = 'REQUESTED';
    IF v_exames_pendentes <> v_esperado THEN
        RAISE_APPLICATION_ERROR(-20966, 'EXAMES_PENDENTES do Veterinario inconsistente.');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'PR_INDICADORES_VETERINARIO: OK para veterinario=' || v_veterinarian_id
    );
END;
/

PROMPT ============================================================
PROMPT 5. CONTRATO DOS CURSORES
PROMPT ============================================================

DECLARE
    v_tutor_id tutor.id%TYPE;
    v_veterinarian_id veterinarian.id%TYPE;
    v_resultado SYS_REFCURSOR;
    v_cursor_number INTEGER := NULL;
    v_ref_cursor_aberto BOOLEAN := FALSE;
    v_tutor_validado BOOLEAN := FALSE;
    v_veterinario_validado BOOLEAN := FALSE;
    v_colunas PLS_INTEGER;
    v_descricao DBMS_SQL.DESC_TAB2;

    PROCEDURE assert_alias(
        p_posicao IN PLS_INTEGER,
        p_alias   IN VARCHAR2
    ) IS
    BEGIN
        IF v_descricao(p_posicao).col_name <> p_alias THEN
            RAISE_APPLICATION_ERROR(
                -20967,
                'Alias na posicao ' || p_posicao || ': esperado=' || p_alias ||
                ', atual=' || v_descricao(p_posicao).col_name
            );
        END IF;
    END assert_alias;

    PROCEDURE validar_descricao IS
    BEGIN
        DBMS_SQL.DESCRIBE_COLUMNS2(v_cursor_number, v_colunas, v_descricao);

        IF v_colunas <> 12 THEN
            RAISE_APPLICATION_ERROR(-20967, 'Cursor nao retornou 12 colunas.');
        END IF;

        assert_alias(1, 'APPOINTMENT_ID');
        assert_alias(2, 'SCHEDULED_AT');
        assert_alias(3, 'REASON');
        assert_alias(4, 'STATUS');
        assert_alias(5, 'CREATED_AT');
        assert_alias(6, 'UPDATED_AT');
        assert_alias(7, 'ANIMAL_ID');
        assert_alias(8, 'ANIMAL_NAME');
        assert_alias(9, 'TUTOR_CPF');
        assert_alias(10, 'TUTOR_NAME');
        assert_alias(11, 'VETERINARIAN_CPF');
        assert_alias(12, 'VETERINARIAN_NAME');
    END validar_descricao;
BEGIN
    SELECT MIN(id) INTO v_tutor_id FROM tutor;
    IF v_tutor_id IS NOT NULL THEN
        pkg_clyvo_relatorios.pr_consultas_tutor_periodo(
            v_tutor_id,
            TIMESTAMP '1900-01-01 00:00:00',
            TIMESTAMP '2999-01-01 00:00:00',
            v_resultado
        );
        v_ref_cursor_aberto := TRUE;
        v_cursor_number := DBMS_SQL.TO_CURSOR_NUMBER(v_resultado);
        v_ref_cursor_aberto := FALSE;
        validar_descricao;
        DBMS_SQL.CLOSE_CURSOR(v_cursor_number);
        v_cursor_number := NULL;
        v_tutor_validado := TRUE;
    END IF;

    SELECT MIN(id) INTO v_veterinarian_id FROM veterinarian;
    IF v_veterinarian_id IS NOT NULL THEN
        pkg_clyvo_relatorios.pr_consultas_veterinario_periodo(
            v_veterinarian_id,
            TIMESTAMP '1900-01-01 00:00:00',
            TIMESTAMP '2999-01-01 00:00:00',
            v_resultado
        );
        v_ref_cursor_aberto := TRUE;
        v_cursor_number := DBMS_SQL.TO_CURSOR_NUMBER(v_resultado);
        v_ref_cursor_aberto := FALSE;
        validar_descricao;
        DBMS_SQL.CLOSE_CURSOR(v_cursor_number);
        v_cursor_number := NULL;
        v_veterinario_validado := TRUE;
    END IF;

    IF v_tutor_validado THEN
        DBMS_OUTPUT.PUT_LINE('CONTRATO DO CURSOR DE TUTOR: OK');
    ELSE
        DBMS_OUTPUT.PUT_LINE('CONTRATO DO CURSOR DE TUTOR: sem Tutor; ignorado.');
    END IF;

    IF v_veterinario_validado THEN
        DBMS_OUTPUT.PUT_LINE('CONTRATO DO CURSOR DE VETERINARIO: OK');
    ELSE
        DBMS_OUTPUT.PUT_LINE('CONTRATO DO CURSOR DE VETERINARIO: sem Veterinario; ignorado.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        IF v_cursor_number IS NOT NULL AND DBMS_SQL.IS_OPEN(v_cursor_number) THEN
            DBMS_SQL.CLOSE_CURSOR(v_cursor_number);
        END IF;
        IF v_ref_cursor_aberto THEN
            CLOSE v_resultado;
        END IF;
        RAISE;
END;
/

PROMPT ============================================================
PROMPT 6. CONSULTAS DO TUTOR POR PERIODO E CURSOR VAZIO
PROMPT ============================================================

DECLARE
    v_tutor_id tutor.id%TYPE;
    v_tutor_sem_consulta tutor.id%TYPE;
    v_resultado SYS_REFCURSOR;
    v_inicio CONSTANT TIMESTAMP := TIMESTAMP '1900-01-01 00:00:00';
    v_fim CONSTANT TIMESTAMP := TIMESTAMP '2999-01-01 00:00:00';

    v_appointment_id tb_appointments.id%TYPE;
    v_scheduled_at tb_appointments.scheduled_at%TYPE;
    v_reason tb_appointments.reason%TYPE;
    v_status tb_appointments.status%TYPE;
    v_created_at tb_appointments.created_at%TYPE;
    v_updated_at tb_appointments.updated_at%TYPE;
    v_animal_id animal.id%TYPE;
    v_animal_name animal.name%TYPE;
    v_tutor_cpf tutor.cpf%TYPE;
    v_tutor_name tutor.name%TYPE;
    v_veterinarian_cpf veterinarian.cpf%TYPE;
    v_veterinarian_name veterinarian.name%TYPE;

    v_data_anterior tb_appointments.scheduled_at%TYPE;
    v_id_anterior tb_appointments.id%TYPE;
    v_quantidade PLS_INTEGER := 0;
    v_esperado PLS_INTEGER;
    v_pertence PLS_INTEGER;
    v_cursor_aberto BOOLEAN := FALSE;
BEGIN
    SELECT MIN(id) INTO v_tutor_id FROM tutor;
    IF v_tutor_id IS NULL THEN
        DBMS_OUTPUT.PUT_LINE('CURSOR DO TUTOR: sem Tutor; casos ignorados.');
        RETURN;
    END IF;

    SELECT COUNT(*)
      INTO v_esperado
      FROM tb_appointments
     WHERE tutor_id = v_tutor_id
       AND scheduled_at >= v_inicio
       AND scheduled_at < v_fim;

    pkg_clyvo_relatorios.pr_consultas_tutor_periodo(
        v_tutor_id, v_inicio, v_fim, v_resultado
    );
    v_cursor_aberto := TRUE;

    LOOP
        FETCH v_resultado INTO
            v_appointment_id, v_scheduled_at, v_reason, v_status,
            v_created_at, v_updated_at, v_animal_id, v_animal_name,
            v_tutor_cpf, v_tutor_name, v_veterinarian_cpf,
            v_veterinarian_name;
        EXIT WHEN v_resultado%NOTFOUND;
        v_quantidade := v_quantidade + 1;

        SELECT COUNT(*)
          INTO v_pertence
          FROM tb_appointments
         WHERE id = v_appointment_id
           AND tutor_id = v_tutor_id
           AND scheduled_at >= v_inicio
           AND scheduled_at < v_fim;

        IF v_pertence <> 1 THEN
            RAISE_APPLICATION_ERROR(-20968, 'Cursor retornou consulta fora do Tutor/periodo.');
        END IF;

        IF v_data_anterior IS NOT NULL AND (
            v_scheduled_at < v_data_anterior OR
            (v_scheduled_at = v_data_anterior AND v_appointment_id < v_id_anterior)
        ) THEN
            RAISE_APPLICATION_ERROR(-20968, 'Cursor do Tutor fora da ordem crescente.');
        END IF;

        v_data_anterior := v_scheduled_at;
        v_id_anterior := v_appointment_id;
    END LOOP;
    CLOSE v_resultado;
    v_cursor_aberto := FALSE;

    IF v_quantidade <> v_esperado THEN
        RAISE_APPLICATION_ERROR(-20968, 'Quantidade do cursor do Tutor inconsistente.');
    END IF;

    SELECT MIN(tutor_record.id)
      INTO v_tutor_sem_consulta
      FROM tutor tutor_record
     WHERE NOT EXISTS (
         SELECT 1
           FROM tb_appointments appointment
          WHERE appointment.tutor_id = tutor_record.id
     );

    IF v_tutor_sem_consulta IS NOT NULL THEN
        pkg_clyvo_relatorios.pr_consultas_tutor_periodo(
            v_tutor_sem_consulta, v_inicio, v_fim, v_resultado
        );
        v_cursor_aberto := TRUE;
        FETCH v_resultado INTO
            v_appointment_id, v_scheduled_at, v_reason, v_status,
            v_created_at, v_updated_at, v_animal_id, v_animal_name,
            v_tutor_cpf, v_tutor_name, v_veterinarian_cpf,
            v_veterinarian_name;
        IF v_resultado%FOUND THEN
            RAISE_APPLICATION_ERROR(-20970, 'Cursor vazio do Tutor retornou linha.');
        END IF;
        CLOSE v_resultado;
        v_cursor_aberto := FALSE;
        DBMS_OUTPUT.PUT_LINE('CURSOR VAZIO DO TUTOR: OK');
    ELSE
        DBMS_OUTPUT.PUT_LINE('CURSOR VAZIO DO TUTOR: sem Tutor elegivel; ignorado.');
    END IF;

    DBMS_OUTPUT.PUT_LINE('CURSOR DO TUTOR: registros=' || v_quantidade);
EXCEPTION
    WHEN OTHERS THEN
        IF v_cursor_aberto THEN
            CLOSE v_resultado;
        END IF;
        RAISE;
END;
/

PROMPT ============================================================
PROMPT 7. CONSULTAS DO VETERINARIO POR PERIODO E CURSOR VAZIO
PROMPT ============================================================

DECLARE
    v_veterinarian_id veterinarian.id%TYPE;
    v_veterinario_sem_consulta veterinarian.id%TYPE;
    v_resultado SYS_REFCURSOR;
    v_inicio CONSTANT TIMESTAMP := TIMESTAMP '1900-01-01 00:00:00';
    v_fim CONSTANT TIMESTAMP := TIMESTAMP '2999-01-01 00:00:00';

    v_appointment_id tb_appointments.id%TYPE;
    v_scheduled_at tb_appointments.scheduled_at%TYPE;
    v_reason tb_appointments.reason%TYPE;
    v_status tb_appointments.status%TYPE;
    v_created_at tb_appointments.created_at%TYPE;
    v_updated_at tb_appointments.updated_at%TYPE;
    v_animal_id animal.id%TYPE;
    v_animal_name animal.name%TYPE;
    v_tutor_cpf tutor.cpf%TYPE;
    v_tutor_name tutor.name%TYPE;
    v_veterinarian_cpf veterinarian.cpf%TYPE;
    v_veterinarian_name veterinarian.name%TYPE;

    v_data_anterior tb_appointments.scheduled_at%TYPE;
    v_id_anterior tb_appointments.id%TYPE;
    v_quantidade PLS_INTEGER := 0;
    v_esperado PLS_INTEGER;
    v_pertence PLS_INTEGER;
    v_cursor_aberto BOOLEAN := FALSE;
BEGIN
    SELECT MIN(id) INTO v_veterinarian_id FROM veterinarian;
    IF v_veterinarian_id IS NULL THEN
        DBMS_OUTPUT.PUT_LINE('CURSOR DO VETERINARIO: sem Veterinario; casos ignorados.');
        RETURN;
    END IF;

    SELECT COUNT(*)
      INTO v_esperado
      FROM tb_appointments
     WHERE veterinarian_id = v_veterinarian_id
       AND scheduled_at >= v_inicio
       AND scheduled_at < v_fim;

    pkg_clyvo_relatorios.pr_consultas_veterinario_periodo(
        v_veterinarian_id, v_inicio, v_fim, v_resultado
    );
    v_cursor_aberto := TRUE;

    LOOP
        FETCH v_resultado INTO
            v_appointment_id, v_scheduled_at, v_reason, v_status,
            v_created_at, v_updated_at, v_animal_id, v_animal_name,
            v_tutor_cpf, v_tutor_name, v_veterinarian_cpf,
            v_veterinarian_name;
        EXIT WHEN v_resultado%NOTFOUND;
        v_quantidade := v_quantidade + 1;

        SELECT COUNT(*)
          INTO v_pertence
          FROM tb_appointments
         WHERE id = v_appointment_id
           AND veterinarian_id = v_veterinarian_id
           AND scheduled_at >= v_inicio
           AND scheduled_at < v_fim;

        IF v_pertence <> 1 THEN
            RAISE_APPLICATION_ERROR(-20969, 'Cursor retornou consulta fora do Veterinario/periodo.');
        END IF;

        IF v_data_anterior IS NOT NULL AND (
            v_scheduled_at < v_data_anterior OR
            (v_scheduled_at = v_data_anterior AND v_appointment_id < v_id_anterior)
        ) THEN
            RAISE_APPLICATION_ERROR(-20969, 'Cursor do Veterinario fora da ordem crescente.');
        END IF;

        v_data_anterior := v_scheduled_at;
        v_id_anterior := v_appointment_id;
    END LOOP;
    CLOSE v_resultado;
    v_cursor_aberto := FALSE;

    IF v_quantidade <> v_esperado THEN
        RAISE_APPLICATION_ERROR(-20969, 'Quantidade do cursor do Veterinario inconsistente.');
    END IF;

    SELECT MIN(veterinarian_record.id)
      INTO v_veterinario_sem_consulta
      FROM veterinarian veterinarian_record
     WHERE NOT EXISTS (
         SELECT 1
           FROM tb_appointments appointment
          WHERE appointment.veterinarian_id = veterinarian_record.id
     );

    IF v_veterinario_sem_consulta IS NOT NULL THEN
        pkg_clyvo_relatorios.pr_consultas_veterinario_periodo(
            v_veterinario_sem_consulta, v_inicio, v_fim, v_resultado
        );
        v_cursor_aberto := TRUE;
        FETCH v_resultado INTO
            v_appointment_id, v_scheduled_at, v_reason, v_status,
            v_created_at, v_updated_at, v_animal_id, v_animal_name,
            v_tutor_cpf, v_tutor_name, v_veterinarian_cpf,
            v_veterinarian_name;
        IF v_resultado%FOUND THEN
            RAISE_APPLICATION_ERROR(-20970, 'Cursor vazio do Veterinario retornou linha.');
        END IF;
        CLOSE v_resultado;
        v_cursor_aberto := FALSE;
        DBMS_OUTPUT.PUT_LINE('CURSOR VAZIO DO VETERINARIO: OK');
    ELSE
        DBMS_OUTPUT.PUT_LINE(
            'CURSOR VAZIO DO VETERINARIO: sem Veterinario elegivel; ignorado.'
        );
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'CURSOR DO VETERINARIO: registros=' || v_quantidade
    );
EXCEPTION
    WHEN OTHERS THEN
        IF v_cursor_aberto THEN
            CLOSE v_resultado;
        END IF;
        RAISE;
END;
/

PROMPT ============================================================
PROMPT 8. TESTES NEGATIVOS DE IDS E RECURSOS INEXISTENTES
PROMPT ============================================================

DECLARE
    v_quantidade NUMBER;
    v_tutor_inexistente tutor.id%TYPE;
    v_veterinarian_inexistente veterinarian.id%TYPE;
BEGIN
    SELECT NVL(MAX(id), 0) + 1 INTO v_tutor_inexistente FROM tutor;
    SELECT NVL(MAX(id), 0) + 1 INTO v_veterinarian_inexistente FROM veterinarian;

    BEGIN
        v_quantidade := pkg_clyvo_relatorios.fn_qtd_animais_tutor(NULL);
        RAISE_APPLICATION_ERROR(-20971, 'ID nulo foi aceito.');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE <> pkg_clyvo_relatorios.c_err_parametro_invalido THEN
                RAISE;
            END IF;
    END;

    BEGIN
        v_quantidade := pkg_clyvo_relatorios.fn_qtd_animais_tutor(0);
        RAISE_APPLICATION_ERROR(-20971, 'ID zero foi aceito.');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE <> pkg_clyvo_relatorios.c_err_parametro_invalido THEN
                RAISE;
            END IF;
    END;

    BEGIN
        v_quantidade := pkg_clyvo_relatorios.fn_qtd_pacientes_veterinario(1.5);
        RAISE_APPLICATION_ERROR(-20971, 'ID fracionario foi aceito.');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE <> pkg_clyvo_relatorios.c_err_parametro_invalido THEN
                RAISE;
            END IF;
    END;

    BEGIN
        v_quantidade := pkg_clyvo_relatorios.fn_qtd_animais_tutor(
            v_tutor_inexistente
        );
        RAISE_APPLICATION_ERROR(-20972, 'Tutor inexistente foi aceito.');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE <> pkg_clyvo_relatorios.c_err_tutor_inexistente THEN
                RAISE;
            END IF;
    END;

    BEGIN
        v_quantidade := pkg_clyvo_relatorios.fn_qtd_pacientes_veterinario(
            v_veterinarian_inexistente
        );
        RAISE_APPLICATION_ERROR(-20972, 'Veterinario inexistente foi aceito.');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE <> pkg_clyvo_relatorios.c_err_veterinario_inexistente THEN
                RAISE;
            END IF;
    END;

    DBMS_OUTPUT.PUT_LINE('IDS INVALIDOS E INEXISTENTES: OK');
END;
/

PROMPT ============================================================
PROMPT 9. TESTES NEGATIVOS DE DATAS E INTERVALOS
PROMPT ============================================================

DECLARE
    v_tutor_id tutor.id%TYPE;
    v_veterinarian_id veterinarian.id%TYPE;
    v_resultado SYS_REFCURSOR;
    v_total_pets NUMBER;
    v_consultas_hoje NUMBER;
    v_consultas_pendentes NUMBER;
    v_veterinarios_vinculados NUMBER;
    v_exames_pendentes NUMBER;
    v_atencoes_vacinais NUMBER;
BEGIN
    SELECT MIN(id) INTO v_tutor_id FROM tutor;
    SELECT MIN(id) INTO v_veterinarian_id FROM veterinarian;

    IF v_tutor_id IS NOT NULL THEN
        BEGIN
            pkg_clyvo_relatorios.pr_indicadores_tutor(
                v_tutor_id, NULL, v_total_pets, v_consultas_hoje,
                v_consultas_pendentes, v_veterinarios_vinculados,
                v_exames_pendentes, v_atencoes_vacinais
            );
            RAISE_APPLICATION_ERROR(-20973, 'Referencia nula foi aceita.');
        EXCEPTION
            WHEN OTHERS THEN
                IF SQLCODE <> pkg_clyvo_relatorios.c_err_parametro_obrigatorio THEN
                    RAISE;
                END IF;
        END;

        BEGIN
            pkg_clyvo_relatorios.pr_consultas_tutor_periodo(
                v_tutor_id, NULL, TIMESTAMP '2026-11-05 00:00:00', v_resultado
            );
            RAISE_APPLICATION_ERROR(-20973, 'Inicio nulo foi aceito.');
        EXCEPTION
            WHEN OTHERS THEN
                IF SQLCODE <> pkg_clyvo_relatorios.c_err_parametro_obrigatorio THEN
                    RAISE;
                END IF;
        END;

        BEGIN
            pkg_clyvo_relatorios.pr_consultas_tutor_periodo(
                v_tutor_id, TIMESTAMP '2026-11-04 00:00:00', NULL, v_resultado
            );
            RAISE_APPLICATION_ERROR(-20973, 'Fim nulo foi aceito.');
        EXCEPTION
            WHEN OTHERS THEN
                IF SQLCODE <> pkg_clyvo_relatorios.c_err_parametro_obrigatorio THEN
                    RAISE;
                END IF;
        END;

        BEGIN
            pkg_clyvo_relatorios.pr_consultas_tutor_periodo(
                v_tutor_id,
                TIMESTAMP '2026-11-05 00:00:00',
                TIMESTAMP '2026-11-04 00:00:00',
                v_resultado
            );
            RAISE_APPLICATION_ERROR(-20974, 'Intervalo invertido foi aceito.');
        EXCEPTION
            WHEN OTHERS THEN
                IF SQLCODE <> pkg_clyvo_relatorios.c_err_intervalo_invalido THEN
                    RAISE;
                END IF;
        END;
    END IF;

    IF v_veterinarian_id IS NOT NULL THEN
        BEGIN
            pkg_clyvo_relatorios.pr_consultas_veterinario_periodo(
                v_veterinarian_id,
                TIMESTAMP '2026-11-04 00:00:00',
                TIMESTAMP '2026-11-04 00:00:00',
                v_resultado
            );
            RAISE_APPLICATION_ERROR(-20974, 'Intervalo vazio foi aceito.');
        EXCEPTION
            WHEN OTHERS THEN
                IF SQLCODE <> pkg_clyvo_relatorios.c_err_intervalo_invalido THEN
                    RAISE;
                END IF;
        END;
    END IF;

    DBMS_OUTPUT.PUT_LINE('DATAS E INTERVALOS INVALIDOS: OK');
END;
/

PROMPT ============================================================
PROMPT 10. CONSISTENCIA COM PKG_CLYVO_VACINACAO
PROMPT ============================================================

DECLARE
    v_tutor_id tutor.id%TYPE;
    v_referencia CONSTANT TIMESTAMP := TIMESTAMP '2026-11-04 12:34:56';
    v_total_pets NUMBER;
    v_consultas_hoje NUMBER;
    v_consultas_pendentes NUMBER;
    v_veterinarios_vinculados NUMBER;
    v_exames_pendentes NUMBER;
    v_atencoes_relatorios NUMBER;
    v_atencoes_vacinacao NUMBER;
BEGIN
    SELECT MIN(id) INTO v_tutor_id FROM tutor;

    IF v_tutor_id IS NULL THEN
        DBMS_OUTPUT.PUT_LINE('CONSISTENCIA VACINAL: sem Tutor; caso ignorado.');
        RETURN;
    END IF;

    pkg_clyvo_relatorios.pr_indicadores_tutor(
        v_tutor_id, v_referencia, v_total_pets, v_consultas_hoje,
        v_consultas_pendentes, v_veterinarios_vinculados,
        v_exames_pendentes, v_atencoes_relatorios
    );

    v_atencoes_vacinacao := pkg_clyvo_vacinacao.fn_qtd_atencoes_tutor(
        v_tutor_id,
        CAST(v_referencia AS DATE)
    );

    IF v_atencoes_relatorios <> v_atencoes_vacinacao THEN
        RAISE_APPLICATION_ERROR(-20975, 'Contagem vacinal divergiu entre packages.');
    END IF;

    DBMS_OUTPUT.PUT_LINE('CONSISTENCIA COM PKG_CLYVO_VACINACAO: OK');
END;
/

PROMPT ============================================================
PROMPT 11. INDICIOS ESTATICOS DE LEITURA E CONTAGEM DE LINHAS
PROMPT ============================================================

DECLARE
    v_comandos_proibidos PLS_INTEGER;
    v_rows_after NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_comandos_proibidos
      FROM user_source
     WHERE name = 'PKG_CLYVO_RELATORIOS'
       AND type IN ('PACKAGE', 'PACKAGE BODY')
       AND REGEXP_LIKE(
           UPPER(text),
           '(^|[^A-Z])(INSERT|UPDATE|DELETE|MERGE|COMMIT|ROLLBACK)([^A-Z]|$)|EXECUTE[[:space:]]+IMMEDIATE|AUTONOMOUS_TRANSACTION'
       );

    IF v_comandos_proibidos <> 0 THEN
        RAISE_APPLICATION_ERROR(-20976, 'Package contem comando incompativel com leitura.');
    END IF;

    SELECT (SELECT COUNT(*) FROM tutor)
         + (SELECT COUNT(*) FROM veterinarian)
         + (SELECT COUNT(*) FROM animal)
         + (SELECT COUNT(*) FROM tb_veterinarian_tutor_links)
         + (SELECT COUNT(*) FROM tb_appointments)
         + (SELECT COUNT(*) FROM tb_medical_records)
         + (SELECT COUNT(*) FROM tb_exams)
         + (SELECT COUNT(*) FROM tb_vaccinations)
      INTO v_rows_after
      FROM dual;

    IF v_rows_after <> :clyvo_rows_before THEN
        RAISE_APPLICATION_ERROR(-20977, 'Quantidade de linhas mudou durante a validacao.');
    END IF;

    DBMS_OUTPUT.PUT_LINE('SOURCE SEM COMANDOS MUTAVEIS LITERAIS E CONTAGENS PRESERVADAS: OK');
END;
/

PROMPT PKG_CLYVO_RELATORIOS: VALIDACAO CONCLUIDA; REVISAR CASOS INFORMADOS COMO IGNORADOS.
