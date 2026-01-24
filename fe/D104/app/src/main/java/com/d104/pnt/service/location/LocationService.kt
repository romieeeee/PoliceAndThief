package com.d104.pnt.service.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log // 디버그
import androidx.core.app.NotificationCompat
import com.d104.pnt.data.repository.LocationRepository
import com.google.android.gms.location.*

class LocationService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 위치 업데이트 시 실행될 콜백 정의
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    // 여기서 위치 정보를 서버로 보내거나 DB에 저장하면 됩니다.
                    // 예: Log.d("LocationService", "위도: ${location.latitude}, 경도: ${location.longitude}")
                    // 디버그용
                    Log.d("LocationService", "위도: ${location.latitude}, 경도: ${location.longitude}")
                    LocationRepository.updateCurrentLocation(location)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // 1. 위치 업데이트 중지 (가장 중요! 배터리 절약)
        // callback이 초기화되어 있는지 확인 후 제거
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        // 2. 포그라운드 상태 해제 및 알림 제거
        // API 24 이상 지원 프로젝트이므로 STOP_FOREGROUND_REMOVE 사용
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. 알림 채널 생성 (안드로이드 8.0 이상 필수)
        createNotificationChannel()

        // 2. 알림 생성 (사용자에게 앱이 백그라운드에서 실행 중임을 알림)
        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("위치 추적 중")
            .setContentText("백그라운드에서 위치 정보를 수집하고 있습니다.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // 아이콘 설정 필요
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // 3. 포그라운드 서비스 시작 (ID는 0이 아니어야 함)
        startForeground(100, notification)

        val inGameMode: Boolean = intent?.getBooleanExtra(EXTRA_GAME_MODE, false) ?: false

        // 4. 위치 업데이트 요청 시작
        startLocationUpdates(inGameMode)

        return START_STICKY // 서비스가 강제 종료되어도 다시 시작하도록 설정
    }

    private fun startLocationUpdates(interval: Boolean) {
        val intervalMillis = if (interval) 1000L else 30000L

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis).apply {
            setMinUpdateIntervalMillis(intervalMillis) // 최소 업데이트 간격
            setMinUpdateDistanceMeters(if (interval) 1.0f else 10.0f) // 최소 갱신 거리
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