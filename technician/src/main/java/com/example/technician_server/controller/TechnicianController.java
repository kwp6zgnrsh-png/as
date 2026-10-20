package com.example.technician_server.controller;


import com.example.technician_server.domain.Assignment;
import com.example.technician_server.repository.AssignmentRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class TechnicianController {

    private final AssignmentRepository assignmentRepository;

    public TechnicianController(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    /**
     * 기사 배정 등록 (구체적 방문 시간 LocalTime 수신)
     */
    @PostMapping
    public ResponseEntity<Assignment> assignTechnician(@RequestBody AssignmentRequest request) {
        if (assignmentRepository.findByRequestId(request.getRequestId()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        LocalDateTime assignedAt = LocalDateTime.of(request.getDate(), request.getTime());

        Assignment assignment = new Assignment();
        assignment.setRequestId(request.getRequestId());
        assignment.setTechnicianId(request.getTechnicianId());
        assignment.setStatus("ASSIGNED");
        assignment.setAssignedAt(assignedAt);

        Assignment saved = assignmentRepository.save(assignment);
        return ResponseEntity.ok(saved);
    }

    /**
     * 전체 배정 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<Assignment>> getAllAssignments() {
        List<Assignment> assignments = assignmentRepository.findAll();
        return ResponseEntity.ok(assignments);
    }

    /**
     * 기사 배정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentRequest {
        private Long requestId;
        private Long technicianId;
        private LocalDate date; // 방문/접수 날짜 (예: "2026-10-25")
        private LocalTime time; // 접수처 지정 방문 시간 (예: "10:30:00" 또는 "10:30")
    }
}