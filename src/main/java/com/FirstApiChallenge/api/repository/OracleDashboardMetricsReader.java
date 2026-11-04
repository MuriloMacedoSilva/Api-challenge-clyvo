package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.exception.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@Profile("oracle")
public class OracleDashboardMetricsReader implements DashboardMetricsReader {

    static final String INPUT_TUTOR_ID = "P_TUTOR_ID";
    static final String INPUT_VETERINARIAN_ID = "P_VETERINARIAN_ID";
    static final String INPUT_REFERENCE_TIME = "P_INSTANTE_REFERENCIA";

    static final String OUTPUT_TOTAL_PETS = "P_TOTAL_PETS";
    static final String OUTPUT_APPOINTMENTS_TODAY = "P_CONSULTAS_HOJE";
    static final String OUTPUT_PENDING_APPOINTMENTS = "P_CONSULTAS_PENDENTES";
    static final String OUTPUT_LINKED_VETERINARIANS = "P_VETERINARIOS_VINCULADOS";
    static final String OUTPUT_PENDING_EXAMS = "P_EXAMES_PENDENTES";
    static final String OUTPUT_VACCINATIONS_ATTENTION = "P_ATENCOES_VACINAIS";

    static final String OUTPUT_LINKED_TUTORS = "P_TUTORES_VINCULADOS";
    static final String OUTPUT_ACTIVE_PATIENTS = "P_PACIENTES_ATIVOS";
    static final String OUTPUT_CONFIRMED_APPOINTMENTS = "P_CONSULTAS_CONFIRMADAS";
    static final String OUTPUT_COMPLETED_APPOINTMENTS = "P_CONSULTAS_CONCLUIDAS";

    private final SimpleJdbcCall tutorMetricsCall;
    private final SimpleJdbcCall veterinarianMetricsCall;

    @Autowired
    public OracleDashboardMetricsReader(DataSource dataSource) {
        this(
                new SimpleJdbcCall(dataSource)
                        .withCatalogName("PKG_CLYVO_RELATORIOS")
                        .withProcedureName("PR_INDICADORES_TUTOR")
                        .withoutProcedureColumnMetaDataAccess()
                        .declareParameters(
                                new SqlParameter(INPUT_TUTOR_ID, Types.NUMERIC),
                                new SqlParameter(INPUT_REFERENCE_TIME, Types.TIMESTAMP),
                                out(OUTPUT_TOTAL_PETS),
                                out(OUTPUT_APPOINTMENTS_TODAY),
                                out(OUTPUT_PENDING_APPOINTMENTS),
                                out(OUTPUT_LINKED_VETERINARIANS),
                                out(OUTPUT_PENDING_EXAMS),
                                out(OUTPUT_VACCINATIONS_ATTENTION)
                        ),
                new SimpleJdbcCall(dataSource)
                        .withCatalogName("PKG_CLYVO_RELATORIOS")
                        .withProcedureName("PR_INDICADORES_VETERINARIO")
                        .withoutProcedureColumnMetaDataAccess()
                        .declareParameters(
                                new SqlParameter(INPUT_VETERINARIAN_ID, Types.NUMERIC),
                                new SqlParameter(INPUT_REFERENCE_TIME, Types.TIMESTAMP),
                                out(OUTPUT_LINKED_TUTORS),
                                out(OUTPUT_ACTIVE_PATIENTS),
                                out(OUTPUT_APPOINTMENTS_TODAY),
                                out(OUTPUT_PENDING_APPOINTMENTS),
                                out(OUTPUT_CONFIRMED_APPOINTMENTS),
                                out(OUTPUT_COMPLETED_APPOINTMENTS),
                                out(OUTPUT_PENDING_EXAMS)
                        )
        );
    }

    OracleDashboardMetricsReader(
            SimpleJdbcCall tutorMetricsCall,
            SimpleJdbcCall veterinarianMetricsCall) {
        this.tutorMetricsCall = tutorMetricsCall;
        this.veterinarianMetricsCall = veterinarianMetricsCall;
    }

    @Override
    public TutorMetrics readTutorMetrics(
            Long tutorId,
            String tutorCpf,
            LocalDateTime referenceTime) {
        try {
            Map<String, Object> result = tutorMetricsCall.execute(new MapSqlParameterSource()
                    .addValue(INPUT_TUTOR_ID, tutorId)
                    .addValue(INPUT_REFERENCE_TIME, Timestamp.valueOf(referenceTime)));
            return new TutorMetrics(
                    requiredLong(result, OUTPUT_TOTAL_PETS),
                    requiredLong(result, OUTPUT_APPOINTMENTS_TODAY),
                    requiredLong(result, OUTPUT_PENDING_APPOINTMENTS),
                    requiredLong(result, OUTPUT_LINKED_VETERINARIANS),
                    requiredLong(result, OUTPUT_PENDING_EXAMS),
                    requiredLong(result, OUTPUT_VACCINATIONS_ATTENTION)
            );
        } catch (DataAccessException exception) {
            throw translateException(exception);
        }
    }

    @Override
    public VeterinarianMetrics readVeterinarianMetrics(
            Long veterinarianId,
            String veterinarianCpf,
            LocalDateTime referenceTime) {
        try {
            Map<String, Object> result = veterinarianMetricsCall.execute(new MapSqlParameterSource()
                    .addValue(INPUT_VETERINARIAN_ID, veterinarianId)
                    .addValue(INPUT_REFERENCE_TIME, Timestamp.valueOf(referenceTime)));
            return new VeterinarianMetrics(
                    requiredLong(result, OUTPUT_LINKED_TUTORS),
                    requiredLong(result, OUTPUT_ACTIVE_PATIENTS),
                    requiredLong(result, OUTPUT_APPOINTMENTS_TODAY),
                    requiredLong(result, OUTPUT_PENDING_APPOINTMENTS),
                    requiredLong(result, OUTPUT_CONFIRMED_APPOINTMENTS),
                    requiredLong(result, OUTPUT_COMPLETED_APPOINTMENTS),
                    requiredLong(result, OUTPUT_PENDING_EXAMS)
            );
        } catch (DataAccessException exception) {
            throw translateException(exception);
        }
    }

    private static SqlOutParameter out(String name) {
        return new SqlOutParameter(name, Types.NUMERIC);
    }

    private static long requiredLong(Map<String, Object> result, String outputName) {
        Object value = result.get(outputName);
        if (!(value instanceof Number number)) {
            throw integrationFailure();
        }
        return number.longValue();
    }

    private static CustomException translateException(DataAccessException exception) {
        Integer errorCode = findOracleApplicationError(exception);
        if (errorCode == null) {
            return integrationFailure();
        }

        return switch (errorCode) {
            case 20021 -> new CustomException("Parâmetro de relatório inválido", HttpStatus.BAD_REQUEST);
            case 20022 -> new CustomException("Parâmetro obrigatório não informado", HttpStatus.BAD_REQUEST);
            case 20023 -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND);
            case 20024 -> new CustomException("Veterinário não encontrado", HttpStatus.NOT_FOUND);
            case 20025 -> new CustomException("Intervalo de relatório inválido", HttpStatus.BAD_REQUEST);
            default -> integrationFailure();
        };
    }

    private static Integer findOracleApplicationError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                SQLException candidate = sqlException;
                while (candidate != null) {
                    int errorCode = Math.abs(candidate.getErrorCode());
                    if (errorCode >= 20021 && errorCode <= 20025) {
                        return errorCode;
                    }
                    candidate = candidate.getNextException();
                }
            }
            current = current.getCause();
        }
        return null;
    }

    private static CustomException integrationFailure() {
        return new CustomException(
                "Não foi possível consultar os indicadores do dashboard",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
