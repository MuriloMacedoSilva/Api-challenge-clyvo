package com.FirstApiChallenge.api.repository;

import com.FirstApiChallenge.api.dto.VaccinationResponseDTO;
import com.FirstApiChallenge.api.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OracleVaccinationHistoryReaderTests {

    @Mock
    private SimpleJdbcCall historyCall;

    @Mock
    private ResultSet resultSet;

    @Test
    void mapsCursorColumnsToExistingResponseContract() throws SQLException {
        LocalDate applicationDate = LocalDate.of(2026, 10, 10);
        LocalDate nextDoseDate = LocalDate.of(2027, 10, 10);
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 10, 9, 30, 15);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 10, 11, 10, 45, 20);

        when(resultSet.getObject("ID")).thenReturn(BigDecimal.valueOf(91));
        when(resultSet.getObject("ANIMAL_ID")).thenReturn(BigDecimal.valueOf(17));
        when(resultSet.getString("ANIMAL_NAME")).thenReturn("Luna");
        when(resultSet.getString("VETERINARIAN_CPF")).thenReturn("98765432100");
        when(resultSet.getString("VETERINARIAN_NAME")).thenReturn("Dra. Ana");
        when(resultSet.getString("VETERINARIAN_CRMV_NUMBER")).thenReturn("12345");
        when(resultSet.getString("VETERINARIAN_CRMV_STATE")).thenReturn("SP");
        when(resultSet.getString("VACCINE_NAME")).thenReturn("V10");
        when(resultSet.getDate("APPLICATION_DATE")).thenReturn(Date.valueOf(applicationDate));
        when(resultSet.getDate("NEXT_DOSE_DATE")).thenReturn(Date.valueOf(nextDoseDate));
        when(resultSet.getString("BATCH_NUMBER")).thenReturn("LOTE-10");
        when(resultSet.getString("MANUFACTURER")).thenReturn("Laboratório CLYVO");
        when(resultSet.getString("OBSERVATIONS")).thenReturn("Sem intercorrências");
        when(resultSet.getTimestamp("CREATED_AT")).thenReturn(Timestamp.valueOf(createdAt));
        when(resultSet.getTimestamp("UPDATED_AT")).thenReturn(Timestamp.valueOf(updatedAt));

        VaccinationResponseDTO result = OracleVaccinationHistoryReader.ROW_MAPPER
                .mapRow(resultSet, 0);

        assertThat(result).isEqualTo(new VaccinationResponseDTO(
                91L,
                17L,
                "Luna",
                "98765432100",
                "Dra. Ana",
                "12345",
                "SP",
                "V10",
                applicationDate,
                nextDoseDate,
                "LOTE-10",
                "Laboratório CLYVO",
                "Sem intercorrências",
                createdAt,
                updatedAt
        ));
        verify(resultSet, never()).getString("DOSE_DATE_STATUS");
    }

    @Test
    void mapsOptionalNullValues() throws SQLException {
        when(resultSet.getObject("ID")).thenReturn(BigDecimal.ONE);
        when(resultSet.getObject("ANIMAL_ID")).thenReturn(BigDecimal.TEN);
        when(resultSet.getDate("APPLICATION_DATE")).thenReturn(Date.valueOf("2026-11-04"));
        when(resultSet.getTimestamp("CREATED_AT"))
                .thenReturn(Timestamp.valueOf("2026-11-04 12:00:00"));

        VaccinationResponseDTO result = OracleVaccinationHistoryReader.ROW_MAPPER
                .mapRow(resultSet, 0);

        assertThat(result.nextDoseDate()).isNull();
        assertThat(result.batchNumber()).isNull();
        assertThat(result.manufacturer()).isNull();
        assertThat(result.observations()).isNull();
        assertThat(result.updatedAt()).isNull();
    }

    @Test
    void preservesCursorOrderAndDeclaresAnimalInput() {
        VaccinationResponseDTO newest = response(2L, LocalDate.of(2026, 11, 4));
        VaccinationResponseDTO oldest = response(1L, LocalDate.of(2025, 11, 4));
        when(historyCall.execute(any(SqlParameterSource.class)))
                .thenReturn(Map.of(
                        OracleVaccinationHistoryReader.OUTPUT_RESULT,
                        List.of(newest, oldest)
                ));

        List<VaccinationResponseDTO> result = reader().findByAnimalId(17L);

        assertThat(result).containsExactly(newest, oldest);
        ArgumentCaptor<SqlParameterSource> parameters =
                ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(historyCall).execute(parameters.capture());
        assertThat(parameters.getValue().getValue(
                OracleVaccinationHistoryReader.INPUT_ANIMAL_ID
        )).isEqualTo(17L);
    }

    @Test
    void returnsEmptyHistoryWhenCursorHasNoRows() {
        when(historyCall.execute(any(SqlParameterSource.class)))
                .thenReturn(Map.of(OracleVaccinationHistoryReader.OUTPUT_RESULT, List.of()));

        assertThat(reader().findByAnimalId(17L)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "20001, BAD_REQUEST",
            "20002, NOT_FOUND",
            "20003, NOT_FOUND",
            "20004, BAD_REQUEST"
    })
    void translatesKnownOracleApplicationErrors(int errorCode, HttpStatus expectedStatus) {
        SQLException sqlException = new SQLException("internal Oracle detail", "72000", errorCode);
        when(historyCall.execute(any(SqlParameterSource.class)))
                .thenThrow(new UncategorizedSQLException("call", null, sqlException));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> reader().findByAnimalId(17L)
        );

        assertThat(exception.getStatus()).isEqualTo(expectedStatus);
        assertThat(exception.getMessage()).doesNotContain("Oracle", "internal", "SQL");
    }

    @Test
    void hidesUnexpectedDatabaseFailure() {
        SQLException sqlException = new SQLException("jdbc:oracle:thin:@secret", "72000", 17002);
        when(historyCall.execute(any(SqlParameterSource.class)))
                .thenThrow(new UncategorizedSQLException("call", "secret SQL", sqlException));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> reader().findByAnimalId(17L)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getMessage()).isEqualTo(
                "Não foi possível consultar o histórico de vacinação"
        );
    }

    private OracleVaccinationHistoryReader reader() {
        return new OracleVaccinationHistoryReader(historyCall);
    }

    private VaccinationResponseDTO response(Long id, LocalDate applicationDate) {
        return new VaccinationResponseDTO(
                id,
                17L,
                "Luna",
                "98765432100",
                "Dra. Ana",
                "12345",
                "SP",
                "V10",
                applicationDate,
                null,
                null,
                null,
                null,
                applicationDate.atStartOfDay(),
                null
        );
    }
}
