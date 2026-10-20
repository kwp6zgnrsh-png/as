package dev.as_request.repository;

import dev.as_request.dto.CreateRepairRequest;
import dev.as_request.dto.RepairResponse;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class RepairRepository {

    private final JdbcTemplate jdbcTemplate;

    public RepairRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public RepairResponse save(CreateRepairRequest request, Long productId, Long technicianId) {
        String sql = """
                INSERT INTO as_request (
                    request_no, customer_name, phone, product_id, region,
                    symptom, technician_id, status, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String status = "RECEIVED";
        // request_no가 NOT NULL이므로 트랜잭션 안에서만 쓰는 고유 임시 번호를 넣습니다.
        // 자동 증가 ID를 받은 뒤 커밋 전에 AS-{request_id}로 교체합니다.
        String temporaryRequestNo = "TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 26);
        KeyHolder keyHolder = new GeneratedKeyHolder();

        int inserted = jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"request_id"});
            statement.setString(1, temporaryRequestNo);
            statement.setString(2, request.customerName());
            statement.setString(3, request.phone());
            statement.setLong(4, productId);
            statement.setString(5, request.region());
            statement.setString(6, request.memo());
            statement.setObject(7, technicianId, Types.BIGINT);
            statement.setString(8, status);
            statement.setTimestamp(9, Timestamp.valueOf(request.visitDateTime()));
            return statement;
        }, keyHolder);

        Number generatedKey = keyHolder.getKey();
        if (inserted != 1 || generatedKey == null) {
            throw new DataRetrievalFailureException("접수 ID를 생성하지 못했습니다.");
        }

        Long requestId = generatedKey.longValue();
        String requestNo = "AS-" + requestId;
        int updated = jdbcTemplate.update(
                "UPDATE as_request SET request_no = ? WHERE request_id = ?",
                requestNo, requestId
        );
        if (updated != 1) {
            throw new DataRetrievalFailureException("접수번호를 설정하지 못했습니다.");
        }

        return new RepairResponse(
                requestId, requestNo, request.customerName(), request.phone(), productId,
                request.region(), request.memo(), technicianId, status, request.visitDateTime()
        );
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
