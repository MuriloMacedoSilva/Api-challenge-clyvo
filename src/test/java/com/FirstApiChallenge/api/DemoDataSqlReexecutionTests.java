package com.FirstApiChallenge.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:clyvoDemoDataTestDB")
class DemoDataSqlReexecutionTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void supportsReexecutionAndKeepsIdentityUsable() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new FileSystemResource("demo-data.sql")
        );
        populator.execute(dataSource);
        populator.execute(dataSource);

        assertEquals(10L, count("tutor"));
        assertEquals(25L, count("tb_appointments"));
        assertEquals(38L, count("tb_messages"));

        jdbcTemplate.update("""
                insert into tutor (name, email, cpf, phone_number, password, role)
                values ('Novo Tutor', 'novo@demo.clyvo.com', '00112233445', '11911112222', '12345678', 'tutor')
                """);
        assertEquals(11L, jdbcTemplate.queryForObject(
                "select id from tutor where cpf = '00112233445'",
                Long.class
        ));
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
    }
}
