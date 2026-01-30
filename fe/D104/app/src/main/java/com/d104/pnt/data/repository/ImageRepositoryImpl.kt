package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.api.ImageApiService
import com.d104.pnt.data.remote.model.response.PresignedUrlResponse
import com.d104.pnt.domain.model.common.BaseResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class ImageRepositoryImpl @Inject constructor(
    private val imageApiService: ImageApiService
): ImageRepository, BaseRepository() {
    override suspend fun uploadImage(presignedUrl: String, image: File): Result<Unit> {
        return try{
            val mediaType = "image/jpeg".toMediaTypeOrNull() // 서버와 일치 필요
            val requestBody = image.asRequestBody(mediaType)
            val response = imageApiService.uploadImage(presignedUrl, requestBody)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Image upload failed"))
            }
        }
        catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPresignedUrlToProfile(): BaseResult<PresignedUrlResponse> {
        TODO("Not yet implemented")
    }
}