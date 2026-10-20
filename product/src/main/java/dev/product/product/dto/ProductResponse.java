package dev.product.product.dto;

import java.util.Map;

public record ProductResponse(
        Long productId,
        String productName,
        String productType,
        Map<String, Link> _links
) {
    public ProductResponse(Long productId, String productName, String productType) {
        this(productId, productName, productType, Map.of(
                "self", new Link("/api/products/" + productId),
                "products", new Link("/api/products")
        ));
    }

    public record Link(String href) {
    }
}
