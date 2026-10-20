package dev.as_request.dto;

public record ProductResponse(
        Long productId,
        String modelName,
        String category,
        String manufacturer
) {
}
