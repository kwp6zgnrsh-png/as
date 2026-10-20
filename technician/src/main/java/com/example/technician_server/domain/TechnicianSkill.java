package com.example.technician_server.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "technician_skill")
@Getter @Setter
@NoArgsConstructor
@IdClass(TechnicianSkillId.class)
public class TechnicianSkill {

    @Id
    @Column(name = "technician_id")
    private Long technicianId;

    @Id
    @Column(name = "product_type") // DB 컬럼명 명시
    private String productType;    // category 대신 productType 사용
}