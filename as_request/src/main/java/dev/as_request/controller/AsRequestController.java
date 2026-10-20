package dev.as_request.controller;

import dev.as_request.dto.CreateRepairRequest;
import dev.as_request.dto.ProductResponse;
import dev.as_request.dto.RepairResponse;
import dev.as_request.service.ProductService;
import dev.as_request.service.RepairService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AsRequestController {

    private final ProductService productService;
    private final RepairService repairService;

    public AsRequestController(ProductService productService, RepairService repairService) {
        this.productService = productService;
        this.repairService = repairService;
    }

    @GetMapping("/products")
    public List<ProductResponse> getProducts() {
        return productService.getProducts();
    }

    @PostMapping("/repairs")
    @ResponseStatus(HttpStatus.CREATED)
    public RepairResponse createRepair(@RequestBody CreateRepairRequest request) {
        return repairService.createRepair(request);
    }

    @GetMapping("/repairs")
    public List<RepairResponse> getRepairs() {
        return repairService.getRepairs();
    }
}
