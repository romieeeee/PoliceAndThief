package com.pnt.pnt_spring.global.api;

import com.pnt.pnt_spring.global.api.response.CommonResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class APIExample {

    @GetMapping
    public CommonResponse<String> testApi() {
        return new CommonResponse<>("hi", "test message", HttpStatus.OK, null);
    }
}
