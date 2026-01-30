package com.d104.pnt.data.remote.model.response

import com.google.gson.annotations.SerializedName

data class PresignedUrlResponse (
    @SerializedName("uploadUrl")
    val presignedUrl: String,
    @SerializedName("getUrl")
    val getUrl: String
)