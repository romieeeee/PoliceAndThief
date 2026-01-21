package com.d104.pnt.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.d104.pnt.IntroScreen
import com.d104.pnt.ui.MainScreen
import com.d104.pnt.ui.game.create.GameCreateScreen
import com.d104.pnt.ui.login.LoginScreen
import com.d104.pnt.ui.login.SignupScreen


/**
 * 로그인 전 화면 플로우를 관리하는 파일
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main/{userName}"
    ) {
        composable(Routes.INTRO) {
            IntroScreen(
                onClick = {
                    navController.navigate(Routes.LOGIN)
                }
            )
        }

        // 로그인 화면
        composable(Routes.LOGIN) {
            LoginScreen(
                goToSignup = { navController.navigate(Routes.SIGNUP) },
                onLoginSuccess = { userName ->
                    navController.navigate("main/$userName") {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // 회원가입 화면
        composable(Routes.SIGNUP) {
            SignupScreen (
                onSuccess = { navController.navigate(Routes.LOGIN) }
            )
        }

        // 메인 화면
        composable("main/{userName}") { backStackEntry ->
            val userName = backStackEntry.arguments?.getString("userName") ?: ""

            MainScreen(userName = userName)
        }

        composable(Routes.GAME_CREATE) {
            GameCreateScreen()
        }
    }
}
