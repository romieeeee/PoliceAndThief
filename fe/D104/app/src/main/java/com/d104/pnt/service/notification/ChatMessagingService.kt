package com.d104.pnt.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.d104.pnt.MainActivity
import com.d104.pnt.R
import com.d104.pnt.base.Constants
import com.d104.pnt.data.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ChatMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var authRepository: AuthRepository

    // 새로운 토큰이 생성될 때마다 호출
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("새로운 FCM 토큰 발급됨: $token")

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            authRepository.isLoggedIn().collect { isLoggedIn ->
                if (isLoggedIn) {
                    // 로그인 상태일 때만 active = true로 전송
                    authRepository.sendFcmToken(true)
                    Timber.d("로그인 상태 확인: 서버에 새 토큰 갱신 완료")
                }
            }
        }
    }

    // 메시지 수신
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        if (message.data.isNotEmpty()) {

            Timber.d("알림 수신 : ${message.data}")

            val title = message.data["title"] ?: "새 메시지"
            val body = message.data["body"] ?: ""
            val roomId = message.data["chatRoomId"] ?: ""

            Timber.d("알림 생성 - title: $title, body: $body, roomId: $roomId")

            if (roomId.isNotEmpty()) {
                sendNotification(title, body, roomId)
            } else {
                Timber.w("roomId가 비어있어 알림을 표시하지 않습니다")
            }
        } else {
            Timber.w("FCM 데이터가 비어있습니다")
        }
    }

    private fun sendNotification(title: String, body: String, roomId: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = Constants.CHANNEL_CHAT_ROOM

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel(channelId)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    channelId,
                    "채팅 알림",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "채팅방 메시지 알림"
                    enableVibration(true)
                    enableLights(true)
                }
                notificationManager.createNotificationChannel(channel)
                Timber.d("알림 채널 생성 완료: $channelId")
            }
        }

        // Intent 생성 - roomId를 Extra로 전달
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP

            // roomId를 String으로 전달
            putExtra("roomId", roomId)

            // 알림으로부터 왔다는 표시
            putExtra("from_notification", true)

            Timber.d("Intent 생성 - roomId: $roomId")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            roomId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 알림 빌드
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationId = roomId.hashCode()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}