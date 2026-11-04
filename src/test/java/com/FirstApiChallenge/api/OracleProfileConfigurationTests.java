package com.FirstApiChallenge.api;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class OracleProfileConfigurationTests {

    @Test
    void loadsOracleProfilePropertiesWithoutConnectingToOracle() {
        var context = new AnnotationConfigApplicationContext();
        TestPropertyValues.of(
                "spring.profiles.active=oracle",
                "ORACLE_DB_URL=jdbc:oracle:thin:@//example.invalid:1521/ORCL",
                "ORACLE_DB_USERNAME=test-user",
                "ORACLE_DB_PASSWORD=test-password"
        ).applyTo(context);

        new ConfigDataApplicationContextInitializer().initialize(context);

        var environment = context.getEnvironment();
        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:oracle:thin:@//example.invalid:1521/ORCL");
        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("oracle.jdbc.OracleDriver");
        assertThat(environment.getProperty("spring.jpa.database-platform"))
                .isEqualTo("org.hibernate.dialect.OracleDialect");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(environment.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(environment.getProperty("spring.datasource.hikari.maximum-pool-size")).isEqualTo("5");

        var hikariConfig = Binder.get(environment)
                .bind("spring.datasource.hikari", Bindable.of(HikariConfig.class))
                .get();
        assertThat(hikariConfig.getDataSourceProperties())
                .containsEntry("oracle.net.CONNECT_TIMEOUT", "10000")
                .containsEntry("oracle.jdbc.ReadTimeout", "30000");

        context.close();
    }

    @Test
    void includesOracleJdbcDriverOnTheTestRuntimeClasspath() throws ClassNotFoundException {
        assertThat(Class.forName("oracle.jdbc.OracleDriver")).isNotNull();
    }
}
