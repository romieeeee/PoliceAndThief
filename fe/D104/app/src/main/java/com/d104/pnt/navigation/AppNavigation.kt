package com.d104.pnt.navigation

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.IntroScreen
import com.d104.pnt.base.BaseApplication
import com.d104.pnt.permission.PermissionDeniedDialog
import com.d104.pnt.permission.PermissionDialog
import com.d104.pnt.permission.exitApp
import com.d104.pnt.ui.MainScreen
import com.d104.pnt.ui.MainViewModel
import com.d104.pnt.ui.auth.LoginScreen
import com.d104.pnt.ui.auth.SignupScreen
import com.d104.pnt.util.AuthEventBus
import com.d104.pnt.util.PermissionHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import timber.log.Timber

/**
 * 전체 앱 네비게이션
 * 설정 복귀 시 자동 재확인 처리 개선
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppNavigation(
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

    val authEventBus = remember {
        (context.applicationContext as BaseApplication).authEventBus
    }

    // 현재 화면 상태
    var currentScreen by remember { mutableStateOf(AppScreen.Intro) }
    var userName by remember { mutableStateOf("") }

    // 다이얼로그 표시 상태
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    // 필요한 필수 권한 목록
    val neededPermissions = remember {
        listOf(
            PermissionHelper.PermissionType.CAMERA,
            PermissionHelper.PermissionType.LOCATION,
            PermissionHelper.PermissionType.AUDIO,
            PermissionHelper.PermissionType.NOTIFICATION
        ).filter { it.isRequired() }
    }

    // 모든 권한을 하나의 리스트로 합침
    val allPermissions = remember {
        neededPermissions.flatMap { it.permissions.toList() }
    }

    // 권한 상태 관리
    val permissionsState = rememberMultiplePermissionsState(
        permissions = allPermissions,
        onPermissionsResult = { results ->
            if (results.values.all { it }) {
                // 모두 허용 → 메인 화면
                Timber.d("✅ All permissions granted")
                showPermissionDeniedDialog = false // 다이얼로그 닫기
                currentScreen = AppScreen.Main
            } else {
                // 일부/모두 거부 → 거부 다이얼로그
                Timber.w("❌ Some permissions denied")
                showPermissionDeniedDialog = true
            }
        }
    )

    // 핵심: 설정에서 돌아왔을 때 재확인 (onResume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Timber.d("📱 onResume - checking permissions")

                // 거부 다이얼로그가 떠있는 상태에서 설정에서 돌아온 경우
                if (showPermissionDeniedDialog) {
                    if (PermissionHelper.areEssentialPermissionsGranted(context)) {
                        // 모든 권한 허용됨 → 다이얼로그 닫고 메인으로
                        Timber.d("✅ All permissions granted from settings!")
                        showPermissionDeniedDialog = false
                        currentScreen = AppScreen.Main
                    } else {
                        // 여전히 부족함 → 다이얼로그 유지하고 목록 갱신
                        Timber.d("⚠️ Still missing permissions")
                        // 다이얼로그는 자동으로 getDeniedPermissions()로 갱신됨
                    }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        authEventBus.events.collect { event ->
            when (event) {
                is AuthEventBus.AuthEvent.TokenExpired -> {
                    Timber.w("🔴 Token expired - Auto logout")
                    currentScreen = AppScreen.Intro
                    Toast.makeText(
                        context,
                        "세션이 만료되었습니다. 다시 로그인해주세요.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is AuthEventBus.AuthEvent.Unauthorized -> {
                    currentScreen = AppScreen.Intro
                }
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            // 인트로 화면
            AppScreen.Intro -> {
                IntroScreen(
                    onClick = {
                        if (isLoggedIn) {
                            Timber.d("Navigation: Intro -> Main")
                            currentScreen = AppScreen.Main
                        } else {
                            Timber.d("Navigation: Intro -> Login")
                            currentScreen = AppScreen.Login
                        }

                    }
                )
            }

            // 로그인 화면
            AppScreen.Login -> {
                LoginScreen(
                    onLoginSuccess = { userId ->
                        userName = userId
                        Timber.d("Login success: $userId")
                        currentScreen = AppScreen.Main

                        // 이미 권한이 있는 상태로 로그인
                        if (PermissionHelper.areEssentialPermissionsGranted(context)) {
                            Timber.d("Permissions already granted, navigating to Main")
                        } else {
                            // 권한이 없으면 설명 다이얼로그 표시
                            Timber.d("Permissions needed, showing dialog")
                            showPermissionDialog = true
                        }
                    },
                    goToSignup = {
                        currentScreen = AppScreen.Signup
                        Timber.d("Navigation: Login -> Signup")
                    }
                )
            }

            // 회원가입 화면
            AppScreen.Signup -> {
                SignupScreen(
                    onSuccess = {
                        currentScreen = AppScreen.Login
                        Timber.d("Signup success -> Login")
                    },
                    onBack = {
                        currentScreen = AppScreen.Login
                    }
                )
            }

            // 메인 앱
            AppScreen.Main -> {
                MainScreen(
                    userName = userName,
                    navigateToIntro = {
                        Timber.d("Navigation: Main -> Intro (Logout)")
                        currentScreen = AppScreen.Intro // ⭐ Intro로 변경
                    }
                )
            }
        }

        // 권한 설명 다이얼로그 (최초 요청)
        if (showPermissionDialog) {
            PermissionDialog(
                permissionTypes = neededPermissions,
                onConfirm = {
                    showPermissionDialog = false
                    Timber.d("User confirmed, launching permission request")
                    // 시스템 권한 요청
                    permissionsState.launchMultiplePermissionRequest()
                },
                onDismiss = {
                    // "나중에" 클릭 → 앱 종료
                    showPermissionDialog = false
                    Timber.d("User dismissed permission dialog - exiting app")
                    activity?.let { exitApp(it) }
                }
            )
        }

        // 권한 거부 다이얼로그
        if (showPermissionDeniedDialog) {
            // 실시간으로 거부된 권한 목록 확인
            val deniedPermissions = PermissionHelper.getDeniedPermissions(context)

            // ⭐ 만약 설정에서 모두 허용했으면 다이얼로그 자동으로 안 보임
            if (deniedPermissions.isEmpty()) {
                // 모든 권한 허용됨 → 다이얼로그 닫고 메인으로
                showPermissionDeniedDialog = false
                currentScreen = AppScreen.Main
            } else {
                // 여전히 거부된 권한이 있음 → 다이얼로그 표시
                val isPermanentlyDenied = !permissionsState.shouldShowRationale

                PermissionDeniedDialog(
                    deniedPermissions = deniedPermissions,
                    isPermanentlyDenied = isPermanentlyDenied,
                    onGoToSettings = {
                        // 설정 화면으로 이동
                        Timber.d("Opening app settings")
                        PermissionHelper.openAppSettings(context)
                        // onResume에서 자동으로 재확인됨
                    },
                    onExitApp = {
                        // 앱 종료
                        Timber.d("User chose to exit app")
                        activity?.let { exitApp(it) }
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