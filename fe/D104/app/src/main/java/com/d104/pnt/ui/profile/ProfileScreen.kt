package com.d104.pnt.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d104.pnt.R
import com.d104.pnt.data.remote.model.response.ProfileResponse
import com.d104.pnt.domain.model.common.UiState

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
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
                        val safeAvatarUrl = if (currentProfile.avatarUrl.isNullOrBlank()) "DEFAULT" else currentProfile.avatarUrl

                        viewModel.updateProfile(
                            nickname = newName,
                            avatarUrl = safeAvatarUrl
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
                    val newAvatarUrl = when (selectedImage) {
                        is AvatarImage.Resource -> {
                            when (selectedImage.resId) {
                                R.drawable.profile_img_police_1 -> "POLICE_1"
                                R.drawable.profile_img_police_2 -> "POLICE_2"
                                R.drawable.profile_img_thief_1 -> "THIEF_1"
                                R.drawable.profile_img_thief_2 -> "THIEF_2"
                                else -> "DEFAULT"
                            }
                        }
                        is AvatarImage.Gallery -> selectedImage.uri.toString()
                    }

                    val currentNickname = (profileState as? UiState.Success)?.data?.nickname
                    val safeNickname = if (currentNickname.isNullOrBlank()) "이름 없음" else currentNickname

                    viewModel.updateProfile(
                        nickname = safeNickname,
                        avatarUrl = newAvatarUrl
                    )
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
    val wins = try { profile.stat?.wins ?: 0 } catch (e: Exception) { 0 }
    val totalGames = try { profile.stat?.totalGames ?: 0 } catch (e: Exception) { 0 }
    val policeGrade = try { profile.stat?.policeGrade ?: "Unranked" } catch (e: Exception) { "Unranked" }
    val thiefGrade = try { profile.stat?.thiefGrade ?: "Unranked" } catch (e: Exception) { "Unranked" }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp) // 카드 간 간격
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
                .padding(horizontal = 24.dp)
        )

        // 2. 전적 요약 섹션
        StatSummarySection(
            wins = wins,
            totalGames = totalGames,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}