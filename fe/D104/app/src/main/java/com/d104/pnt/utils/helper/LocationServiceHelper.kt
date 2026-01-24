package com.d104.pnt.utils.helper

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresPermission
import com.d104.pnt.service.location.LocationService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

fun Context.changeLocationMode(isGameMode: Boolean) {
    val intent = Intent(this, LocationService::class.java).apply {
        putExtra(LocationService.EXTRA_GAME_MODE, isGameMode)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        this.startForegroundService(intent)
    } else {
        this.startService(intent)
    }
}

@SuppressLint("MissingPermission")
fun Context.stopLocationService() {
    val intent = Intent(this, LocationService::class.java)
    this.stopService(intent) // 서비스 종료 명령
}

@SuppressLint("MissingPermission")
suspend fun Context.getSingleLocation(): Location? {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    return try {
        // Priority.PRIORITY_HIGH_ACCURACY : 정확도 우선
        // CancellationTokenSource().token : 필요 시 취소 가능 토큰
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).await() // 결과가 나올 때까지 기다림 (Coroutines)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}