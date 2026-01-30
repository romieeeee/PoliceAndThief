package com.d104.pnt.service.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.d104.pnt.data.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    @Inject
    lateinit var LocationRepository: LocationRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 위치 업데이트 시 실행될 콜백 정의
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    LocationRepository.updateCurrentLocation(location)
                    Log.d("LocationService", "Location updated: $location")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // 위치 업데이트 중지
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        // 포그라운드 상태 해제 및 알림 제거
        // API 24 이상 지원 프로젝트이므로 STOP_FOREGROUND_REMOVE 사용
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 알림 채널 생성 (안드로이드 8.0 이상 필수)
        createNotificationChannel()

        // 알림 생성 (사용자에게 앱이 백그라운드에서 실행 중임을 알림)
        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("위치 추적 중")
            .setContentText("백그라운드에서 위치 정보를 수집하고 있습니다.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // 포그라운드 서비스 시작 (ID는 0이 아니어야 함)
        startForeground(100, notification)

        val inGameMode: Boolean = intent?.getBooleanExtra(EXTRA_GAME_MODE, false) ?: false

        // 위치 업데이트 요청 시작
        startLocationUpdates(inGameMode)

        return START_STICKY // 서비스가 강제 종료되어도 다시 시작하도록 설정
    }

    private fun startLocationUpdates(shortInterval: Boolean) {
        val intervalMillis = if (shortInterval) 1000L else 30000L

        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis).apply {
                setMinUpdateIntervalMillis(intervalMillis) // 최소 업데이트 간격
                setMinUpdateDistanceMeters(if (shortInterval) 1.0f else 10.0f) // 최소 갱신 거리
                setWaitForAccurateLocation(false) // 정확한 위치 기다리지 않음 (빠른 갱신 위해)
            }.build()

        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_channel",
                "위치 서비스 채널",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val EXTRA_GAME_MODE = "com.d104.pnt.extra_game_mode"
    }
}