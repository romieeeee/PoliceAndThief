package com.d104.pnt.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.d104.pnt.IntroScreen
import com.d104.pnt.permission.PermissionDeniedDialog
import com.d104.pnt.permission.RequestEssentialPermissions
import com.d104.pnt.ui.MainScreen
import com.d104.pnt.ui.game.create.GameCreateScreen
import com.d104.pnt.ui.login.LoginScreen
import com.d104.pnt.ui.login.SignupScreen
import com.example.d104.utils.helper.PermissionHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import timber.log.Timber


/**
 * 전체 앱 네비게이션
 * 로그인 전/후 모든 플로우 관리
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(AppScreen.Intro) }
    var userName by remember { mutableStateOf("") }
    var requestPermissions by remember { mutableStateOf(false) }
    var showDeniedDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            // 인트로
            AppScreen.Intro -> {
                IntroScreen(
                    onClick = {
                        currentScreen = AppScreen.Login
                        Timber.d("Intro -> Login")
                    }
                )
            }

            // 로그인
            AppScreen.Login -> {
                LoginScreen(
                    onLoginSuccess = { userId ->
                        userName = userId
                        currentScreen = AppScreen.Main

                        // 로그인 성공 후 필수 권한 체크
                        if (!PermissionHelper.areEssentialPermissionsGranted()) {
                            requestPermissions = true
                        }

                        Timber.d("Login success: $userId")
                    },
                    goToSignup = {
                        currentScreen = AppScreen.Signup
                    }
                )
            }

            // 회원가입
            AppScreen.Signup -> {
                SignupScreen(
                    onSuccess = {
                        currentScreen = AppScreen.Login
                        Timber.d("Signup success -> Login")
                    }
                )
            }

            // 메인 앱 (BottomNav + 게임 화면들)
            AppScreen.Main -> {
                MainScreen(userName = userName)
            }
        }

        // 권한 자동 요청
        if (requestPermissions) {
            RequestEssentialPermissions(
                onAllGranted = {
                    Timber.d("✅ All permissions granted")
                    requestPermissions = false
                },
                onSomeDenied = {
                    Timber.w("❌ Some permissions denied")
                    requestPermissions = false
                    showDeniedDialog = true
                }
            )
        }

        // 권한 거부 다이얼로그
        if (showDeniedDialog) {
            val denied = PermissionHelper.getDeniedPermissions().firstOrNull()
            denied?.let {
                PermissionDeniedDialog(
                    permissionType = it,
                    onOpenSettings = {
                        PermissionHelper.openAppSettings()
                        showDeniedDialog = false
                    },
                    onDismiss = {
                        showDeniedDialog = false
                    }
                )
            }
        }
    }
}

/**
 * 앱 전체 화면 상태
 */
private enum class AppScreen {
    Intro,   // 인트로
    Login,   // 로그인
    Signup,  // 회원가입
    Main     // 메인 앱
}