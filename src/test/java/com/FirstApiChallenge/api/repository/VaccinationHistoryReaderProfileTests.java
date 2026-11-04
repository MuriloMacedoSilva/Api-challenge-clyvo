package com.FirstApiChallenge.api.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VaccinationHistoryReaderProfileTests {

    @ParameterizedTest
    @ValueSource(strings = {"local", "prod", "render", "test"})
    void selectsJpaReaderOutsideOracle(String profile) {
        try (var context = createContext(profile)) {
            assertThat(context.getBean(VaccinationHistoryReader.class))
                    .isInstanceOf(JpaVaccinationHistoryReader.class);
            assertThat(context.getBeansOfType(OracleVaccinationHistoryReader.class)).isEmpty();
        }
    }

    @Test
    void selectsOracleReaderOnlyForOracleProfile() {
        try (var context = createContext("oracle")) {
            assertThat(context.getBean(VaccinationHistoryReader.class))
                    .isInstanceOf(OracleVaccinationHistoryReader.class);
            assertThat(context.getBeansOfType(JpaVaccinationHistoryReader.class)).isEmpty();
        }
    }

    private AnnotationConfigApplicationContext createContext(String profile) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        context.registerBean(VaccinationRepository.class, () -> mock(VaccinationRepository.class));
        context.registerBean(DataSource.class, () -> mock(DataSource.class));
        context.register(JpaVaccinationHistoryReader.class, OracleVaccinationHistoryReader.class);
        context.refresh();
        return context;
    }
}
