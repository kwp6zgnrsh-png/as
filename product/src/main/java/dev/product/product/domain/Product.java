package dev.product.product.domain;

public record Product(
        Long productId,
        String productName,
        String productType
) {
}
