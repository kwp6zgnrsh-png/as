package dev.as_request.service;

import dev.as_request.dto.ProductResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class ProductService {

    private final RestClient productRestClient;

    public ProductService(@Qualifier("productRestClient") RestClient productRestClient) {
        this.productRestClient = productRestClient;
    }

    public ProductResponse getProduct(Long productId) {
        ProductResponse product = productRestClient.get()
                .uri("/api/products/{productId}", productId)
                .retrieve()
                .body(ProductResponse.class);

        if (product == null) {
            throw new RestClientException("제품 서버의 상세 응답 본문이 비어 있습니다.");
        }
        return product;
    }

    public List<ProductResponse> getProducts() {
        List<ProductResponse> products = productRestClient.get()
                .uri("/api/products")
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductResponse>>() {});

        if (products == null) {
            throw new RestClientException("제품 서버의 응답 본문이 비어 있습니다.");
        }
        return products;
    }
}
