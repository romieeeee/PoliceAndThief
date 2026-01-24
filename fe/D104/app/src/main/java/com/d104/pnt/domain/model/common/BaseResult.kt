package com.d104.pnt.domain.model.common

import com.d104.pnt.data.model.common.ApiError

/**
 * Repository 레이어의 공통 Result 타입
 *
 * Success: 성공 시 데이터 반환
 * Error: 실패 시 에러 정보 반환
 */
sealed class BaseResult<out T> {
    data class Success<T>(val data: T) : BaseResult<T>()
    data class Error(val error: ApiError) : BaseResult<Nothing>()

    /**
     * 성공 여부 확인
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * 실패 여부 확인
     */
    val isError: Boolean
        get() = this is Error

    /**
     * 성공 시 데이터 가져오기 (null 가능)
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    /**
     * 실패 시 에러 가져오기 (null 가능)
     */
    fun errorOrNull(): ApiError? = when (this) {
        is Success -> null
        is Error -> error
    }
}