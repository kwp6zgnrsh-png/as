package com.example.technician_server.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assignment")
@Getter @Setter
@NoArgsConstructor
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long assignmentId;

    @Column(name = "request_id", unique = true, nullable = false)
    private Long requestId;

    @Column(name = "technician_id", nullable = false)
    private Long technicianId;

    @Column(nullable = false)
    private String status;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;
}