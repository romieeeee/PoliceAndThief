package com.d104.pnt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.d104.pnt.navigation.AppNavigation
import com.d104.pnt.ui.MainViewModel
import com.d104.pnt.ui.theme.D104Theme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

//        try {
//            val info = packageManager.getPackageInfo(
//                packageName,
//                PackageManager.GET_SIGNATURES
//            )
//            for (signature in info.signatures!!) {
//                val md = MessageDigest.getInstance("SHA")
//                md.update(signature.toByteArray())
//                val keyHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
//                Timber.d("========================================")
//                Timber.d("📱 Current KeyHash: $keyHash")
//                Timber.d("📱 Package: $packageName")
//                Timber.d("📱 Kakao Native Key: ${BuildConfig.KAKAO_NATIVE_APP_KEY}")
//                Timber.d("========================================")
//            }
//        } catch (e: Exception) {
//            Timber.e(e, "Error getting key hash")
//        }


        setContent {
            D104Theme {
                val viewModel: MainViewModel = hiltViewModel()
                val serverMsg by viewModel.message.collectAsState()


                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
