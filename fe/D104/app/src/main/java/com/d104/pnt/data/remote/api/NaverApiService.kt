package com.d104.pnt.data.remote.api

import com.d104.pnt.domain.model.NaverReverseGeocodeResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NaverApiService {
    @GET("map-reversegeocode/v2/gc")
    suspend fun getAddress(
        @Header("x-ncp-apigw-api-key-id") clientId: String,
        @Header("x-ncp-apigw-api-key") clientSecret: String,
        @Query("coords") coords: String, // "경도,위도" (문자열)
        @Query("output") output: String = "json",
        // ⭐️ 중요: admcode(행정동), legalcode(법정동) 등을 요청.
        // roadaddr(도로명)을 빼면 지번 주소 위주로 줍니다.
        @Query("orders") orders: String = "legalcode,admcode"
    ): NaverReverseGeocodeResponse
}