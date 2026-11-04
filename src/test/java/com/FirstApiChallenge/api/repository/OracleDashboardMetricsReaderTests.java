package com.FirstApiChallenge.api.repository;

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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OracleDashboardMetricsReaderTests {

    @Mock
    private SimpleJdbcCall tutorMetricsCall;

    @Mock
    private SimpleJdbcCall veterinarianMetricsCall;

    @Test
    void mapsTutorOutputsAndSendsInternalIdAndReferenceTimestamp() {
        LocalDateTime referenceTime = LocalDateTime.of(2026, 11, 4, 14, 30, 45);
        when(tutorMetricsCall.execute(any(SqlParameterSource.class))).thenReturn(Map.of(
                OracleDashboardMetricsReader.OUTPUT_TOTAL_PETS, BigDecimal.valueOf(3),
                OracleDashboardMetricsReader.OUTPUT_APPOINTMENTS_TODAY, 2,
                OracleDashboardMetricsReader.OUTPUT_PENDING_APPOINTMENTS, 4L,
                OracleDashboardMetricsReader.OUTPUT_LINKED_VETERINARIANS, BigDecimal.ONE,
                OracleDashboardMetricsReader.OUTPUT_PENDING_EXAMS, BigDecimal.valueOf(5),
                OracleDashboardMetricsReader.OUTPUT_VACCINATIONS_ATTENTION, 6
        ));

        var result = reader().readTutorMetrics(17L, "12345678901", referenceTime);

        assertThat(result).isEqualTo(new DashboardMetricsReader.TutorMetrics(3, 2, 4, 1, 5, 6));
        ArgumentCaptor<SqlParameterSource> parameters =
                ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(tutorMetricsCall).execute(parameters.capture());
        assertThat(parameters.getValue().getValue(OracleDashboardMetricsReader.INPUT_TUTOR_ID))
                .isEqualTo(17L);
        assertThat(parameters.getValue().getValue(
                OracleDashboardMetricsReader.INPUT_REFERENCE_TIME
        )).isEqualTo(Timestamp.valueOf(referenceTime));
        verifyNoInteractions(veterinarianMetricsCall);
    }

    @Test
    void mapsVeterinarianOutputsAndSendsInternalIdAndReferenceTimestamp() {
        LocalDateTime referenceTime = LocalDateTime.of(2026, 11, 4, 15, 45);
        when(veterinarianMetricsCall.execute(any(SqlParameterSource.class))).thenReturn(Map.of(
                OracleDashboardMetricsReader.OUTPUT_LINKED_TUTORS, BigDecimal.valueOf(7),
                OracleDashboardMetricsReader.OUTPUT_ACTIVE_PATIENTS, 8,
                OracleDashboardMetricsReader.OUTPUT_APPOINTMENTS_TODAY, 2L,
                OracleDashboardMetricsReader.OUTPUT_PENDING_APPOINTMENTS, BigDecimal.valueOf(3),
                OracleDashboardMetricsReader.OUTPUT_CONFIRMED_APPOINTMENTS, 4,
                OracleDashboardMetricsReader.OUTPUT_COMPLETED_APPOINTMENTS, BigDecimal.valueOf(9),
                OracleDashboardMetricsReader.OUTPUT_PENDING_EXAMS, 5L
        ));

        var result = reader().readVeterinarianMetrics(23L, "98765432100", referenceTime);

        assertThat(result).isEqualTo(
                new DashboardMetricsReader.VeterinarianMetrics(7, 8, 2, 3, 4, 9, 5)
        );
        ArgumentCaptor<SqlParameterSource> parameters =
                ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(veterinarianMetricsCall).execute(parameters.capture());
        assertThat(parameters.getValue().getValue(
                OracleDashboardMetricsReader.INPUT_VETERINARIAN_ID
        )).isEqualTo(23L);
        assertThat(parameters.getValue().getValue(
                OracleDashboardMetricsReader.INPUT_REFERENCE_TIME
        )).isEqualTo(Timestamp.valueOf(referenceTime));
        verifyNoInteractions(tutorMetricsCall);
    }

    @Test
    void rejectsMissingOrNullOutputInsteadOfReturningZero() {
        Map<String, Object> outputs = new HashMap<>();
        outputs.put(OracleDashboardMetricsReader.OUTPUT_TOTAL_PETS, null);
        when(tutorMetricsCall.execute(any(SqlParameterSource.class))).thenReturn(outputs);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> reader().readTutorMetrics(17L, "12345678901", LocalDateTime.now())
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getMessage()).isEqualTo(
                "Não foi possível consultar os indicadores do dashboard"
        );
    }

    @Test
    void rejectsOutputWithUnexpectedType() {
        when(tutorMetricsCall.execute(any(SqlParameterSource.class))).thenReturn(Map.of(
                OracleDashboardMetricsReader.OUTPUT_TOTAL_PETS, "3"
        ));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> reader().readTutorMetrics(17L, "12345678901", LocalDateTime.now())
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ParameterizedTest
    @CsvSource({
            "20021, BAD_REQUEST",
            "20022, BAD_REQUEST",
            "20023, NOT_FOUND",
            "20024, NOT_FOUND",
            "20025, BAD_REQUEST"
    })
    void translatesKnownOracleApplicationErrors(int errorCode, HttpStatus expectedStatus) {
        SQLException sqlException = new SQLException("internal Oracle detail", "72000", errorCode);
        when(tutorMetricsCall.execute(any(SqlParameterSource.class)))
                .thenThrow(new UncategorizedSQLException("call", null, sqlException));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> reader().readTutorMetrics(17L, "12345678901", LocalDateTime.now())
        );

        assertThat(exception.getStatus()).isEqualTo(expectedStatus);
        assertThat(exception.getMessage()).doesNotContain("Oracle", "internal", "SQL");
    }

    @Test
    void findsKnownErrorInChainedSqlException() {
        SQLException first = new SQLException("wrapper", "72000", 17002);
        first.setNextException(new SQLException("application error", "72000", -20024));
        when(veterinarianMetricsCall.execute(any(SqlParameterSource.class)))
                .thenThrow(new UncategorizedSQLException("call", null, first));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> reader().readVeterinarianMetrics(
                        23L,
                        "98765432100",
                        LocalDateTime.now()
                )
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("Veterinário não encontrado");
    }

    @Test
    void hidesUnexpectedDatabaseFailure() {
        SQLException sqlException = new SQLException("jdbc:oracle:thin:@secret", "72000", 17002);
        when(veterinarianMetricsCall.execute(any(SqlParameterSource.class)))
                .thenThrow(new UncategorizedSQLException("call", "secret SQL", sqlException));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> reader().readVeterinarianMetrics(
                        23L,
                        "98765432100",
                        LocalDateTime.now()
                )
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getMessage()).isEqualTo(
                "Não foi possível consultar os indicadores do dashboard"
        );
    }

    private OracleDashboardMetricsReader reader() {
        return new OracleDashboardMetricsReader(tutorMetricsCall, veterinarianMetricsCall);
    }
}
