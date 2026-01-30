package com.d104.pnt.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

class StepSensorManager @Inject constructor(
    @ApplicationContext private val context: Context
): SensorEventListener {
    private val sensorManager = ContextCompat.getSystemService(context, SensorManager::class.java)
    private val stepSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    // UI나 ViewModel에서 구독할 Flow
    private val _stepCountFlow = MutableStateFlow<Int>(0)
    val stepCountFlow = _stepCountFlow.asStateFlow()

    fun startListening() {
        if (stepSensor == null) {
            Timber.e("이 기기는 걸음 수 센서를 지원하지 않습니다.")
            return
        }

        // 센서 리스너 등록
        sensorManager!!.registerListener(
            this,
            stepSensor,
            SensorManager.SENSOR_DELAY_UI // UI 갱신용 속도
        )
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    // ⭐ 센서 값이 변할 때마다 호출됨
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                // event.values[0]에 "부팅 이후 총 걸음 수"가 들어있음 (float)
                val totalSteps = it.values[0].toInt()
                _stepCountFlow.value = totalSteps
                Timber.d("현재 센서 누적값: $totalSteps")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {  }
}