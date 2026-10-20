package com.example.technician_server.controller;

import com.example.technician_server.domain.Assignment;
import com.example.technician_server.domain.Technician;
import com.example.technician_server.repository.AssignmentRepository;
import com.example.technician_server.repository.TechnicianRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam String category) {
        List<Technician> technicians = technicianRepository.findAvailableTechnicians(region, category);
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