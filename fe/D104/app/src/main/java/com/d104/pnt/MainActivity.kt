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

        handleNotificationIntent(intent)

        enableEdgeToEdge()

        initializeGoogleMaps()

        setContent {
            D104Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        Timber.d("handleNotificationIntent - extras: ${intent?.extras?.keySet()?.joinToString()}")

        val roomId = intent?.getStringExtra("roomId")?.toLongOrNull()
        val fromNotification = intent?.getBooleanExtra("from_notification", false) ?: false

        Timber.d("Extracted - roomId: $roomId, fromNotification: $fromNotification")

        if (roomId != null) {
            Timber.d("알림에서 채팅방 $roomId 로 이동 준비")
            mainViewModel.setPendingChatRoomId(roomId)
        }
    }

    private fun initializeGoogleMaps() {
        Thread {
            try {
                MapsInitializer.initialize(
                    applicationContext,
                    MapsInitializer.Renderer.LATEST
                ) { renderer ->
                    when (renderer) {
                        MapsInitializer.Renderer.LATEST ->
                            Timber.d("구글맵 최신 렌더러 초기화 완료")
                        MapsInitializer.Renderer.LEGACY ->
                            Timber.d("구글맵 레거시 렌더러 초기화 완료")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "구글맵 초기화 실패")
            }
        }.start()
    }

}
