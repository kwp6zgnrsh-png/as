package com.example.technician_server.controller;

import com.example.technician_server.common.TimeSlot;
import com.example.technician_server.domain.Assignment;
import com.example.technician_server.domain.Technician;
import com.example.technician_server.repository.AssignmentRepository;
import com.example.technician_server.repository.TechnicianRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TechnicianController {

    private final TechnicianRepository technicianRepository;
    private final AssignmentRepository assignmentRepository;

    @GetMapping("/technicians/available")
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

        // 수정된 레포지토리 메서드에 시간 범위까지 함께 전달
        List<Technician> technicians = technicianRepository.findAvailableTechnicians(region, productType, startTime, endTime);
        return ResponseEntity.ok(technicians);
    }

    @PostMapping("/assignments")
    public ResponseEntity<Assignment> assignTechnician(@RequestBody AssignmentRequest request) {
        if (assignmentRepository.findByRequestId(request.getRequestId()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        Assignment assignment = new Assignment();
        assignment.setRequestId(request.getRequestId());
        assignment.setTechnicianId(request.getTechnicianId());
        assignment.setStatus("ASSIGNED");
        assignment.setAssignedAt(LocalDateTime.now());

        Assignment saved = assignmentRepository.save(assignment);
        return ResponseEntity.ok(saved);
    }

    @Getter
    public static class AssignmentRequest {
        private Long requestId;
        private Long technicianId;
    }
}