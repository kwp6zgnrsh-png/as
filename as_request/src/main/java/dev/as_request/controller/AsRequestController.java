package dev.as_request.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import dev.as_request.dto.RepairListResponse;
import dev.as_request.dto.RepairHistoryRequest;
import jakarta.validation.Valid;
import dev.as_request.dto.CreateRepairRequest;
import dev.as_request.dto.ProductResponse;
import dev.as_request.service.ProductService;
import dev.as_request.service.RepairService;

import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173, http://172.21.26.98:5173"})
public class AsRequestController {

    private final ProductService productService;
    private final RepairService repairService;

    public AsRequestController(ProductService productService, RepairService repairService) {
        this.productService = productService;
        this.repairService = repairService;
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getProducts() {
        List<ProductResponse> products = productService.getProducts();
        return ResponseEntity.ok(products);
    }

    @PostMapping(value = "/repairs", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<RepresentationModel<?>> createRepair(@RequestBody @Valid CreateRepairRequest request) {

        repairService.createRepair(request);

        RepresentationModel<?> response = new RepresentationModel<>();

        response.add(linkTo(methodOn(AsRequestController.class).getMyRepairs(null)).withRel("search-repairs"));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/repairs/search")
    public ResponseEntity<List<RepairListResponse>> getMyRepairs(
        @Valid @RequestBody RepairHistoryRequest request) {
        return ResponseEntity.ok(repairService.getMyRepairs(request));
    }

    @GetMapping("/repairs")
    public ResponseEntity<List<RepairListResponse>> getRepairs() {
        List<RepairListResponse> repairs = repairService.getRepairs();
        return ResponseEntity.ok(repairs);
    }
}
