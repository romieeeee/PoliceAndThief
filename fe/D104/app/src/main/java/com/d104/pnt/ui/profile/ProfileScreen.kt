package com.d104.pnt.ui.profile

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d104.pnt.R
import com.d104.pnt.data.remote.model.response.ProfileResponse
import com.d104.pnt.domain.model.common.UiState
import timber.log.Timber

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val profileState by viewModel.profileState.collectAsState()
    var showImageDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 배경 화면
        Image(
            painter = painterResource(id = R.drawable.bg_night),
            contentDescription = "배경 화면",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        when (val state = profileState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            is UiState.Success -> {
                val currentProfile = state.data

                ProfileContent(
                    profile = currentProfile,
                    onLogoutClick = { /* 로그아웃 */ },
                    onUpdateNickname = { newName ->
                        val safeAvatarUrl =
                            if (currentProfile.avatarUrl.isNullOrBlank()) "default.jpeg" else currentProfile.avatarUrl

                        viewModel.updateProfile(
                            nickname = newName,
                            imageKey = safeAvatarUrl
                        )
                    },
                    onUpdateAvatar = {
                        showImageDialog = true
                    }
                )
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "정보를 불러오지 못했습니다.\n${state.message}",
                        color = Color.Red
                    )
                }
            }

            else -> {} // Idle
        }

        // 이미지 변경 다이얼로그
        if (showImageDialog) {
            val currentAvatarUrl = (profileState as? UiState.Success)?.data?.avatarUrl

            ProfileImageSelectionDialog(
                currentAvatarUrl = currentAvatarUrl,
                onDismissRequest = { showImageDialog = false },
                onImageSelected = { selectedImage ->
                    val currentNickname = (profileState as? UiState.Success)?.data?.nickname
                    val safeNickname =
                        if (currentNickname.isNullOrBlank()) "이름 없음" else currentNickname
                    viewModel.uploadProfileImage(context, selectedImage, safeNickname)
                }
            )
        }
    }
}

@Composable
fun ProfileContent(
    profile: ProfileResponse,
    onLogoutClick: () -> Unit,
    onUpdateNickname: (String) -> Unit,
    onUpdateAvatar: () -> Unit = {}
) {
    // 안전한 데이터 추출
    val wins = try {
        profile.stat?.wins ?: 0
    } catch (e: Exception) {
        0
    }
    val totalGames = try {
        profile.stat?.totalGames ?: 0
    } catch (e: Exception) {
        0
    }
    val policeGrade = try {
        profile.stat?.policeGrade ?: "Unranked"
    } catch (e: Exception) {
        "Unranked"
    }
    val thiefGrade = try {
        profile.stat?.thiefGrade ?: "Unranked"
    } catch (e: Exception) {
        "Unranked"
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(40.dp) // 카드 간 간격
    ) {
        Spacer(modifier = Modifier.height(40.dp)) // 상단 여백

        // 1. 메인 프로필 카드 (아바타 + 닉네임 + 등급 통합됨)
        ProfileCardSection(
            nickname = profile.nickname ?: "이름 없음",
            avatarUrl = profile.avatarUrl,
            policeGrade = policeGrade,
            thiefGrade = thiefGrade,
            onLogoutClick = onLogoutClick,
            onUpdateNickname = onUpdateNickname,
            onUpdateAvatar = onUpdateAvatar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp)
        )

        // 전적 요약 섹션
        StatSummarySection(
            wins = wins,
            totalGames = totalGames,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp)
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}