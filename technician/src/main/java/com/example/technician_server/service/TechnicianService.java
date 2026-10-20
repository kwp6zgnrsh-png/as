package com.example.technician_server.service;

import com.example.technician_server.common.TimeSlot;
import com.example.technician_server.domain.Technician;
import com.example.technician_server.repository.TechnicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TechnicianService {

    private final TechnicianRepository technicianRepository;

    public List<Technician> findAvailableTechnicians(String region, String productType, LocalDate date, TimeSlot timeSlot) {
        LocalDateTime startTime;
        LocalDateTime endTime;

        if (timeSlot == TimeSlot.MORNING) {
            // 오전: 09:00 ~ 12:00
            startTime = date.atTime(9, 0, 0);
            endTime = date.atTime(12, 0, 0);
        } else {
            // 오후: 13:00 ~ 18:00
            startTime = date.atTime(13, 0, 0);
            endTime = date.atTime(18, 0, 0);
        }

        return technicianRepository.findAvailableTechnicians(region, productType, startTime, endTime);
    }
}