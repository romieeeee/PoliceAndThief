package com.pnt.pnt_spring.global.api.code;


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, 104);

    private HttpStatus statusCode;
    private int customCode;

    ErrorCode(HttpStatus statusCode, int customCode) {
        this.statusCode = statusCode;
        this.customCode = customCode;
    }
}