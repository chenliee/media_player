package com.heyday.media_player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build


/**
 *@Author：chenliee
 *@Date：2023/10/26 17:32
 *Describe:
 */
class BootReceiver :
    BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            // 启动你的后台服务来播放音乐
            val serviceIntent = Intent(
                context,
                MusicService::class.java
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}