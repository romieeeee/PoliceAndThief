package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.model.response.ProfileResponse
import com.d104.pnt.data.remote.model.response.ThiefStatResponse
import com.d104.pnt.data.remote.model.response.PoliceStatResponse
import com.d104.pnt.domain.model.common.BaseResult

interface ProfileRepository {
    suspend fun getMyProfile(memberId: Long): BaseResult<ProfileResponse>
    suspend fun updateProfile(memberId: Long, nickname: String, avatarUrl: String): BaseResult<ProfileResponse>

    suspend fun getThiefStat(memberId: Long): BaseResult<ThiefStatResponse>
    suspend fun getPoliceStat(memberId: Long): BaseResult<PoliceStatResponse>
}