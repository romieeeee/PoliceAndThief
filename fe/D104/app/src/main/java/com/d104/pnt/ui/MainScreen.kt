package com.d104.pnt.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.d104.pnt.navigation.BottomNavBar
import com.d104.pnt.navigation.BottomNavItem
import com.d104.pnt.navigation.Routes
import com.d104.pnt.ui.home.HomeScreen

@Composable
fun MainScreen(userName: String) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel(viewModelStoreOwner = context as ComponentActivity)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // BottomBar를 보여줄 화면들
    val bottomBarRoutes = BottomNavItem.items.map { it.route }

    Box(modifier = Modifier.fillMaxSize()) {
        // NavHost를 먼저 배치 (전체 화면 차지)
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            composable(Routes.HOME) {
                HomeScreen()
            }
        }

        // BottomBar를 위에 띄우기
        if (currentRoute in bottomBarRoutes) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                BottomNavBar(navController)
            }
        }
    }
}