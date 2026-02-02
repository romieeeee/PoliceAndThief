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
) : SensorEventListener {
    private val sensorManager = ContextCompat.getSystemService(context, SensorManager::class.java)
    private val stepSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    // UI나 ViewModel에서 구독할 Flow
    private val _stepCountFlow = MutableStateFlow<Int>(0)
    val stepCountFlow = _stepCountFlow.asStateFlow()

    private var initialSteps: Int = -1

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
        sensorManager?.unregisterListener(this)
    }

    fun resetGameSteps() {
        initialSteps = -1
        _stepCountFlow.value = 0
        Timber.d("걸음 수 카운터 리셋 요청됨")
    }

    // 센서 값이 변할 때마다 호출됨
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                // 현재 디바이스 총 누적 걸음 수
                val currentTotalSteps = it.values[0].toInt()

                // 1. 기준점이 아직 설정되지 않았으면(게임 시작 직후), 현재 값을 기준점으로 설정
                if (initialSteps == -1) {
                    initialSteps = currentTotalSteps
                    Timber.d("게임 시작 기준 걸음 수 설정됨: $initialSteps")
                }

                // 2. 현재 게임 걸음 수 계산 (현재 총값 - 기준값)
                var stepsInGame = currentTotalSteps - initialSteps

                // (예외 처리) 디바이스 재부팅 등으로 센서가 초기화되어 음수가 나올 경우 0으로 처리
                if (stepsInGame < 0) {
                    stepsInGame = 0
                    initialSteps = currentTotalSteps // 기준점 재설정
                }

                _stepCountFlow.value = stepsInGame
                Timber.d("게임 내 걸음 수: $stepsInGame (총: $currentTotalSteps, 기준: $initialSteps)")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}