package com.d104.pnt.data.repository

import com.d104.pnt.data.model.common.ApiError
import com.d104.pnt.data.remote.model.response.BaseResponse
import com.d104.pnt.domain.model.common.BaseResult
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * 모든 Repository의 부모 클래스
 *
 * 역할:
 * - safeApiCall 공통 메서드 제공
 * - 모든 Repository가 상속받아 사용
 */
abstract class BaseRepository {

    /**
     * 공통 API 호출 템플릿
     *
     * 모든 Repository에서 사용 가능
     *
     * 사용 예시:
     * class AuthRepositoryImpl : BaseRepository() {
     *     override suspend fun login(...) = safeApiCall {
     *         apiService.login(...)
     *     }
     * }
     */
    protected suspend fun <T> safeApiCall(
        onSuccess: (suspend (T) -> Unit)? = null,
        apiCall: suspend () -> Response<BaseResponse<T>>
    ): BaseResult<T> {
        return try {
            val response = apiCall()

            if (response.isSuccessful) {
                val body = response.body()

                when {
                    body == null -> {
                        BaseResult.Error(
                            ApiError(
                                message = "응답 데이터가 없습니다",
                                code = response.code(),
                                type = ApiError.ErrorType.UNKNOWN
                            )
                        )
                    }

                    body.data != null -> {
                        // 성공 콜백 실행
                        onSuccess?.invoke(body.data)
                        BaseResult.Success(body.data)
                    }

                    else -> {
                        BaseResult.Error(
                            ApiError(
                                message = body.message ?: "알 수 없는 오류",
                                code = body.code ?: response.code(),
                                type = ApiError.getErrorType(body.code)
                            )
                        )
                    }
                }
            } else {
                // HTTP 오류 (4xx, 5xx)
                BaseResult.Error(
                    ApiError(
                        message = "서버 오류: ${response.message()}",
                        code = response.code(),
                        type = ApiError.getErrorType(response.code())
                    )
                )
            }
        } catch (e: SocketTimeoutException) {
            Timber.e(e, "Timeout error")
            BaseResult.Error(ApiError.timeoutError())
        } catch (e: IOException) {
            Timber.e(e, "Network error")
            BaseResult.Error(ApiError.networkError())
        } catch (e: Exception) {
            Timber.e(e, "Unknown error")
            BaseResult.Error(ApiError.unknownError(e.message ?: "알 수 없는 오류"))
        }
    }
}