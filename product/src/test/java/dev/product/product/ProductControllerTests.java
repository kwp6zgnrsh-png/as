package dev.product.product;

import dev.product.common.exception.GlobalExceptionHandler;
import dev.product.product.controller.ProductController;
import dev.product.product.domain.Product;
import dev.product.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProductService service = new ProductService(productId -> productId == 1L
                ? Optional.of(new Product(1L, "공기청정기", "AIR_PURIFIER"))
                : Optional.empty());
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsProduct() throws Exception {
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productName").value("공기청정기"))
                .andExpect(jsonPath("$.productType").value("AIR_PURIFIER"));
    }

    @Test
    void returnsNotFoundForMissingProduct() throws Exception {
        mockMvc.perform(get("/products/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("제품을 찾을 수 없습니다."));
    }

    @Test
    void rejectsInvalidIds() throws Exception {
        for (String productId : new String[]{"0", "-1", "abc", "9223372036854775808"}) {
            mockMvc.perform(get("/products/" + productId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PRODUCT_ID"))
                    .andExpect(jsonPath("$.message").value("제품 ID는 1 이상의 정수여야 합니다."));
        }
    }
}
