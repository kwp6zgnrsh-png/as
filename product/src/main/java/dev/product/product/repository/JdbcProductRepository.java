package dev.product.product.repository;

import dev.product.product.domain.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JdbcProductRepository implements ProductRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Product> findById(Long productId) {
        return jdbcTemplate.query("""
                SELECT product_id, product_name, product_type
                FROM product
                WHERE product_id = ?
                """, (rs, rowNum) -> new Product(
                rs.getLong("product_id"),
                rs.getString("product_name"),
                rs.getString("product_type")
        ), productId).stream().findFirst();
    }
}
