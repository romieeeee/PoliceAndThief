package com.d104.pnt.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.R
import com.d104.pnt.data.remote.model.response.PresignedUrlResponse
import com.d104.pnt.data.remote.model.response.ProfileResponse
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.ImageRepository
import com.d104.pnt.data.repository.ProfileRepository
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val imageRepository: ImageRepository
) : ViewModel() {

    val memberId: StateFlow<Long> = authRepository.getMemberId()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    // 화면 상태
    private val _profileState = MutableStateFlow<UiState<ProfileResponse>>(UiState.Idle)
    private val _uploadState = MutableStateFlow<UiState<PresignedUrlResponse>>(UiState.Idle)
    val profileState: StateFlow<UiState<ProfileResponse>> = _profileState.asStateFlow()

    init {
        observeMemberId()
    }

    private fun observeMemberId() {
        viewModelScope.launch {
            memberId.collectLatest { id ->
                if (id != 0L) {
                    Timber.d("Member ID 감지됨: $id. 프로필 조회를 시작합니다.")
                    fetchMyProfile(id)
                }
            }
        }
    }

    // 내 프로필 조회
    fun fetchMyProfile(id: Long = memberId.value) {
        if (id == 0L) return

        viewModelScope.launch {
            _profileState.value = UiState.Loading

            val result = profileRepository.getMyProfile(id)

            when (result) {
                is BaseResult.Success -> {
                    _profileState.value = UiState.Success(result.data)
                    Timber.d("프로필 조회 성공")
                }

                is BaseResult.Error -> {
                    _profileState.value = UiState.Error(result.error.message)
                    Timber.e("프로필 조회 실패: ${result.error.message}")
                }
            }
        }
    }

    // 프로필 수정
    fun updateProfile(nickname: String, imageKey: String) {
        val currentId = memberId.value
        if (currentId == 0L) return

        viewModelScope.launch {
            // 수정 요청
            val result = profileRepository.updateProfile(currentId, nickname, imageKey)

            when (result) {
                is BaseResult.Success -> {
                    _profileState.value = UiState.Success(result.data)
                    Timber.d("프로필 수정 성공")
                }

                is BaseResult.Error -> {
                    Timber.e("프로필 수정 실패: ${result.error.message}")
                }
            }
        }
    }

    fun uploadProfileImage(context: Context, selectedImage: AvatarImage, safeNickname: String) {
        viewModelScope.launch {
            _uploadState.value = UiState.Loading

            val uploadFile: File? = when (selectedImage) {
                is AvatarImage.Resource -> {
                    val fileName = when (selectedImage.resId) {
                        R.drawable.profile_img_police_1 -> "police_1.jpeg"
                        R.drawable.profile_img_police_2 -> "police_2.jpeg"
                        R.drawable.profile_img_thief_1 -> "thief_1.jpeg"
                        R.drawable.profile_img_thief_2 -> "thief_2.jpeg"
                        else -> "default.jpeg"
                    }
                    createTmpFileFromDrawable(context, selectedImage.resId, fileName)
                }

                is AvatarImage.Gallery -> {
                    var fileName: String? = null
                    if (selectedImage.uri.scheme == "content") {
                        val cursor = context.contentResolver.query(selectedImage.uri, null, null, null, null)
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (index >= 0) fileName = it.getString(index)
                            }
                        }
                    }
                    if (fileName == null) {
                        fileName = selectedImage.uri.path?.substringAfterLast('/') ?: "unknown.jpg"
                    }

                    // 2. ★ 중요: 실제 데이터 복사 실행 ★
                    createTmpFileFromUri(context, selectedImage.uri, fileName!!)
                }
            }

            if (uploadFile == null || !uploadFile.exists()) {
                Timber.e("파일 생성 실패: 파일이 존재하지 않음")
                _uploadState.value = UiState.Error("이미지 파일을 불러오는데 실패했습니다.")
                return@launch
            }

            val finalFileName = uploadFile.name

            when (val result = imageRepository.getPresignedUrlToProfile(memberId.value, finalFileName)) {
                is BaseResult.Success -> {
                    Timber.d("URL 획득 성공: ${result.data.presignedUrl}")

                    when(val uploadResult = imageRepository.uploadImage(result.data.presignedUrl, uploadFile)) {
                        is BaseResult.Success -> {
                            val imageKey = result.data.imageKey
                            Timber.d("업로드 성공 Key: $imageKey")
                            _uploadState.value = UiState.Success(result.data)
                            updateProfile(safeNickname, imageKey)
                        }
                        is BaseResult.Error -> {
                            _uploadState.value = UiState.Error(uploadResult.error.message)
                        }
                    }
                }
                is BaseResult.Error -> {
                    _uploadState.value = UiState.Error(result.error.message)
                }
            }
        }
    }

    private fun createTmpFileFromUri(context: Context, uri: Uri, fileName: String): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            Timber.e(e, "파일 복사 실패")
            null
        }
    }

    private fun createTmpFileFromDrawable(context: Context, resId: Int, fileName: String): File? {
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, resId)
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            file
        } catch (e: Exception) {
            Timber.e(e, "리소스 변환 실패")
            null
        }
    }

    fun safeLogout() {
        viewModelScope.launch {
            when (val result = authRepository.logout()) {
                is BaseResult.Success -> {}
                is BaseResult.Error -> {
                    Timber.e("로그아웃 실패: ${result.error.message}")
                }
            }
        }
    }

}