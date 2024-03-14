package com.heyday.media_player

import ToastUtil
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import com.heyday.pos.mylibrary.iam.http.Login
import com.heyday.pos.mylibrary.notify.http.Channel
import com.heyday.pos.mylibrary.service_package.ServiceGlobal
import com.heyday.pos.mylibrary.service_package.util.*
import com.heyday.pos.mylibrary.storage.http.File
import kotlinx.coroutines.*
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec


class MusicService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var context: Context
    private var index = 0
    private var audioList: List<String>? = null

    companion object {
        private const val NOTIFICATION_ID =
                1 // 通知的唯一标识符
        private const val NOTIFICATION_CHANNEL_ID =
                "MusicServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        CoroutineScope(Dispatchers.IO).launch {
            RabbitMqManager.Global.token.ifEmpty {
                getToken()
            }

            val deferred = File().getFile(
                context = context,
                path = "meida-branch/${ServiceGlobal.brand}",
                project = "swiper"
            )
            if (deferred != null) {
                audioList =
                        deferred.map { it.url!! }
                launch(Dispatchers.Main) {
                    if(!audioList.isNullOrEmpty()) {
                        mediaPlayer = MediaPlayer()
                        mediaPlayer?.setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(
                                    AudioAttributes.CONTENT_TYPE_MUSIC
                                )
                                .build()
                        )
                        mediaPlayer?.setDataSource(
                            audioList!![index]
                        )
                        mediaPlayer?.prepare()
                        mediaPlayer?.start()
                        // 监听音频播放完成事件
                        mediaPlayer?.setOnCompletionListener {
                            playNextAudio()
                        }
                    }
                }
            }
        }
    }

    private fun playNextAudio() {
        // 停止并释放当前的MediaPlayer
        mediaPlayer?.release()
        mediaPlayer = null
        index = (index + 1) % audioList!!.size
        val intent = Intent("ACTION_PLAY_NEXT")
        intent.putExtra("songIndex", index)
        sendBroadcast(intent)
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_MUSIC
                )
                .build()
        )
        mediaPlayer?.setDataSource(
            audioList!![index]
        )
        mediaPlayer?.prepare()

        // 监听音频播放完成事件
        mediaPlayer?.setOnCompletionListener {
            playNextAudio()
        }
        // 开始播放下一个音频
        mediaPlayer?.start()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
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

    fun pauseMusic(): Boolean {
        return if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            true
        } else {
            mediaPlayer?.start()
            false
        }
    }

    fun onclick(index: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                if(audioList == null) {
                    val deferred = File().getFile(
                        context = context,
                        path = "meida-branch/${ServiceGlobal.brand}",
                        project = "swiper"
                    )
                    if (deferred != null) {
                        audioList =
                                deferred.map { it.url!! }
                    }
                }
                CoroutineScope(Dispatchers.Main).launch {
                    mediaPlayer?.release()
                    mediaPlayer = null
                    mediaPlayer = MediaPlayer()
                    mediaPlayer?.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(
                                AudioAttributes.CONTENT_TYPE_MUSIC
                            )
                            .build()
                    )
                    mediaPlayer?.setDataSource(
                        audioList!![index]
                    )
                    mediaPlayer?.prepare()
                    // 监听音频播放完成事件
                    mediaPlayer?.setOnCompletionListener {
                        playNextAudio()
                    }

                    mediaPlayer?.start()
                }
            }
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

    @SuppressLint("Range")
    private suspend fun getToken() {
        val dbHelper = DatabaseUtil(this)
        val db: SQLiteDatabase =
                dbHelper.writableDatabase
        // 首先查询是否已经存在数据
        val query =
                "SELECT * FROM authentication_table"
        val cursor: Cursor = db.rawQuery(query, null)
        try {

                if (cursor.moveToFirst()) {
                    val sn = cursor.getString(
                        cursor.getColumnIndex("sn")
                    )
                    val privateKeyString =
                            cursor.getString(
                                cursor.getColumnIndex("private_key")
                            )
                    val deferred =
                            Channel().deviceRegistration(
                                cid = "media",
                                pac = "com.heyday.media_player",
                                token = sn,
                                context = context
                            )
                    val privateKey: PrivateKey =
                            KeyFactory.getInstance(
                                "RSA"
                            ).generatePrivate(
                                PKCS8EncodedKeySpec(
                                    Base64.decode(
                                        privateKeyString,
                                        Base64.DEFAULT
                                    )
                                )
                            )
                    if (deferred != null) {
                        val signData =
                                RSAUtil().signData(
                                    deferred,
                                    privateKey
                                ).replace("\n", "")
                        val res = Channel().deviceBinding(
                                    pac = "com.heyday.media_player",
                                    cid = "media",
                                    token = sn,
                                    uuid = deferred,
                                    code = signData,
                                    context = context
                                )
                        ServiceGlobal.initToken(
                            uid = "",
                            token = res?.get("token")!!
                        )
                        val device =
                                Channel().getDevice(
                                    context
                                )
                        ServiceGlobal.brand =
                                device?.meta?.brand
                                    ?: ""
                        val de =
                                Login().authLogin(
                                    token = res["token"],
                                    provider = "media",
                                    context = context
                                )
                        if (de != null) {
                            ServiceGlobal.initToken(
                                uid = de.data!!.uid,
                                token = de.data!!.token
                            )
                        }
                        val jwt =
                                res["token"]?.let {
                                    JWTUtil().jwt(
                                        it
                                    )
                                }
                        if (!res["token"]?.isEmpty()!!) {
                            RabbitMqManager.Global.sn =
                                    res["sn"]!!
                            RabbitMqManager.Global.token =
                                    res["token"]!!
                            RabbitMqManager.Global.url =
                                    (jwt?.get("url") as String?)!!
                            RabbitMqManager.Global.routingKey =
                                    (jwt?.get("routingKey") as String?)!!
                            RabbitMqManager.Global.queueName =
                                    (jwt?.get("queueName") as String?)!!
                            RabbitMqManager.Global.exchange =
                                    (jwt?.get("exchange") as String?)!!
                            RabbitMqManager.Global.upRoutingKey =
                                    (jwt?.get("upRoutingKey") as String?)!!
                            startService(
                                Intent(
                                    context,
                                    RabbitMqService::class.java
                                )
                            )
                        }
                    }
                }

        } catch (e: Exception) {
            cursor.close()
            ToastUtil.getInstance()
                .showToast(context, "請聯繫it綁定設備 ")
        }
    }
}
