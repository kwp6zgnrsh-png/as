package dev.as_request.service;

import dev.as_request.dto.AssignmentRequest;
import dev.as_request.dto.AssignmentResponse;
import dev.as_request.dto.CreateRepairRequest;
import dev.as_request.dto.ProductResponse;
import dev.as_request.dto.RepairListResponse;
import dev.as_request.dto.RepairHistoryRequest;
import dev.as_request.dto.RepairResponse;
import dev.as_request.dto.TechnicianResponse;
import dev.as_request.repository.RepairRepository;
import dev.as_request.exception.AssignmentFailedException;
import dev.as_request.exception.ProductServiceException;
import dev.as_request.exception.TechnicianNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public void createRepair(CreateRepairRequest request) {

        ProductResponse product = productService.getProduct(request.productId());

        if(product == null) {
            throw new ProductServiceException("제품 정보를 가져올 수 없습니다.");
        }

        String timeSlot = request.visitDateTime().getHour() < 12 ? "MORNING" : "AFTERNOON";

        List<TechnicianResponse> technicians =
            technicianService.getAvailableTechnicians(
                request.region(),
                product.productType(),
                request.visitDateTime().toLocalDate(),
                timeSlot
            );

        if (technicians.isEmpty()) {
            throw new TechnicianNotFoundException();
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

        if(assignmentResponse == null) {
            throw new AssignmentFailedException("기사 배정 응답 본문이 비어 있습니다.");
        }

        repairRepository.updateStatus(repairResponse.requestId(), "ASSIGNED");
    }

    public List<RepairListResponse> getMyRepairs(RepairHistoryRequest request) {
        return repairRepository.findByCustomerNameAndPhone(request.customerName(), request.phone());
    }

    public List<RepairListResponse> getRepairs() {
        return repairRepository.findAll();
    }

}
