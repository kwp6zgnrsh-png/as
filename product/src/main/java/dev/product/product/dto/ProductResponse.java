package dev.product.product.dto;

public record ProductResponse(
        Long productId,
        String productName,
        String productType
) {
}
