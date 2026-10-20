package com.example.technician_server.repository;

import com.example.technician_server.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    Optional<Assignment> findByRequestId(Long requestId);
}