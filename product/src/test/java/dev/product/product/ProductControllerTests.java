package dev.product.product;

import dev.product.common.exception.GlobalExceptionHandler;
import dev.product.product.controller.ProductController;
import dev.product.product.domain.Product;
import dev.product.product.service.ProductService;
import dev.product.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        List<Product> products = List.of(
                new Product(1L, "공기청정기", "AIR_PURIFIER"),
                new Product(2L, "에어컨", "AIR_CONDITIONER"),
                new Product(3L, "냉장고", "REFRIGERATOR"),
                new Product(4L, "TV", "TV"),
                new Product(5L, "전자레인지", "MICROWAVE")
        );
        ProductRepository repository = mock(ProductRepository.class);
        when(repository.findAll()).thenReturn(products);
        when(repository.findById(1L)).thenReturn(Optional.of(products.getFirst()));
        when(repository.findById(999L)).thenReturn(Optional.empty());
        ProductService service = new ProductService(repository);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsProduct() throws Exception {
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productName").value("공기청정기"))
                .andExpect(jsonPath("$.productType").value("AIR_PURIFIER"))
                .andExpect(jsonPath("$._links.self.href").value("/api/products/1"))
                .andExpect(jsonPath("$._links.products.href").value("/api/products"));
    }

    @Test
    void returnsProductListWithLinks() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[1].productId").value(2))
                .andExpect(jsonPath("$[1].productName").value("에어컨"))
                .andExpect(jsonPath("$[1].productType").value("AIR_CONDITIONER"))
                .andExpect(jsonPath("$[1]._links.self.href").value("/api/products/2"))
                .andExpect(jsonPath("$[1]._links.products.href").value("/api/products"));
    }

    @Test
    void returnsNotFoundForMissingProduct() throws Exception {
        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("제품을 찾을 수 없습니다."));
    }

    @Test
    void rejectsInvalidIds() throws Exception {
        for (String productId : new String[]{"0", "-1", "abc", "9223372036854775808"}) {
            mockMvc.perform(get("/api/products/" + productId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PRODUCT_ID"))
                    .andExpect(jsonPath("$.message").value("제품 ID는 1 이상의 정수여야 합니다."));
        }
    }
}
