package com.pnt.pnt_spring.global.api.response;

import com.pnt.pnt_spring.global.api.code.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class CommonResponse<T> extends ResponseEntity<ApiBody<T>> {
    public CommonResponse(T data, String message, HttpStatus code, Long memberId) {
        super(new ApiBody<>(data, message, code.value(),memberId), code);
    }

    public CommonResponse(T data, String message, ErrorCode code, Long memberId) {
        super(new ApiBody<>(data, message, code.getCustomCode(), memberId), code.getStatusCode());
    }
}
