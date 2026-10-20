package dev.as_request.service;

import dev.as_request.dto.ProductResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    public List<ProductResponse> getProducts() {
        // TODO: 제품 서버의 API를 호출하여 제품 DB에 저장된 목록을 반환합니다.
        // DB 연결 전에는 제품을 임의로 생성하지 않고 빈 목록을 반환합니다.
        return List.of();
    }
}
