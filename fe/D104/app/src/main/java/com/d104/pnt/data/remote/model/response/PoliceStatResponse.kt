package com.d104.pnt.data.remote.model.response

import com.google.gson.annotations.SerializedName

data class PoliceStatResponse(
    @SerializedName("memberId")
    val memberId: Long,

    @SerializedName("policeStat")
    val policeStat: PoliceStat
)

data class PoliceStat(
    @SerializedName("arrestCount")
    val arrestCount: Int,

    @SerializedName("grade")
    val grade: String,

    @SerializedName("mostArrestsInGame")
    val mostArrestsInGame: Int,

    @SerializedName("totalArrestCount")
    val totalArrestCount: Int
)