package com.d104.pnt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.d104.pnt.permission.PermissionDeniedDialog
import com.d104.pnt.permission.RequestEssentialPermissions
import com.d104.pnt.ui.MainScreen
import com.d104.pnt.ui.login.LoginScreen
import com.d104.pnt.ui.login.SignupScreen
import com.d104.pnt.ui.theme.D104Theme
import com.example.d104.utils.helper.PermissionHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import timber.log.Timber


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            D104Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App()
                }
            }
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun App() {
    var currentScreen by remember { mutableStateOf(Screen.Intro) }
    var userName by remember { mutableStateOf("") }
    var requestPermissions by remember { mutableStateOf(false) }
    var showDeniedDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            // 인트로 화면
            Screen.Intro -> {
                IntroScreen(
                    onClick = {
                        currentScreen = Screen.Login
                        Timber.d("Intro -> Login")
                    }
                )
            }

            // 로그인 화면
            Screen.Login -> {
                LoginScreen(
                    onLoginSuccess = { userId ->
                        userName = userId
                        currentScreen = Screen.Main

                        // 로그인 성공 후 권한 체크
                        if (!PermissionHelper.areEssentialPermissionsGranted()) {
                            requestPermissions = true
                        }

                        Timber.d("Login success: $userId")
                    },
                    goToSignup = {
                        currentScreen = Screen.Signup
                    }
                )
            }

            // 회원가입 화면
            Screen.Signup -> {
                SignupScreen(
                    onSuccess = {
                        currentScreen = Screen.Login
                        Timber.d("Signup success -> Login")
                    }
                )
            }

            // 메인 화면
            Screen.Main -> {
                MainScreen(userName = userName)
            }
        }

        // 권한 자동 요청 (로그인 성공 후)
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

        // 권한 거부 시 안내 다이얼로그
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
                        // 권한 없어도 일단 진행 (기능 제한)
                    }
                )
            }
        }
    }
}

private enum class Screen {
    Intro,
    Login,
    Signup,
    Main
}