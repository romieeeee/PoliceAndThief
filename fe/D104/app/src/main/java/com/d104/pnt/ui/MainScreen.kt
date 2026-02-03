package com.d104.pnt.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.d104.pnt.ui.chatroom.chat.ChatRoomScreen
import com.d104.pnt.ui.chatroom.create.ChatRoomCreateScreen
import com.d104.pnt.ui.chatroomlist.ChatRoomListScreen
import com.d104.pnt.ui.component.KickedNoticeDialog
import com.d104.pnt.ui.game.create.GameCreateScreen
import com.d104.pnt.ui.game.end.GameResultScreen
import com.d104.pnt.ui.game.end.news.NewsLoadingScreen
import com.d104.pnt.ui.game.end.news.NewsScreen
import com.d104.pnt.ui.game.load.GameLoadingScreen
import com.d104.pnt.ui.game.play.GamePlayScreen
import com.d104.pnt.ui.game.play.GameRoleScreen
import com.d104.pnt.ui.game.play.mission.CameraScreen
import com.d104.pnt.ui.game.wait.GameRoomScreen
import com.d104.pnt.ui.game.wait.role.RoleSelectScreen
import com.d104.pnt.ui.home.HomeScreen
import com.d104.pnt.ui.profile.ProfileScreen
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun MainScreen(navigateToIntro: () -> Unit) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as? Activity

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // BottomBar 표시 화면
    val bottomBarRoutes = BottomNavItem.items.map { it.route }

    // 뒤로가기 두 번 누르기 처리
    var backPressedTime by remember { mutableLongStateOf(0L) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()


    // BottomNav 화면에서 뒤로가기 처리
    BackHandler(enabled = currentRoute in bottomBarRoutes) {

        if (System.currentTimeMillis() - backPressedTime <= 2000) {
            // 2초 이내에 다시 누르면 앱 종료
            activity?.finish()
        } else {
            // 스낵바 메시지 표시
            backPressedTime = System.currentTimeMillis()
            scope.launch {
                snackbarHostState.showSnackbar("한 번 더 누르면 종료됩니다")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // ===== BottomNav 탭 =====
            composable(Routes.HOME) { backStackEntry ->
                val savedStateHandle = backStackEntry.savedStateHandle
                val kickMessage = savedStateHandle.get<String>("kick_message")

                if (kickMessage != null) {
                    KickedNoticeDialog(
                        reason = kickMessage,
                        onConfirm = {
                            savedStateHandle.remove<String>("kick_message")
                        }
                    )
                }

                HomeScreen(
                    goToGameCreate = {
                        navController.navigate(Routes.GAME_CREATE)
                    },
                    navigateToGameRoom = { roomId ->
                        navController.navigate(Routes.buildRoleSelect(roomId))
                    },
                    navigateToIntro = { navigateToIntro() }
                )
            }

            composable(Routes.CHAT) {
                ChatRoomListScreen(
                    navigateToChatRoom = { chatRoomId ->
                        navController.navigate(Routes.buildChatRoom(chatRoomId))
                    },
                    navigateToChatCreate = {
                        navController.navigate(Routes.CHAT_CREATE)
                    }
                )
            }

            composable(Routes.CHAT_CREATE) {
                ChatRoomCreateScreen(
                    onCancel = { navController.popBackStack() },
                    onConfirm = { chatRoomId ->
                        Timber.d("채팅방 생성 완료, ID: $chatRoomId")

                        // 채팅방 화면으로 이동
                        navController.navigate(Routes.buildChatRoom(chatRoomId)) {
                            // 생성 화면은 스택에서 제거
                            popUpTo(Routes.CHAT) { inclusive = false }
                        }
                    },
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

                ChatRoomScreen(
                    modifier = Modifier.fillMaxSize(),
                    onBackPressed = {
                        navController.popBackStack(Routes.CHAT, inclusive = false)
                    })
            }

            // ===== 게임 대기방 =====
            composable(
                route = "${Routes.GAME_ROOM}/{${NavArgs.ROOM_ID}}/{${NavArgs.ROLE}}",
                arguments = listOf(
                    navArgument(NavArgs.ROOM_ID) { type = NavType.LongType },
                    navArgument(NavArgs.ROLE) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getLong(NavArgs.ROOM_ID) ?: 0L
                val roleString = backStackEntry.arguments?.getString(NavArgs.ROLE) ?: "THIEF"

                GameRoomScreen(
                    roomId = roomId,
                    initialRole = GameRole.fromName(roleString),
                    onStartGame = { gameId, role ->
                        navController.navigate(Routes.buildGamePlay(gameId, role.name)) {
                            popUpTo(Routes.HOME)
                        }
                    },
                    onChangeRole = {
                        navController.navigate(Routes.buildRoleSelect(roomId))
                    },
                    onBackPressed = { navController.popBackStack() },
                    onNavigateRole = { roomId, role ->
                        navController.navigate(Routes.buildGameIntro(roomId, role.name))
                    },
                    onNavigateHome = { message ->
                        if (message != null) {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("kick_message", message)
                        }
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    }
                )
            }

            // ===== 게임 플로우 =====

            // 역할 선택 (대기방 내에서)
            composable(
                route = "${Routes.ROLE_SELECT}/{${NavArgs.ROOM_ID}}",
                arguments = listOf(
                    navArgument(NavArgs.ROOM_ID) { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getLong(NavArgs.ROOM_ID) ?: 0L

                RoleSelectScreen(
                    onRoleSelected = { role: GameRole ->
                        navController.navigate(Routes.buildGameRoom(roomId, role.name)) {
                            popUpTo(Routes.HOME)
                        }
                    },
                    onBackPressed = { navController.popBackStack() },
                )
            }

            // 게임 인트로 (역할 안내)
            composable(
                route = "${Routes.GAME_ROLE}/{${NavArgs.ROOM_ID}}/{${NavArgs.ROLE}}",
                arguments = listOf(
                    navArgument(NavArgs.ROOM_ID) { type = NavType.LongType },
                    navArgument(NavArgs.ROLE) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getLong(NavArgs.ROOM_ID) ?: 0L
                val roleName = backStackEntry.arguments?.getString(NavArgs.ROLE) ?: "THIEF"
                val role = GameRole.fromName(roleName)

                GameRoleScreen(
                    role = role,
                    onIntroFinished = {
                        navController.navigate(Routes.buildGameLoading(roomId, role.name)) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    }
                )
            }

            // 게임 로딩 (카운트다운)
            composable(
                route = "${Routes.GAME_LOADING}/{${NavArgs.ROOM_ID}}/{${NavArgs.ROLE}}",
                arguments = listOf(
                    navArgument(NavArgs.ROOM_ID) { type = NavType.LongType },
                    navArgument(NavArgs.ROLE) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getLong(NavArgs.ROOM_ID) ?: 0L
                val roleName = backStackEntry.arguments?.getString(NavArgs.ROLE) ?: "THIEF"
                val role = GameRole.fromName(roleName)

                GameLoadingScreen(
                    roomId = roomId,
                    role = role,
                    onLoadingComplete = { gameId ->
                        navController.navigate(Routes.buildGamePlay(gameId, role.name)) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    }
                )
            }

            composable(Routes.GAME_CREATE) {
                GameCreateScreen(
                    onCancel = {
                        navController.popBackStack()
                    },
                    onConfirm = { roomId ->
                        navController.navigate(Routes.buildRoleSelect(roomId)) {
                            popUpTo(Routes.HOME)
                        }
                    }
                )
            }

            composable(
                route = "${Routes.MISSION_CAMERA}/{${NavArgs.MISSION_ID}}",
                arguments = listOf(
                    navArgument(NavArgs.MISSION_ID) { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val missionId = backStackEntry.arguments?.getLong(NavArgs.MISSION_ID) ?: 0L
                CameraScreen(
                    onPhotoConfirmed = {
                        navController.popBackStack()
                    },
                    compressionQuality = 80, // 압축 품질 (0-100) - 기본값 80
                    maxWidth = 1280,         // 최대 가로 해상도 - 기본값 1280px
                    maxHeight = 720,         // 최대 세로 해상도 - 기본값 720px
                    missionId = missionId
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
                    onBackToHome = {
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    },
                    onNavigateToLoading = { id ->
                        navController.navigate(Routes.buildNewsLoading(id)) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    },
                    goToCamera = { missionId -> navController.navigate(Routes.buildMissionCamera(missionId)) }
                )
            }

            composable(
                route = "${Routes.GAME_NEWS_LOADING}/{${NavArgs.GAME_ID}}",
                arguments = listOf(
                    navArgument(NavArgs.GAME_ID) { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getLong(NavArgs.GAME_ID) ?: 0L

                NewsLoadingScreen(
                    gameId = gameId,
                    onNewsReady = { gId, nId ->
                        // 분석(소켓+HTTP) 완료 시 gameId와 newsId를 가지고 실제 뉴스로 이동
                        navController.navigate(Routes.buildGameNews(gId, nId)) {
                            popUpTo(Routes.GAME_NEWS_LOADING) { inclusive = true }
                        }
                    }
                )
            }

            // 게임 뉴스
            composable(
                route = "${Routes.GAME_NEWS}/{${NavArgs.GAME_ID}}/{${NavArgs.NEWS_ID}}",
                arguments = listOf(
                    navArgument(NavArgs.GAME_ID) { type = NavType.LongType },
                    navArgument(NavArgs.NEWS_ID) { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getLong(NavArgs.GAME_ID) ?: 0L
                val newsId = backStackEntry.arguments?.getLong(NavArgs.NEWS_ID) ?: 0L

                NewsScreen(
                    gameId = gameId,
                    newsId = newsId,
                    onNextClick = {
                        navController.navigate(Routes.buildGameResult(gameId)) {
                            popUpTo(Routes.GAME_NEWS_LOADING) { inclusive = true }
                        }
                    }
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
                    gameId = gameId,
                    onBackToHome = {
                        // 홈으로 - DisposableEffect에서 이미 게임 소켓 정리됨
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onBackToWaitingRoom = { roomId ->
                        navController.navigate(Routes.buildGameRoom(roomId, GameRole.ANY.roleNameEn)) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    }
                )
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


        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(bottom = 80.dp) // BottomBar 높이 + 여유 공간
        )
    }
}