package com.example.technician_server.repository;

import com.example.technician_server.domain.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    @Query("SELECT t FROM Technician t JOIN TechnicianSkill s ON t.technicianId = s.technicianId " +
            "WHERE t.region = :region AND s.category = :category AND t.active = true")
    List<Technician> findAvailableTechnicians(@Param("region") String region, @Param("category") String category);
}