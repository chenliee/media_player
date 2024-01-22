package com.heyday.media_player

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.chenliee.library.Global
import com.chenliee.library.utils.OkHttpUtil
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MusicService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var context:Context
    private var index = 0
    private lateinit var audioList: List<Uri>

    companion object {
        private const val NOTIFICATION_ID = 1 // 通知的唯一标识符
        private const val NOTIFICATION_CHANNEL_ID = "MusicServiceChannel"
    }

    @SuppressLint("HardwareIds")
    private fun getSN(context: Context): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Settings.Secure.getString(context.contentResolver,
                Settings.Secure.ANDROID_ID)
        } else Build.SERIAL
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        Global.initBaseUrl(
            devUrl = "https://gateway.dev.heyday-catering.com:20443/storage",
            uatUrl = "https://gateway.uat.heyday-catering.com:20443/storage",
            proUrl = "https://gateway.pro.heyday-catering.com:20443/storage",
        )
        CoroutineScope(Dispatchers.IO).launch {
            val sn = getSN(context)
            val response = OkHttpUtil.getInstance().getJson("/swiper/public/config.json")
            val jsonObject = Gson().fromJson(
                response,
                JsonObject::class.java
            )

            val branch =
                    jsonObject.getAsJsonObject("device")
                        .get(sn)
            if(branch != null) {
                val playlistArray =
                        jsonObject.getAsJsonObject("playlist")
                            .getAsJsonArray(branch.asString)
                val list = mutableListOf<Uri>()
                for (resourceId in playlistArray) {
                    val uriString =
                            "${OkHttpUtil.getBaseUrl()}/swiper/public/" + resourceId.asString
                    list.add(Uri.parse(uriString))
                }

                audioList = list.toList()
                launch(Dispatchers.Main) {
                    mediaPlayer = MediaPlayer.create(
                        context,
                        audioList[index]
                    )
                    mediaPlayer?.start()
                    // 监听音频播放完成事件
                    mediaPlayer?.setOnCompletionListener {
                        playNextAudio()
                    }
                }
            }
        }
    }

    private fun playNextAudio() {
        // 停止并释放当前的MediaPlayer
        mediaPlayer?.release()
        mediaPlayer = null
        index = (index + 1) % audioList.size
        val intent = Intent("ACTION_PLAY_NEXT")
        intent.putExtra("songIndex", index)
        sendBroadcast(intent)
        mediaPlayer = MediaPlayer.create(
            this,
            audioList[index]
        )

        // 监听音频播放完成事件
        mediaPlayer?.setOnCompletionListener {
            playNextAudio()
        }
        // 开始播放下一个音频
        mediaPlayer?.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder {
        return MusicBinder()
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService {
            return this@MusicService
        }
    }

    fun isMusicPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    fun pauseMusic() : Boolean{
        return if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            true
        } else {
            mediaPlayer!!.start()
            false
        }
    }

    fun onclick(index: Int) {
        mediaPlayer?.release()
        mediaPlayer = null
        mediaPlayer =
                MediaPlayer.create(
                    context,
                    audioList[index]
                )
        // 监听音频播放完成事件
        mediaPlayer?.setOnCompletionListener {
            playNextAudio()
        }

        // 开始播放下一个音频
        mediaPlayer?.start()
    }

    private fun createNotification(): Notification {
        // 创建通知渠道（仅适用于 Android 8.0 及更高版本）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Music Service Channel",
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
            0
        )

        return NotificationCompat.Builder(
            this,
            NOTIFICATION_CHANNEL_ID
        )
        .setContentTitle("正在播放音乐")
        .setContentText("点击以打开应用")
        .setSmallIcon(R.mipmap.logo)
        .setContentIntent(pendingIntent)
        .build()
    }

}
