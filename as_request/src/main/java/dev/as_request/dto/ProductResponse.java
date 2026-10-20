package dev.as_request.dto;

public record ProductResponse(
        Long productId,
        String productName,
        String productType
) {
}
