package com.d104.pnt.domain.model

data class NaverReverseGeocodeResponse(
    val status: Status,
    val results: List<NaverResult>
)

data class Status(
    val code: Int,
    val name: String,
    val message: String
)

data class NaverResult(
    val name: String, // region type (legalcode, admcode 등)
    val region: Region,
    val code: Code
)

data class Region(
    val area1: Area, // 시/도
    val area2: Area, // 시/구/군
    val area3: Area, // 동/읍/면
    val area4: Area  // 리 (잘 안씀)
)

data class Area(
    val name: String,
    val coords: Coords // 필요하면 사용
)

data class Coords(
    val center: Center
)

data class Center(
    val crs: String,
    val x: Float,
    val y: Float
)

data class Code(
    val id: String,
    val type: String,
    val mappingId: String
)