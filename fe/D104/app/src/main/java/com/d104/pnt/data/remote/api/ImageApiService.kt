package com.d104.pnt.data.remote.api

import com.d104.pnt.data.remote.model.response.BaseResponse
import com.d104.pnt.data.remote.model.response.PresignedUrlForProfileResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface ImageApiService {
    @Headers("X-No-Auth: true")
    @PUT
    suspend fun uploadImage(
        @Url url: String,
        @Body requestBody: RequestBody,
    ): Response<Unit>

    @GET("members/{id}/presigned-url")
    suspend fun getPresignedUrlForProfile(
        @Path("id") id: Long,
        @Query("fileName") fileName: String,
    ): Response<BaseResponse<PresignedUrlForProfileResponse>>
}