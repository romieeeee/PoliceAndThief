package com.d104.pnt

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.d104.pnt.navigation.AppNavigation
import com.d104.pnt.ui.theme.D104Theme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
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

        // 앱이 꺼져있을 때 알림을 클릭해 들어온 경우
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 앱이 이미 켜져있는 상태에서 알림을 클릭한 경우
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val roomId = intent?.getStringExtra("roomId")
        if (!roomId.isNullOrEmpty()) {
            // TODO: 여기서 roomId를 가지고 채팅방 화면으로 이동하는 로직 구현
            // 예: findNavController().navigate(R.id.chatFragment, bundleOf("roomId" to roomId))
            Timber.d("채팅방 $roomId 로 이동합니다.")
        }
    }
}
