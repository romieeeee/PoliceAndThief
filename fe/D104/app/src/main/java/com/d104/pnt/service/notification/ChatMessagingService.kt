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

            val title = message.data["title"] ?: "새 메시지"
            val body = message.data["body"] ?: ""
            val roomId = message.data["roomId"] ?: ""

            sendNotification(title, body, roomId)
        }
    }

    private fun sendNotification(title: String, body: String, roomId: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = Constants.CHANNEL_CHAT_ROOM

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, "채팅 알림", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // 1. 딥링크 URI 생성 (예: pnt://chat/{roomId})
        val deepLinkUri = "pnt://chat/$roomId"

        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = deepLinkUri.toUri() // URI 설정
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("roomId", roomId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            roomId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // 알림 표시 (알림 ID도 roomId.hashCode로 하면 같은 방 알림은 갱신됨)
        notificationManager.notify(roomId.hashCode(), notificationBuilder.build())
    }
}