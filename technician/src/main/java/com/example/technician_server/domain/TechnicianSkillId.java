package com.example.technician_server.domain;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@NoArgsConstructor
@EqualsAndHashCode
public class TechnicianSkillId implements Serializable {
    private Long technicianId;
    private String category;
}