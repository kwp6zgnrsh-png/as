package dev.as_request.service;

import dev.as_request.dto.ProductResponse;
import dev.as_request.exception.ProductServiceException;
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
        ProductResponse product;
        try {
            product = productRestClient.get()
                    .uri("/api/products/{productId}", productId)
                    .retrieve()
                    .body(ProductResponse.class);
        } catch (RestClientException exception) {
            throw new ProductServiceException(
                    "제품 서버에서 정보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.", exception);
        }

        if (product == null) {
            throw new ProductServiceException("제품 서버의 상세 응답 본문이 비어 있습니다.");
        }
        return product;
    }

    public List<ProductResponse> getProducts() {
        List<ProductResponse> products;
        try {
            products = productRestClient.get()
                    .uri("/api/products")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ProductResponse>>() {});
        } catch (RestClientException exception) {
            throw new ProductServiceException(
                    "제품 서버에서 정보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.", exception);
        }

        if (products == null) {
            throw new ProductServiceException("제품 서버의 응답 본문이 비어 있습니다.");
        }
        return products;
    }
}
