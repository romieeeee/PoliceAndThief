package com.d104.pnt.ui

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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.navigation.BottomNavBar
import com.d104.pnt.navigation.BottomNavItem
import com.d104.pnt.navigation.NavArgs
import com.d104.pnt.navigation.Routes
import com.d104.pnt.ui.chatroom.ChatRoomCreateScreen
import com.d104.pnt.ui.chatroomlist.ChatRoomListScreen
import com.d104.pnt.ui.game.create.GameCreateScreen
import com.d104.pnt.ui.game.end.GameResultScreen
import com.d104.pnt.ui.game.load.GameLoadingScreen
import com.d104.pnt.ui.game.play.GamePlayScreen
import com.d104.pnt.ui.game.play.GameRoleScreen
import com.d104.pnt.ui.game.play.mission.CameraScreen
import com.d104.pnt.ui.game.wait.GameWaitingScreen
import com.d104.pnt.ui.game.wait.RoleSelectScreen
import com.d104.pnt.ui.home.HomeScreen
import com.d104.pnt.ui.profile.ProfileScreen

@Composable
fun MainScreen(userName: String) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // BottomBar 표시 화면
    val bottomBarRoutes = BottomNavItem.items.map { it.route }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // ===== BottomNav 탭 =====
            composable(Routes.HOME) {
                HomeScreen(
                    goToGameCreate = {
                        navController.navigate(Routes.GAME_CREATE)
                    },
                    navigateToGameRoom = { roomId ->
                        navController.navigate(Routes.ROLE_SELECT)
                    }
                )
            }

            composable(Routes.CHAT) {
                ChatRoomListScreen(
//                    navigateToChatRoom = { chatId ->
//                        navController.navigate(Routes.buildChatRoom(chatId))
//                    }
                    navigateToChatCreate = {
                        navController.navigate(Routes.CHAT_CREATE)
                    }
                )
            }

            composable(Routes.CHAT_CREATE){
                ChatRoomCreateScreen(
                    onCancel = { navController.popBackStack() },
                    onConfirm = { "TODO: 채팅방 생성"},
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen()
            }

            // ===== 채팅방 =====
            composable(
                route = "${Routes.CHAT_ROOM}/{${NavArgs.CHAT_ID}}",
                arguments = listOf(
                    navArgument(NavArgs.CHAT_ID) { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getLong(NavArgs.CHAT_ID) ?: 0L
//                ChatRoomScreen(
//                    chatId = chatId,
//                    onBackPressed = { navController.popBackStack() }
//                )
            }

            // ===== 게임 대기방 =====
            composable(
                route = "${Routes.GAME_ROOM}/{${NavArgs.ROOM_ID}}",
                arguments = listOf(
                    navArgument(NavArgs.ROOM_ID) { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getLong(NavArgs.ROOM_ID) ?: 0L
                GameWaitingScreen(
                    roomId = roomId,
                    onStartGame = { gameId, role ->
                        navController.navigate(Routes.buildGamePlay(gameId, role.name)) {
                            popUpTo(Routes.HOME)
                        }
                    },
                    onBackPressed = { navController.popBackStack() }
                )
            }

            // ===== 게임 플로우 =====

            // 역할 선택 (대기방 내에서)
            composable(Routes.ROLE_SELECT) {
                RoleSelectScreen(
                    onRoleSelected = { role: GameRole ->
                        navController.navigate(Routes.buildGameIntro(role.name))
                    },
                    onBackPressed = { navController.popBackStack() }
                )
            }

            // 게임 인트로 (역할 안내)
            composable(
                route = "${Routes.GAME_ROLE}/{${NavArgs.ROLE}}",
                arguments = listOf(
                    navArgument(NavArgs.ROLE) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val roleName = backStackEntry.arguments?.getString(NavArgs.ROLE) ?: "THIEF"
                val role = GameRole.fromName(roleName)

                GameRoleScreen(
                    role = role,
                    onIntroFinished = {
                        navController.navigate(Routes.buildGameLoading(role.name))
                    }
                )
            }

            // 게임 로딩 (카운트다운)
            composable(
                route = "${Routes.GAME_LOADING}/{${NavArgs.ROLE}}",
                arguments = listOf(
                    navArgument(NavArgs.ROLE) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val roleName = backStackEntry.arguments?.getString(NavArgs.ROLE) ?: "THIEF"
                val role = GameRole.fromName(roleName)

                // (SavedStateHandle로 role 자동 주입)
                GameLoadingScreen(
                    onLoadingComplete = { gameId ->
                        navController.navigate(Routes.buildGamePlay(gameId, role.name)) {
                            popUpTo(Routes.HOME)
                        }
                    }
                )
            }

            composable(Routes.GAME_CREATE) {
                GameCreateScreen(
                    onCancel = {
                        navController.popBackStack()
                    },
                    onConfirm = {
                        // TODO: 게임 방 생성 후 대기방 이동할지 메인으로 갈지 고민중
//                        navController.navigate(
//                            Routes.buildGameRoom(1)
//                        ) {
//                            popUpTo(Routes.HOME)
//                        }
                    }
                )
            }

            composable(Routes.MISSION_CAMERA) {
                CameraScreen(
                    onPhotoConfirmed = { compressedPhotoFile ->
                        // 이미 압축된 파일이 전달됨
//                        viewModel.submitMissionPhoto(compressedPhotoFile)

                        // 또는 다음 화면으로 이동
                         navController.popBackStack()
                    },
                    compressionQuality = 80, // 압축 품질 (0-100) - 기본값 80
                    maxWidth = 1280,         // 최대 가로 해상도 - 기본값 1280px
                    maxHeight = 720          // 최대 세로 해상도 - 기본값 720px
                )
            }


            // 게임 플레이
            composable(
                route = "${Routes.GAME_PLAY}/{${NavArgs.GAME_ID}}/{${NavArgs.ROLE}}",
                arguments = listOf(
                    navArgument(NavArgs.GAME_ID) { type = NavType.LongType },
                    navArgument(NavArgs.ROLE) {
                        type = NavType.StringType
                        defaultValue = "THIEF"
                    }
                )
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getLong(NavArgs.GAME_ID) ?: 0L
                val roleString = backStackEntry.arguments?.getString(NavArgs.ROLE) ?: "THIEF"

                GamePlayScreen(
                    gameId = gameId,
                    role = GameRole.fromName(roleString),
                    onGameEnd = {
                        navController.navigate(Routes.buildGameResult(gameId)) {
                            popUpTo(Routes.HOME)
                        }
                    },
                    goToCamera = { navController.navigate(Routes.MISSION_CAMERA) }
                )
            }

            // 게임 결과
            composable(
                route = "${Routes.GAME_RESULT}/{${NavArgs.GAME_ID}}",
                arguments = listOf(
                    navArgument(NavArgs.GAME_ID) { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getLong(NavArgs.GAME_ID) ?: 0L
                GameResultScreen(
//                    gameId = gameId,
//                    onViewNews = { newsId ->
//                        navController.navigate(Routes.buildGameNews(newsId))
//                    },
//                    onBackToHome = {
//                        navController.navigate(Routes.HOME) {
//                            popUpTo(Routes.HOME) { inclusive = true }
//                        }
//                    },
//                    onPlayAgain = {
//                        navController.navigate(Routes.ROLE_SELECT)
//                    }
                )
            }

            // 게임 뉴스
            composable(
                route = "${Routes.GAME_NEWS}/{${NavArgs.NEWS_ID}}",
                arguments = listOf(
                    navArgument(NavArgs.NEWS_ID) { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val newsId = backStackEntry.arguments?.getLong(NavArgs.NEWS_ID) ?: 0L
//                GameNewsScreen(
//                    newsId = newsId,
//                    onBackPressed = { navController.popBackStack() }
//                )
            }


            // ===== 신고 =====
            composable(
                route = "${Routes.REPORT}/{${NavArgs.NICKNAME}}",
                arguments = listOf(
                    navArgument(NavArgs.NICKNAME) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val nickname = backStackEntry.arguments?.getString(NavArgs.NICKNAME) ?: ""
//                ReportScreen(
//                    reportedNickname = nickname,
//                    onReportSubmitted = { navController.popBackStack() },
//                    onBackPressed = { navController.popBackStack() }
//                )
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