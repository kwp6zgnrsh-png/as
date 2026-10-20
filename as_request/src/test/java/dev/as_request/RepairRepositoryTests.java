package dev.as_request;

import dev.as_request.dto.CreateRepairRequest;
import dev.as_request.dto.RepairResponse;
import dev.as_request.repository.RepairRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(RepairRepositoryTests.TestConfig.class)
class RepairRepositoryTests {

    @Autowired
    private RepairRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createTestTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS as_request");
        jdbcTemplate.execute("""
                CREATE TABLE as_request (
                    request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    request_no VARCHAR(30) NOT NULL UNIQUE,
                    customer_name VARCHAR(50) NOT NULL,
                    phone VARCHAR(20) NOT NULL,
                    product_id BIGINT NOT NULL,
                    region VARCHAR(50) NOT NULL,
                    symptom TEXT,
                    technician_id BIGINT,
                    status VARCHAR(30) NOT NULL,
                    created_at DATETIME NOT NULL
                )
                """);
    }

    @Test
    void savesFrontendInputAndExplicitProductAndTechnicianIds() {
        CreateRepairRequest request = new CreateRepairRequest(
                999L, "테스트고객", "01000000001", "서울특별시",
                LocalDateTime.now().plusDays(1), "전원이 켜지지 않습니다. '오류' 표시\n방문 전 연락 바랍니다."
        );

        RepairResponse saved = repository.save(request, 101L, 701L);
        List<RepairResponse> rows = repository.findAll();

        assertEquals(1, rows.size());
        assertEquals(saved, rows.getFirst());
        assertEquals("AS-" + saved.requestId(), saved.requestNo());
        assertEquals(request.customerName(), saved.customerName());
        assertEquals(request.phone(), saved.phone());
        assertEquals(101L, saved.productId());
        assertEquals(request.region(), saved.region());
        assertEquals(request.memo(), saved.symptom());
        assertEquals(701L, saved.technicianId());
        assertEquals("RECEIVED", saved.status());
        assertNotNull(saved.createdAt());
    }

    @Test
    void usesAutoIncrementIdForEachRequestNumber() {
        RepairResponse first = repository.save(request(null), 101L, 701L);
        RepairResponse second = repository.save(request(null), 102L, 702L);

        assertEquals(first.requestId() + 1, second.requestId());
        assertEquals("AS-" + first.requestId(), first.requestNo());
        assertEquals("AS-" + second.requestId(), second.requestNo());
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM as_request WHERE request_no LIKE 'TMP-%'", Long.class));
    }

    @Test
    void preservesNullableSymptomAndTechnicianId() {
        RepairResponse saved = repository.save(request(null), 101L, null);

        assertNull(saved.symptom());
        assertNull(saved.technicianId());
        assertEquals(saved, repository.findAll().getFirst());
    }

    @Test
    void rollsBackInsertIfFinalRequestNumberUpdateFails() {
        jdbcTemplate.execute("""
                ALTER TABLE as_request ADD CONSTRAINT test_reject_final_number
                CHECK (request_no LIKE 'TMP-%')
                """);

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.save(request("고장 증상"), 101L, 701L));

        assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM as_request", Long.class));
    }

    private CreateRepairRequest request(String memo) {
        return new CreateRepairRequest(1L, "테스트고객", "01000000001", "서울특별시",
                LocalDateTime.now().plusDays(1), memo);
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(
                    "jdbc:h2:mem:repair_repository_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                    "sa", ""
            );
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        RepairRepository repairRepository(JdbcTemplate jdbcTemplate) {
            return new RepairRepository(jdbcTemplate);
        }
    }
}
