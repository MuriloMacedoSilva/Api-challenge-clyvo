-- Oracle 19c, somente leitura. Uso: @export_veterinary_appointments.sql /caminho/veterinary_appointments.b64
-- Saida enquadrada em DOC_BEGIN/DOC_END, base64 em blocos; converter localmente para JSON/JSONL.
SET ECHO OFF
SET VERIFY OFF
SET FEEDBACK OFF
SET HEADING OFF
SET PAGESIZE 0
SET LINESIZE 32767
SET TRIMSPOOL ON
SET SERVEROUTPUT ON SIZE UNLIMITED FORMAT WRAPPED
WHENEVER OSERROR EXIT FAILURE
WHENEVER SQLERROR EXIT SQL.SQLCODE
SPOOL /tmp/clyvo-export-privado/veterinary_appointments.b64

DECLARE
    v_json CLOB;
    v_offset PLS_INTEGER;
    v_piece VARCHAR2(32767);
BEGIN
    FOR row_data IN (
        SELECT JSON_OBJECT(
            '_id' VALUE 'oracle:appointment:' || TO_CHAR(ap.id, 'FM9999999999999999999'),
            'schemaVersion' VALUE 1,
            'sourceSystem' VALUE 'CLYVO_ORACLE_19C',
            'exportedAt' VALUE TO_CHAR(SYSTIMESTAMP AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.FF3"Z"'),
            'appointmentId' VALUE TO_CHAR(ap.id, 'FM9999999999999999999'),
            'animalId' VALUE TO_CHAR(ap.animal_id, 'FM9999999999999999999'),
            'tutorId' VALUE TO_CHAR(ap.tutor_id, 'FM9999999999999999999'),
            'veterinarianId' VALUE TO_CHAR(ap.veterinarian_id, 'FM9999999999999999999'),
            'scheduledAtLocal' VALUE TO_CHAR(ap.scheduled_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF6'),
            'status' VALUE ap.status
            NULL ON NULL RETURNING CLOB
        ) AS document_json
        FROM tb_appointments ap
        ORDER BY ap.id
    ) LOOP
        v_json := row_data.document_json;
        DBMS_OUTPUT.PUT_LINE('DOC_BEGIN');
        v_offset := 1;
        WHILE v_offset <= DBMS_LOB.GETLENGTH(v_json) LOOP
            v_piece := DBMS_LOB.SUBSTR(v_json, 4000, v_offset);
            DBMS_OUTPUT.PUT_LINE(UTL_RAW.CAST_TO_VARCHAR2(
                UTL_ENCODE.BASE64_ENCODE(UTL_I18N.STRING_TO_RAW(v_piece, 'AL32UTF8'))
            ));
            v_offset := v_offset + LENGTH(v_piece);
        END LOOP;
        DBMS_OUTPUT.PUT_LINE('DOC_END');
    END LOOP;
END;
/
SPOOL OFF
