package com.pnt.pnt_spring.global.api.response;

import com.pnt.pnt_spring.global.api.code.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class CommonResponse<T> extends ResponseEntity<ApiBody<T>> {
    public CommonResponse(T data, String message, HttpStatus code) {
        super(new ApiBody<>(data, message, code.value()), code);
    }

    public CommonResponse(T data, String message, ErrorCode code) {
        super(new ApiBody<>(data, message, code.getCustomCode()), code.getStatusCode());
    }
}
