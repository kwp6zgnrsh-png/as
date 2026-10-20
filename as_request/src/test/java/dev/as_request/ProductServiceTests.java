package dev.as_request;

import dev.as_request.dto.ProductResponse;
import dev.as_request.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import dev.as_request.exception.ProductServiceException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ProductServiceTests {

    private MockRestServiceServer server;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://product-service.test");
        server = MockRestServiceServer.bindTo(builder).build();
        productService = new ProductService(builder.build());
    }

    @Test
    void requestsUpstreamProductByIdAndMapsResponse() {
        server.expect(requestTo("http://product-service.test/api/products/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"productId":1,"productName":"공기청정기","productType":"AIR_PURIFIER","_links":{"self":{"href":"/api/products/1"}}}
                        """, MediaType.APPLICATION_JSON));

        assertEquals(new ProductResponse(1L, "공기청정기", "AIR_PURIFIER"), productService.getProduct(1L));
        server.verify();
    }

    @Test
    void propagatesMissingProductError() {
        server.expect(requestTo("http://product-service.test/api/products/999"))
                .andRespond(withResourceNotFound());
        assertThrows(ProductServiceException.class, () -> productService.getProduct(999L));
        server.verify();
    }

    @Test
    void rejectsMissingProductDetailBody() {
        server.expect(requestTo("http://product-service.test/api/products/1"))
                .andRespond(withNoContent());
        assertThrows(ProductServiceException.class, () -> productService.getProduct(1L));
        server.verify();
    }

    @Test
    void requestsUpstreamProductsAndMapsResponse() {
        server.expect(requestTo("http://product-service.test/api/products"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"productId":1,"productName":"공기청정기","productType":"AIR_PURIFIER","_links":{"self":{"href":"/api/products/1"}}}]
                        """, MediaType.APPLICATION_JSON));

        List<ProductResponse> products = productService.getProducts();

        assertEquals(List.of(new ProductResponse(1L, "공기청정기", "AIR_PURIFIER")), products);
        server.verify();
    }

    @Test
    void returnsEmptyListWhenUpstreamHasNoProducts() {
        server.expect(requestTo("http://product-service.test/api/products"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertTrue(productService.getProducts().isEmpty());
        server.verify();
    }

    @Test
    void propagatesUpstreamErrorForGlobalExceptionHandler() {
        server.expect(requestTo("http://product-service.test/api/products"))
                .andRespond(withServerError());
        assertThrows(ProductServiceException.class, productService::getProducts);
        server.verify();
    }

    @Test
    void rejectsMissingResponseBody() {
        server.expect(requestTo("http://product-service.test/api/products"))
                .andRespond(withNoContent());
        assertThrows(ProductServiceException.class, productService::getProducts);
        server.verify();
    }
}
