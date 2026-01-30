package com.pnt.pnt_spring.global.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PresignedUrlResponse {
	private String presignedUrl;
	private String imageKey;
}