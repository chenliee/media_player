package com.heyday.media_player

/**
 *@Author：chenliee
 *@Date：2024/1/22 15:02
 *Describe:
 */
import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.heyday.pos.mylibrary.service_package.util.RabbitMqManager
import java.lang.Runnable

class RabbitMqService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 2 // 通知的唯一标识符
        private const val NOTIFICATION_CHANNEL_ID = "RabbitMq"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        RabbitMqManager.initializeConnectionFactory(applicationContext)
        //RabbitMqManager.printConnection()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        //Log.e("RabbitMqService","我啟動了")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        //Log.e("RabbitMqService","我销毁了")
        RabbitMqManager.closeConnection() //断开消息队列
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createNotification(): Notification {
        // 创建通知渠道（仅适用于 Android 8.0 及更高版本）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "RabbitMq Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(
                channel
            )
        }

        // 创建通知的意图（你可以根据你的需求进行定制）
        val notificationIntent =
                Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(
            this,
            NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle("已連接消息隊列")
            .setContentText("点击以打开应用")
            .setSmallIcon(R.mipmap.logo)
            .setContentIntent(pendingIntent)
            .build()
    }
}
