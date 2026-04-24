package com.foodshop.exception;

import lombok.Getter;

@Getter
public class GlobalException extends RuntimeException {
    private final int code;

    public GlobalException(GlobalCode globalCode) {
        super(globalCode.getMessage());
        this.code = globalCode.getCode();
    }

    public GlobalException(GlobalCode globalCode, String customMessage) {
        super(customMessage);
        this.code = globalCode.getCode();
    }
}