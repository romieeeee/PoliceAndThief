package com.d104.pnt.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.d104.pnt.navigation.BottomNavBar
import com.d104.pnt.navigation.BottomNavItem
import com.d104.pnt.navigation.Routes
import com.d104.pnt.ui.chatroomlist.ChatRoomListScreen
import com.d104.pnt.ui.game.create.GameCreateScreen
import com.d104.pnt.ui.home.HomeScreen
import com.d104.pnt.ui.profile.ProfileScreen

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
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    goToGameCreate = { navController.navigate(Routes.GAME_CREATE) }
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen()
            }

            composable(Routes.CHAT) {
                ChatRoomListScreen()
            }

            composable(Routes.GAME_CREATE) {
                GameCreateScreen()
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