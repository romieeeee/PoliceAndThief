package com.d104.pnt.data.remote.model.response

import com.google.gson.annotations.SerializedName

/**
 * API 공통 응답 래퍼
 *
 * @param T 실제 데이터 타입
 * @property data 응답 데이터 (nullable, API마다 data 유무가 다를 수 있음)
 * @property message 응답 메시지
 * @property code HTTP 상태 코드
 * @property success 성공 여부 (일부 API에서 사용)
 */
data class BaseResponse<T>(
    @SerializedName("data")
    val data: T? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("code")
    val code: Int? = null,

    @SerializedName("success")
    val success: Boolean? = null
)