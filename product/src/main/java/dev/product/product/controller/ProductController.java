package dev.product.product.controller;

import dev.product.product.dto.ProductResponse;
import dev.product.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable("productId") Long productId) {
        return productService.getProduct(productId);
    }

    @GetMapping
    public List<ProductResponse> getProducts() {
        return productService.getProducts();
    }
}
