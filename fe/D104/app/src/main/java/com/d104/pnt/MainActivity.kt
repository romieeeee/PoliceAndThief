package com.d104.pnt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.d104.pnt.navigation.Routes
import com.d104.pnt.ui.MainScreen
import com.d104.pnt.ui.component.GifImage
import com.d104.pnt.ui.login.LoginScreen
import com.d104.pnt.ui.login.SignupScreen
import com.d104.pnt.ui.theme.D104Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            D104Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

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

@Composable
fun IntroScreen(onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        GifImage(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = { onClick() }),
            imageRes = R.drawable.intro_game
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets
                        .statusBars
                        .only(WindowInsetsSides.Top)
                )
                .windowInsetsPadding(
                    WindowInsets
                        .navigationBars
                        .only(WindowInsetsSides.Bottom)
                ),
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "<< TOUCH TO START >>",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
