package com.example.technician_server.controller;

import com.example.technician_server.domain.Assignment;
import com.example.technician_server.domain.Technician;
import com.example.technician_server.repository.AssignmentRepository;
import com.example.technician_server.repository.TechnicianRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TechnicianController {

    private final AssignmentRepository assignmentRepository;
    private final TechnicianRepository technicianRepository;

    public TechnicianController(AssignmentRepository assignmentRepository, TechnicianRepository technicianRepository) {
        this.assignmentRepository = assignmentRepository;
        this.technicianRepository = technicianRepository;
    }

    /**
     * 기사 배정 등록 (POST /api/assignments)
     */
    @PostMapping("/assignments")
    public ResponseEntity<Assignment> assignTechnician(@RequestBody AssignmentRequest request) {
        if (assignmentRepository.findByRequestId(request.getRequestId()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        Assignment assignment = new Assignment();
        assignment.setRequestId(request.getRequestId());
        assignment.setTechnicianId(request.getTechnicianId());
        assignment.setStatus("ASSIGNED");
        assignment.setAssignedAt(request.getDate());

        Assignment saved = assignmentRepository.save(assignment);

        return ResponseEntity.ok(saved);
    }

    /**
     * 전체 배정 목록 조회 (GET /api/assignments)
     */
    @GetMapping("/assignments")
    public ResponseEntity<List<Assignment>> getAllAssignments() {
        List<Assignment> assignments = assignmentRepository.findAll();
        return ResponseEntity.ok(assignments);
    }

    /**
     * 방문 가능 기사 조회 (GET /api/assignments/technicians/available)
     */
    @GetMapping("/assignments/technicians/available")
    public ResponseEntity<List<Technician>> getAvailableTechnicians(
            @RequestParam String region,
            @RequestParam String productType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam TimeSlot timeSlot) {

        // 오전 / 오후 시간대 범위 설정 (오전: 09:00 ~ 12:00, 오후: 13:00 ~ 18:00)
        LocalDateTime startTime;
        LocalDateTime endTime;

        if (timeSlot == TimeSlot.MORNING) {
            startTime = date.atTime(9, 0, 0);
            endTime = date.atTime(12, 0, 0);
        } else {
            startTime = date.atTime(13, 0, 0);
            endTime = date.atTime(18, 0, 0);
        }

        // 레포지토리 메서드에 시간 범위 전달
        List<Technician> technicians = technicianRepository.findAvailableTechnicians(region, productType, startTime, endTime);
        return ResponseEntity.ok(technicians);
    }

    /**
     * 시간대 구분용 Enum (프로젝트 내 별도 파일로 존재할 경우 생략 가능)
     */
    public enum TimeSlot {
        MORNING, AFTERNOON
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
        private LocalDateTime date;
        private TimeSlot timeSlot; // 구체적 시간 대신 TimeSlot 수신
    }
}