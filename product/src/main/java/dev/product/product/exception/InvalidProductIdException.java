package dev.product.product.exception;

public class InvalidProductIdException extends RuntimeException {

    public InvalidProductIdException() {
        super("제품 ID는 1 이상의 정수여야 합니다.");
    }
}
