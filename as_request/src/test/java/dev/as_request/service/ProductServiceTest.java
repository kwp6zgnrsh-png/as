package dev.as_request.service;

import dev.as_request.dto.ProductResponse;
import dev.as_request.exception.ProductServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final String BASE_URL = "http://product-service.test";

    @Mock
    private ClientHttpRequestFactory requestFactory;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(RestClient.builder()
                .baseUrl(BASE_URL).requestFactory(requestFactory).build());
    }

    @Test
    @DisplayName("제품 목록을 조회해 제품 정보를 반환한다")
    void getProductsReturnsProducts() throws IOException {
        // Given
        URI uri = URI.create(BASE_URL + "/api/products");
        given(requestFactory.createRequest(uri, HttpMethod.GET)).willReturn(responseRequest("""
                [{"productId":101,"productName":"공기청정기","productType":"AIR_PURIFIER"}]
                """, HttpStatus.OK));

        // When
        List<ProductResponse> result = productService.getProducts();

        // Then
        assertThat(result).containsExactly(new ProductResponse(101L, "공기청정기", "AIR_PURIFIER"));
        then(requestFactory).should().createRequest(uri, HttpMethod.GET);
    }

    @Test
    @DisplayName("제품 서버가 빈 배열을 반환하면 빈 목록을 반환한다")
    void getProductsReturnsEmptyList() throws IOException {
        // Given
        given(requestFactory.createRequest(URI.create(BASE_URL + "/api/products"), HttpMethod.GET))
                .willReturn(responseRequest("[]", HttpStatus.OK));

        // When
        List<ProductResponse> result = productService.getProducts();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("제품 ID로 상세 정보를 조회한다")
    void getProductReturnsProduct() throws IOException {
        // Given
        URI uri = URI.create(BASE_URL + "/api/products/101");
        given(requestFactory.createRequest(uri, HttpMethod.GET)).willReturn(responseRequest("""
                {"productId":101,"productName":"공기청정기","productType":"AIR_PURIFIER"}
                """, HttpStatus.OK));

        // When
        ProductResponse result = productService.getProduct(101L);

        // Then
        assertThat(result).isEqualTo(new ProductResponse(101L, "공기청정기", "AIR_PURIFIER"));
        then(requestFactory).should().createRequest(uri, HttpMethod.GET);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("제품 목록 또는 상세 응답 본문이 없으면 예외를 발생시킨다")
    void rejectsEmptyResponse(boolean detail) throws IOException {
        // Given
        String path = detail ? "/api/products/101" : "/api/products";
        given(requestFactory.createRequest(URI.create(BASE_URL + path), HttpMethod.GET))
                .willReturn(responseRequest("", HttpStatus.OK));

        // When
        Throwable thrown = catchThrowable(() -> {
            if (detail) productService.getProduct(101L);
            else productService.getProducts();
        });

        // Then
        assertThat(thrown).isInstanceOf(ProductServiceException.class)
                .hasMessageContaining("응답 본문이 비어 있습니다");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("제품 서버 HTTP 오류를 제품 서비스 예외로 변환한다")
    void wrapsServerError(boolean detail) throws IOException {
        // Given
        String path = detail ? "/api/products/101" : "/api/products";
        given(requestFactory.createRequest(URI.create(BASE_URL + path), HttpMethod.GET))
                .willReturn(responseRequest("{}", HttpStatus.INTERNAL_SERVER_ERROR));

        // When
        Throwable thrown = catchThrowable(() -> {
            if (detail) productService.getProduct(101L);
            else productService.getProducts();
        });

        // Then
        assertThat(thrown).isInstanceOf(ProductServiceException.class)
                .hasMessageContaining("제품 서버에서 정보를 가져오지 못했습니다")
                .hasCauseInstanceOf(org.springframework.web.client.RestClientException.class);
    }

    @Test
    @DisplayName("제품 서버 연결 실패를 제품 서비스 예외로 변환한다")
    void wrapsConnectionFailure() throws IOException {
        // Given
        IOException failure = new IOException("connection refused");
        given(requestFactory.createRequest(URI.create(BASE_URL + "/api/products"), HttpMethod.GET))
                .willThrow(failure);

        // When
        Throwable thrown = catchThrowable(productService::getProducts);

        // Then
        assertThat(thrown).isInstanceOf(ProductServiceException.class).hasRootCause(failure);
    }

    private MockClientHttpRequest responseRequest(String body, HttpStatus status) {
        MockClientHttpResponse response = new MockClientHttpResponse(body.getBytes(StandardCharsets.UTF_8), status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        MockClientHttpRequest request = new MockClientHttpRequest();
        request.setResponse(response);
        return request;
    }
}
