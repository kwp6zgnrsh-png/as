package com.example.technician_server.controller;

import com.example.technician_server.domain.Assignment;
import com.example.technician_server.repository.AssignmentRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/assignments")
public class TechnicianController {

    private final AssignmentRepository assignmentRepository;

    public TechnicianController(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @PostMapping
    public ResponseEntity<Assignment> assignTechnician(@RequestBody AssignmentRequest request) {
        if (assignmentRepository.findByRequestId(request.getRequestId()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        // 요청받은 날짜(date)와 구체적인 시간(time)을 조합하여 저장
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
     * 기사 배정 요청 DTO (TimeSlot 대신 구체적인 time 받기)
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