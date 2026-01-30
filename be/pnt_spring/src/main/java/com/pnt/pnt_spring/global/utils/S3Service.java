package com.pnt.pnt_spring.global.utils;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3Service {

	private final S3Presigner s3Presigner;

	@Value("${AWS_S3_BUCKET_NAME}")
	private String bucketName;

	// Presigned URL 생성 메서드
	public String getPresignedUrl(String prefix, String fileName) {
		if (fileName == null || fileName.isBlank()) {
			return null; // 또는 예외 처리
		}

		// 경로 설정 (예: profile/uuid_파일명)
		String path = prefix + "/" + UUID.randomUUID() + "_" + fileName;

		PutObjectRequest objectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(path)
			.contentType("image/jpeg") // 필요 시 파라미터로 받아 동적으로 처리
			.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(5)) // URL 유효 시간 (5분)
			.putObjectRequest(objectRequest)
			.build();

		// 클라이언트가 업로드할 URL 반환
		return s3Presigner.presignPutObject(presignRequest).url().toString();
	}
}