-- CLYVO - body do package Oracle 19c de relatorios.
-- Depende das tabelas operacionais e de PKG_CLYVO_VACINACAO.

SET ECHO ON
SET FEEDBACK ON
SET DEFINE OFF
WHENEVER OSERROR EXIT FAILURE
WHENEVER SQLERROR EXIT SQL.SQLCODE

CREATE OR REPLACE PACKAGE BODY pkg_clyvo_relatorios AS

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

    PROCEDURE validar_instante_referencia(
        p_instante_referencia IN TIMESTAMP
    ) IS
    BEGIN
        IF p_instante_referencia IS NULL THEN
            RAISE_APPLICATION_ERROR(
                c_err_parametro_obrigatorio,
                'Instante de referencia e obrigatorio.'
            );
        END IF;
    END validar_instante_referencia;

    PROCEDURE validar_intervalo(
        p_inicio IN TIMESTAMP,
        p_fim    IN TIMESTAMP
    ) IS
    BEGIN
        IF p_inicio IS NULL OR p_fim IS NULL THEN
            RAISE_APPLICATION_ERROR(
                c_err_parametro_obrigatorio,
                'Inicio e fim do periodo sao obrigatorios.'
            );
        END IF;

        IF p_inicio >= p_fim THEN
            RAISE_APPLICATION_ERROR(
                c_err_intervalo_invalido,
                'Inicio do periodo deve ser anterior ao fim.'
            );
        END IF;
    END validar_intervalo;

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

    PROCEDURE validar_veterinario_existente(
        p_veterinarian_id IN NUMBER
    ) IS
        v_encontrado PLS_INTEGER;
    BEGIN
        validar_id(p_veterinarian_id, 'Veterinario');

        SELECT 1
          INTO v_encontrado
          FROM veterinarian
         WHERE id = p_veterinarian_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(
                c_err_veterinario_inexistente,
                'Veterinario nao encontrado.'
            );
    END validar_veterinario_existente;

    FUNCTION contar_animais_tutor(
        p_tutor_id IN NUMBER
    ) RETURN NUMBER IS
        v_quantidade NUMBER;
    BEGIN
        SELECT COUNT(*)
          INTO v_quantidade
          FROM animal animal_record
         WHERE animal_record.tutor_id = p_tutor_id;

        RETURN v_quantidade;
    END contar_animais_tutor;

    FUNCTION contar_pacientes_veterinario(
        p_veterinarian_id IN NUMBER
    ) RETURN NUMBER IS
        v_quantidade NUMBER;
    BEGIN
        SELECT COUNT(*)
          INTO v_quantidade
          FROM animal animal_record
         WHERE EXISTS (
             SELECT 1
               FROM tb_veterinarian_tutor_links link_record
              WHERE link_record.tutor_id = animal_record.tutor_id
                AND link_record.veterinarian_id = p_veterinarian_id
                AND link_record.status = 'ACCEPTED'
         );

        RETURN v_quantidade;
    END contar_pacientes_veterinario;

    FUNCTION contar_exames_pendentes_veterinario(
        p_veterinarian_id IN NUMBER
    ) RETURN NUMBER IS
        v_quantidade NUMBER;
    BEGIN
        SELECT COUNT(*)
          INTO v_quantidade
          FROM tb_exams exam
          JOIN tb_medical_records medical_record
            ON medical_record.id = exam.medical_record_id
         WHERE medical_record.veterinarian_id = p_veterinarian_id
           AND exam.status = 'REQUESTED';

        RETURN v_quantidade;
    END contar_exames_pendentes_veterinario;

    FUNCTION fn_qtd_animais_tutor(
        p_tutor_id IN NUMBER
    ) RETURN NUMBER IS
    BEGIN
        validar_tutor_existente(p_tutor_id);
        RETURN contar_animais_tutor(p_tutor_id);
    END fn_qtd_animais_tutor;

    FUNCTION fn_qtd_pacientes_veterinario(
        p_veterinarian_id IN NUMBER
    ) RETURN NUMBER IS
    BEGIN
        validar_veterinario_existente(p_veterinarian_id);
        RETURN contar_pacientes_veterinario(p_veterinarian_id);
    END fn_qtd_pacientes_veterinario;

    FUNCTION fn_qtd_exames_pendentes_veterinario(
        p_veterinarian_id IN NUMBER
    ) RETURN NUMBER IS
    BEGIN
        validar_veterinario_existente(p_veterinarian_id);
        RETURN contar_exames_pendentes_veterinario(p_veterinarian_id);
    END fn_qtd_exames_pendentes_veterinario;

    PROCEDURE pr_indicadores_tutor(
        p_tutor_id                 IN NUMBER,
        p_instante_referencia      IN TIMESTAMP,
        p_total_pets               OUT NUMBER,
        p_consultas_hoje           OUT NUMBER,
        p_consultas_pendentes      OUT NUMBER,
        p_veterinarios_vinculados  OUT NUMBER,
        p_exames_pendentes         OUT NUMBER,
        p_atencoes_vacinais        OUT NUMBER
    ) IS
        v_inicio_dia TIMESTAMP;
        v_fim_dia TIMESTAMP;
    BEGIN
        validar_tutor_existente(p_tutor_id);
        validar_instante_referencia(p_instante_referencia);

        v_inicio_dia := CAST(
            TRUNC(CAST(p_instante_referencia AS DATE)) AS TIMESTAMP
        );
        v_fim_dia := v_inicio_dia + INTERVAL '1' DAY;

        p_total_pets := contar_animais_tutor(p_tutor_id);

        SELECT COUNT(*)
          INTO p_consultas_hoje
          FROM tb_appointments appointment
         WHERE appointment.tutor_id = p_tutor_id
           AND appointment.scheduled_at >= v_inicio_dia
           AND appointment.scheduled_at < v_fim_dia
           AND appointment.status <> 'CANCELLED';

        SELECT COUNT(*)
          INTO p_consultas_pendentes
          FROM tb_appointments appointment
         WHERE appointment.tutor_id = p_tutor_id
           AND appointment.status = 'PENDING';

        SELECT COUNT(*)
          INTO p_veterinarios_vinculados
          FROM tb_veterinarian_tutor_links link_record
         WHERE link_record.tutor_id = p_tutor_id
           AND link_record.status = 'ACCEPTED';

        SELECT COUNT(*)
          INTO p_exames_pendentes
          FROM tb_exams exam
          JOIN tb_medical_records medical_record
            ON medical_record.id = exam.medical_record_id
          JOIN animal animal_record
            ON animal_record.id = medical_record.animal_id
         WHERE animal_record.tutor_id = p_tutor_id
           AND exam.status = 'REQUESTED';

        p_atencoes_vacinais := pkg_clyvo_vacinacao.fn_qtd_atencoes_tutor(
            p_tutor_id,
            CAST(p_instante_referencia AS DATE)
        );
    END pr_indicadores_tutor;

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
    ) IS
        v_inicio_dia TIMESTAMP;
        v_fim_dia TIMESTAMP;
    BEGIN
        validar_veterinario_existente(p_veterinarian_id);
        validar_instante_referencia(p_instante_referencia);

        v_inicio_dia := CAST(
            TRUNC(CAST(p_instante_referencia AS DATE)) AS TIMESTAMP
        );
        v_fim_dia := v_inicio_dia + INTERVAL '1' DAY;

        SELECT COUNT(*)
          INTO p_tutores_vinculados
          FROM tb_veterinarian_tutor_links link_record
         WHERE link_record.veterinarian_id = p_veterinarian_id
           AND link_record.status = 'ACCEPTED';

        p_pacientes_ativos := contar_pacientes_veterinario(p_veterinarian_id);

        SELECT COUNT(*)
          INTO p_consultas_hoje
          FROM tb_appointments appointment
         WHERE appointment.veterinarian_id = p_veterinarian_id
           AND appointment.scheduled_at >= v_inicio_dia
           AND appointment.scheduled_at < v_fim_dia
           AND appointment.status <> 'CANCELLED';

        SELECT COUNT(CASE WHEN appointment.status = 'PENDING' THEN 1 END),
               COUNT(CASE WHEN appointment.status = 'CONFIRMED' THEN 1 END),
               COUNT(CASE WHEN appointment.status = 'COMPLETED' THEN 1 END)
          INTO p_consultas_pendentes,
               p_consultas_confirmadas,
               p_consultas_concluidas
          FROM tb_appointments appointment
         WHERE appointment.veterinarian_id = p_veterinarian_id;

        p_exames_pendentes := contar_exames_pendentes_veterinario(
            p_veterinarian_id
        );
    END pr_indicadores_veterinario;

    PROCEDURE pr_consultas_tutor_periodo(
        p_tutor_id  IN NUMBER,
        p_inicio    IN TIMESTAMP,
        p_fim       IN TIMESTAMP,
        p_resultado OUT SYS_REFCURSOR
    ) IS
    BEGIN
        validar_tutor_existente(p_tutor_id);
        validar_intervalo(p_inicio, p_fim);

        OPEN p_resultado FOR
            SELECT appointment.id AS appointment_id,
                   appointment.scheduled_at AS scheduled_at,
                   appointment.reason AS reason,
                   appointment.status AS status,
                   appointment.created_at AS created_at,
                   appointment.updated_at AS updated_at,
                   animal_record.id AS animal_id,
                   animal_record.name AS animal_name,
                   tutor.cpf AS tutor_cpf,
                   tutor.name AS tutor_name,
                   veterinarian.cpf AS veterinarian_cpf,
                   veterinarian.name AS veterinarian_name
              FROM tb_appointments appointment
              JOIN animal animal_record
                ON animal_record.id = appointment.animal_id
              JOIN tutor tutor
                ON tutor.id = appointment.tutor_id
              JOIN veterinarian veterinarian
                ON veterinarian.id = appointment.veterinarian_id
             WHERE appointment.tutor_id = p_tutor_id
               AND appointment.scheduled_at >= p_inicio
               AND appointment.scheduled_at < p_fim
             ORDER BY appointment.scheduled_at ASC,
                      appointment.id ASC;
    END pr_consultas_tutor_periodo;

    PROCEDURE pr_consultas_veterinario_periodo(
        p_veterinarian_id IN NUMBER,
        p_inicio          IN TIMESTAMP,
        p_fim             IN TIMESTAMP,
        p_resultado       OUT SYS_REFCURSOR
    ) IS
    BEGIN
        validar_veterinario_existente(p_veterinarian_id);
        validar_intervalo(p_inicio, p_fim);

        OPEN p_resultado FOR
            SELECT appointment.id AS appointment_id,
                   appointment.scheduled_at AS scheduled_at,
                   appointment.reason AS reason,
                   appointment.status AS status,
                   appointment.created_at AS created_at,
                   appointment.updated_at AS updated_at,
                   animal_record.id AS animal_id,
                   animal_record.name AS animal_name,
                   tutor.cpf AS tutor_cpf,
                   tutor.name AS tutor_name,
                   veterinarian.cpf AS veterinarian_cpf,
                   veterinarian.name AS veterinarian_name
              FROM tb_appointments appointment
              JOIN animal animal_record
                ON animal_record.id = appointment.animal_id
              JOIN tutor tutor
                ON tutor.id = appointment.tutor_id
              JOIN veterinarian veterinarian
                ON veterinarian.id = appointment.veterinarian_id
             WHERE appointment.veterinarian_id = p_veterinarian_id
               AND appointment.scheduled_at >= p_inicio
               AND appointment.scheduled_at < p_fim
             ORDER BY appointment.scheduled_at ASC,
                      appointment.id ASC;
    END pr_consultas_veterinario_periodo;

END pkg_clyvo_relatorios;
/

SHOW ERRORS PACKAGE BODY pkg_clyvo_relatorios

DECLARE
    v_status user_objects.status%TYPE;
BEGIN
    SELECT status
      INTO v_status
      FROM user_objects
     WHERE object_name = 'PKG_CLYVO_RELATORIOS'
       AND object_type = 'PACKAGE BODY';

    IF v_status <> 'VALID' THEN
        RAISE_APPLICATION_ERROR(-20961, 'Package body ficou INVALID.');
    END IF;
END;
/
