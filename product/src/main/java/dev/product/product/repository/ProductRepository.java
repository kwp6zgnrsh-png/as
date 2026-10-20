package dev.product.product.repository;

import dev.product.product.domain.Product;

import java.util.Optional;
import java.util.List;

public interface ProductRepository {
    List<Product> findAll();

    /**
     * 제품 ID로 조회하고, 제품이 없으면 Optional.empty()를 반환한다.
     */
    Optional<Product> findById(Long productId);
}
