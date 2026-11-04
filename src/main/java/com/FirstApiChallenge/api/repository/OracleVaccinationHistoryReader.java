package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.dto.VaccinationResponseDTO;
import com.FirstApiChallenge.api.exception.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@Profile("oracle")
public class OracleVaccinationHistoryReader implements VaccinationHistoryReader {

    static final String INPUT_ANIMAL_ID = "P_ANIMAL_ID";
    static final String OUTPUT_RESULT = "P_RESULTADO";
    static final RowMapper<VaccinationResponseDTO> ROW_MAPPER =
            OracleVaccinationHistoryReader::mapRow;

    private final SimpleJdbcCall historyCall;

    @Autowired
    public OracleVaccinationHistoryReader(DataSource dataSource) {
        this(new SimpleJdbcCall(dataSource)
                .withCatalogName("PKG_CLYVO_VACINACAO")
                .withProcedureName("PR_HISTORICO_ANIMAL")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(INPUT_ANIMAL_ID, Types.NUMERIC),
                        new SqlOutParameter(OUTPUT_RESULT, Types.REF_CURSOR, ROW_MAPPER)
                ));
    }

    OracleVaccinationHistoryReader(SimpleJdbcCall historyCall) {
        this.historyCall = historyCall;
    }

    @Override
    public List<VaccinationResponseDTO> findByAnimalId(Long animalId) {
        try {
            Map<String, Object> result = historyCall.execute(
                    new MapSqlParameterSource(INPUT_ANIMAL_ID, animalId)
            );
            return extractHistory(result.get(OUTPUT_RESULT));
        } catch (DataAccessException exception) {
            throw translateException(exception);
        }
    }

    private static VaccinationResponseDTO mapRow(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new VaccinationResponseDTO(
                getLong(resultSet, "ID"),
                getLong(resultSet, "ANIMAL_ID"),
                resultSet.getString("ANIMAL_NAME"),
                resultSet.getString("VETERINARIAN_CPF"),
                resultSet.getString("VETERINARIAN_NAME"),
                resultSet.getString("VETERINARIAN_CRMV_NUMBER"),
                resultSet.getString("VETERINARIAN_CRMV_STATE"),
                resultSet.getString("VACCINE_NAME"),
                getLocalDate(resultSet, "APPLICATION_DATE"),
                getLocalDate(resultSet, "NEXT_DOSE_DATE"),
                resultSet.getString("BATCH_NUMBER"),
                resultSet.getString("MANUFACTURER"),
                resultSet.getString("OBSERVATIONS"),
                getLocalDateTime(resultSet, "CREATED_AT"),
                getLocalDateTime(resultSet, "UPDATED_AT")
        );
    }

    private static Long getLong(ResultSet resultSet, String column) throws SQLException {
        Number value = (Number) resultSet.getObject(column);
        return value == null ? null : value.longValue();
    }

    private static LocalDate getLocalDate(ResultSet resultSet, String column) throws SQLException {
        Date value = resultSet.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private static LocalDateTime getLocalDateTime(ResultSet resultSet, String column)
            throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static List<VaccinationResponseDTO> extractHistory(Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> rows)) {
            throw integrationFailure();
        }

        try {
            return rows.stream().map(VaccinationResponseDTO.class::cast).toList();
        } catch (ClassCastException exception) {
            throw integrationFailure();
        }
    }

    private static CustomException translateException(DataAccessException exception) {
        Integer errorCode = findOracleApplicationError(exception);
        if (errorCode == null) {
            return integrationFailure();
        }

        return switch (errorCode) {
            case 20001 -> new CustomException(
                    "Parâmetro inválido para consulta do histórico de vacinação",
                    HttpStatus.BAD_REQUEST
            );
            case 20002 -> new CustomException("Tutor não encontrado", HttpStatus.NOT_FOUND);
            case 20003 -> new CustomException("Animal não encontrado", HttpStatus.NOT_FOUND);
            case 20004 -> new CustomException("Filtro de vacinação inválido", HttpStatus.BAD_REQUEST);
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
                    if (errorCode >= 20001 && errorCode <= 20004) {
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
                "Não foi possível consultar o histórico de vacinação",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
