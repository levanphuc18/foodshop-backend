package com.foodshop.dto;

import com.foodshop.exception.GlobalCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;

    public ApiResponse(GlobalCode globalCode, String message, T data) {
        this.code = globalCode.getCode();
        this.message = message;
        this.data = data;
    }

    public ApiResponse(GlobalCode globalCode, T data) {
        this.code = globalCode.getCode();
        this.message = globalCode.getMessage();
        this.data = data;
    }
}

