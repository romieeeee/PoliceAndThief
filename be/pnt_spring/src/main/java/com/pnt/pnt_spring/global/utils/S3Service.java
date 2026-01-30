package com.pnt.pnt_spring.global.utils;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pnt.pnt_spring.global.api.response.PresignedUrlResponse;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3Service {

	private final S3Presigner s3Presigner;

	@Value("${AWS_S3_BUCKET_NAME}")
	private String bucketName;

	/**
	 * Presigned URL 생성 메서드 (업로드용 - PUT)
	 * memberId를 받아 경로에 포함시킵니다.
	 */
	public PresignedUrlResponse getPresignedPutUrl(String prefix, String fileName, Long memberId) {
		if (fileName == null || fileName.isBlank()) {
			return null;
		}

		// 서버에서 UUID를 포함한 최종 경로(Key) 생성
		String imageKey = prefix + "/" + memberId + "/" + UUID.randomUUID() + "_" + fileName;

		PutObjectRequest objectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(imageKey) // 생성한 Key 사용
			.contentType("image/jpeg")
			.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(5))
			.putObjectRequest(objectRequest)
			.build();

		String presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString();

		// URL과 Key를 함께 반환
		return PresignedUrlResponse.builder()
			.presignedUrl(presignedUrl)
			.imageKey(imageKey)
			.build();
	}

	// 2. 조회용 URL (GET) - Private 이미지를 볼 수 있게 함
	public String getPresignedGetUrl(String path) {
		if (path == null || path.isBlank()) return null;

		GetObjectRequest objectRequest = GetObjectRequest.builder()
			.bucket(bucketName)
			.key(path)
			.build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
			.signatureDuration(Duration.ofHours(3)) // 3시간 유효
			.getObjectRequest(objectRequest)
			.build();

		return s3Presigner.presignGetObject(presignRequest).url().toString();
	}
}