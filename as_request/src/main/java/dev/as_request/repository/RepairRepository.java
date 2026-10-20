package dev.as_request.repository;

import dev.as_request.dto.RepairResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class RepairRepository {

    private final JdbcTemplate jdbcTemplate;

    public RepairRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RepairResponse> findAll() {
        String sql = """
                SELECT request_id, request_no, customer_name, phone, product_id,
                       region, symptom, technician_id, status, created_at
                FROM as_request
                ORDER BY created_at DESC, request_id DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new RepairResponse(
                rs.getLong("request_id"),
                rs.getString("request_no"),
                rs.getString("customer_name"),
                rs.getString("phone"),
                rs.getLong("product_id"),
                rs.getString("region"),
                rs.getString("symptom"),
                rs.getObject("technician_id", Long.class),
                rs.getString("status"),
                rs.getObject("created_at", LocalDateTime.class)
        ));
    }
}
