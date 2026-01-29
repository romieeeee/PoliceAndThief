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
        @Query("orders") orders: String = "legalcode"
    ): NaverReverseGeocodeResponse
}