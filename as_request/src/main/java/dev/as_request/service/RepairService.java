package dev.as_request.service;

import dev.as_request.dto.AssignmentRequest;
import dev.as_request.dto.AssignmentResponse;
import dev.as_request.dto.CreateRepairRequest;
import dev.as_request.dto.ProductResponse;
import dev.as_request.dto.RepairResponse;
import dev.as_request.dto.TechnicianResponse;
import dev.as_request.repository.RepairRepository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RepairService {

    private final RepairRepository repairRepository;
    private final ProductService productService;
    private final TechnicianService technicianService;

    public RepairService(RepairRepository repairRepository, ProductService productService,
        TechnicianService technicianService) {
        this.repairRepository = repairRepository;
        this.productService = productService;
        this.technicianService = technicianService;
    }

    public RepairResponse createRepair(CreateRepairRequest request) {
        if(productService.getProduct(request.productId()) == null) {
            throw new RuntimeException("Product not found");
        }

        ProductResponse product = productService.getProduct(request.productId());

        LocalDateTime visitDateTime = request.visitDateTime();

        String timeSlot = visitDateTime.getHour() < 12 ? "MORNING" : "AFTERNOON";

        List<TechnicianResponse> technicians =
            technicianService.getAvailableTechnicians(
                request.region(),
                product.productType(),
                visitDateTime.toLocalDate(),
                timeSlot
            );

        for (TechnicianResponse technician : technicians) {
            System.out.println(technician);
        }

        if(technicians.isEmpty()) {
            throw new RuntimeException("Technician not found");
        }

        Long technicianId = technicians.getFirst().technicianId();

        RepairResponse repairResponse = repairRepository.save(request, product.productId(), technicianId);

        AssignmentResponse assignmentResponse = technicianService.assignTechnician(
            new AssignmentRequest(
                repairResponse.requestId(),
                technicianId,
                request.visitDateTime(),
                timeSlot
            )
        );

        System.out.println(assignmentResponse);

        return repairResponse;
    }

    public List<RepairResponse> getRepairs() {
        return repairRepository.findAll();
    }

}
