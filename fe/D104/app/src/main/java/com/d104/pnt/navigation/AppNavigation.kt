package com.d104.pnt.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.d104.pnt.IntroScreen
import com.d104.pnt.ui.MainScreen
import com.d104.pnt.ui.login.LoginScreen
import com.d104.pnt.ui.login.SignupScreen


@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.INTRO
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

        composable(Routes.SIGNUP) {
            SignupScreen (
                onSuccess = { navController.navigate(Routes.LOGIN) }
            )
        }

        composable("main/{userName}") { backStackEntry ->
            val userName = backStackEntry.arguments?.getString("userName") ?: ""
            MainScreen(userName = userName)
        }
    }
}
