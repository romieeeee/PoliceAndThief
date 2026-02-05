package com.d104.pnt.domain.model.common

import com.d104.pnt.data.remote.model.response.ErrorResponse

/**
 * API 에러 정보를 담는 클래스
 *
 * @property message 에러 메시지
 * @property code HTTP 상태 코드
 * @property type 에러 타입 (네트워크, 서버, 알 수 없음 등)
 */
data class ApiError(
    val message: String,
    val code: Int? = null,
    val type: ErrorType = ErrorType.UNKNOWN
) {
    enum class ErrorType {
        NETWORK,        // 네트워크 연결 오류
        SERVER,         // 서버 오류 (5xx)
        CLIENT,         // 클라이언트 오류 (4xx)
        UNAUTHORIZED,   // 인증 오류 (401)
        FORBIDDEN,      // 권한 오류 (403)
        NOT_FOUND,      // 리소스 없음 (404)
        TIMEOUT,        // 타임아웃
        UNKNOWN         // 알 수 없는 오류
    }

    companion object {
        /**
         * HTTP 상태 코드로부터 ErrorType 결정
         */
        fun getErrorType(code: Int?): ErrorType {
            return when (code) {
                in 400..499 -> when (code) {
                    401 -> ErrorType.UNAUTHORIZED
                    403 -> ErrorType.FORBIDDEN
                    404 -> ErrorType.NOT_FOUND
                    else -> ErrorType.CLIENT
                }

                in 500..599 -> ErrorType.SERVER
                else -> ErrorType.UNKNOWN
            }
        }

        /**
         * 네트워크 에러 생성
         */
        fun networkError(message: String = "네트워크 연결을 확인해주세요"): ApiError {
            return ApiError(message = message, type = ErrorType.NETWORK)
        }

        /**
         * 타임아웃 에러 생성
         */
        fun timeoutError(message: String = "요청 시간이 초과되었습니다"): ApiError {
            return ApiError(message = message, type = ErrorType.TIMEOUT)
        }

        /**
         * 알 수 없는 에러 생성
         */
        fun unknownError(message: String = "알 수 없는 오류가 발생했습니다"): ApiError {
            return ApiError(message = message, type = ErrorType.UNKNOWN)
        }
    }

    fun fromErrorBody(errorBody: String?): String {
        return try {
            val gson = com.google.gson.Gson()
            val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
            errorResponse.message
        } catch (e: Exception) {
            "알 수 없는 오류가 발생했습니다"
        }
    }
}