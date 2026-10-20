package com.example.technician_server.repository;

import com.example.technician_server.domain.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    @Query("""
        SELECT DISTINCT t 
        FROM Technician t 
        JOIN TechnicianSkill ts ON t.technicianId = ts.technicianId 
        WHERE t.region = :region 
          AND ts.productType = :productType 
          AND t.active = true 
          AND t.technicianId NOT IN (
              SELECT a.technicianId 
              FROM Assignment a 
              WHERE a.status = 'ASSIGNED' 
                AND a.assignedAt >= :startTime 
                AND a.assignedAt < :endTime
          )
    """)
    List<Technician> findAvailableTechnicians(
            @Param("region") String region,
            @Param("productType") String productType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}