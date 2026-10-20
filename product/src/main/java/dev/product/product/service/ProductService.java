package dev.product.product.service;

import dev.product.product.domain.Product;
import dev.product.product.dto.ProductResponse;
import dev.product.product.exception.InvalidProductIdException;
import dev.product.product.exception.ProductNotFoundException;
import dev.product.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse getProduct(Long productId) {
        if (productId == null || productId <= 0) {
            throw new InvalidProductIdException();
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        return new ProductResponse(
                product.productId(),
                product.productName(),
                product.productType()
        );
    }
}
