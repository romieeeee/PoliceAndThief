package com.d104.pnt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.d104.pnt.navigation.AppNavigation
import com.d104.pnt.ui.theme.D104Theme
import com.google.android.gms.maps.MapsInitializer
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
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
}