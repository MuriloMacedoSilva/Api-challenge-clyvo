package com.FirstApiChallenge.api.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DashboardMetricsReaderProfileTests {

    @ParameterizedTest
    @ValueSource(strings = {"local", "prod", "render", "test"})
    void selectsJpaReaderOutsideOracle(String profile) {
        try (var context = createContext(profile)) {
            assertThat(context.getBean(DashboardMetricsReader.class))
                    .isInstanceOf(JpaDashboardMetricsReader.class);
            assertThat(context.getBeansOfType(OracleDashboardMetricsReader.class)).isEmpty();
        }
    }

    @Test
    void selectsOracleReaderOnlyForOracleProfile() {
        try (var context = createContext("oracle")) {
            assertThat(context.getBean(DashboardMetricsReader.class))
                    .isInstanceOf(OracleDashboardMetricsReader.class);
            assertThat(context.getBeansOfType(JpaDashboardMetricsReader.class)).isEmpty();
        }
    }

    private AnnotationConfigApplicationContext createContext(String profile) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        context.registerBean(AnimalRepository.class, () -> mock(AnimalRepository.class));
        context.registerBean(AppointmentRepository.class, () -> mock(AppointmentRepository.class));
        context.registerBean(
                VeterinarianTutorLinkRepository.class,
                () -> mock(VeterinarianTutorLinkRepository.class)
        );
        context.registerBean(ExamRepository.class, () -> mock(ExamRepository.class));
        context.registerBean(VaccinationRepository.class, () -> mock(VaccinationRepository.class));
        context.registerBean(DataSource.class, () -> mock(DataSource.class));
        context.register(JpaDashboardMetricsReader.class, OracleDashboardMetricsReader.class);
        context.refresh();
        return context;
    }
}
