package com.d104.pnt.data.remote.api

import com.d104.pnt.data.remote.model.request.UpdateProfileRequest
import com.d104.pnt.data.remote.model.response.BaseResponse
import com.d104.pnt.data.remote.model.response.ProfileResponse
import com.d104.pnt.data.remote.model.response.ThiefStatResponse
import com.d104.pnt.data.remote.model.response.PoliceStatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface ProfileApiService {

    @GET("members/{id}")
    suspend fun getMyProfile(
        @Path("id") memberId: Long
    ): Response<BaseResponse<ProfileResponse>>

    @GET("members/{id}/thief")
    suspend fun getThiefStat(
        @Path("id") memberId: Long
    ): Response<BaseResponse<ThiefStatResponse>>

    @GET("members/{id}/police")
    suspend fun getPoliceStat(
        @Path("id") memberId: Long
    ): Response<BaseResponse<PoliceStatResponse>>

    @PATCH("members/{id}")
    suspend fun updateProfile(
        @Path("id") memberId: Long,
        @Body request: UpdateProfileRequest
    ): Response<BaseResponse<ProfileResponse>>
}
