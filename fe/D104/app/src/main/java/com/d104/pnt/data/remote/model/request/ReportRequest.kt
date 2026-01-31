package com.d104.pnt.data.remote.model.request

data class ReportRequest(
    val reportedNickname: String,
    val reason: String, // 서버 enum 문자열 (예: "ABUSE")
    val detail: String
)
