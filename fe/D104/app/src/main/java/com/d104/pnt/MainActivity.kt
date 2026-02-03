package com.d104.pnt

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.d104.pnt.navigation.AppNavigation
import com.d104.pnt.ui.MainViewModel
import com.d104.pnt.ui.theme.D104Theme
import com.google.android.gms.maps.MapsInitializer
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.O)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val roomId = intent.getStringExtra("roomId")?.toLongOrNull()
        mainViewModel.setPendingChatRoomId(roomId)

        enableEdgeToEdge()
        setContent {
            D104Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // 이제 startChatRoomId를 수동으로 넘길 필요 없음!
                    AppNavigation()
                }
            }
        }
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // 앱이 이미 켜져있는 상태에서 알림을 클릭한 경우
        val roomId = intent.getStringExtra("roomId")?.toLongOrNull()
        mainViewModel.setPendingChatRoomId(roomId)

        handleNotificationIntent(intent)

        Thread {
            try {
                // Context, 렌더러 옵션(LATEST 추천), 콜백
                MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST) { renderer ->
                    when (renderer) {
                        MapsInitializer.Renderer.LATEST -> Timber.d("구글맵 최신 렌더러 초기화 완료")
                        MapsInitializer.Renderer.LEGACY -> Timber.d("구글맵 레거시 렌더러 초기화 완료")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val roomId = intent?.getStringExtra("roomId")
        if (!roomId.isNullOrEmpty()) {
            Timber.d("채팅방 $roomId 로 이동합니다.")
        }
    }
}
