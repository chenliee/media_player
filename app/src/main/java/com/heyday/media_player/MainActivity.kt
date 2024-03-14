package com.heyday.media_player

import android.annotation.SuppressLint
import android.content.*
import android.net.Uri
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.stetho.Stetho
import com.heyday.media_player.demo.MediaObserve
import com.heyday.pos.mylibrary.service_package.ServiceGlobal
import com.heyday.pos.mylibrary.service_package.util.RSAUtil
import com.heyday.pos.mylibrary.service_package.util.RabbitMqManager
import com.heyday.pos.mylibrary.storage.http.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import java.util.*


/**
 *@Author：chenliee
 *@Date：2023/10/17 09:16
 *Describe:
 */

class MainActivity : AppCompatActivity(),
    View.OnClickListener {
    private var listView: RecyclerView? = null
    private lateinit var context: Context
    private lateinit var snView: TextView
    private lateinit var serviceSnView: TextView
    private lateinit var brandCode: TextView
    private lateinit var emptyView: TextView
    private lateinit var play: ImageView
    private lateinit var next: ImageView
    private lateinit var adapter: MediaPlayerAdapter
    private lateinit var nameList: List<String?>
    private var musicService: MusicService? = null
    private var musicServiceBound = false

    private val connection = object :
        ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            val binder =
                    service as MusicService.MusicBinder
            musicService = binder.getService()
            musicServiceBound = true
            // 在这里获取音乐播放状态并决定是否播放音乐
            if (musicService?.isMusicPlaying() == true) {

            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            musicServiceBound = false
        }
    }

    private val playNextReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?
                ) {
                    val songIndex =
                            intent?.getIntExtra(
                                "songIndex",
                                0
                            )
                    for (i in 0 until listView!!.childCount) {
                        val previousSelectedItem =
                                listView!!.getChildAt(
                                    i
                                )
                        if (i == songIndex) {
                            previousSelectedItem?.setBackgroundColor(
                                resources.getColor(
                                    R.color.gray
                                )
                            )
                        } else {
                            previousSelectedItem?.setBackgroundColor(
                                resources.getColor(
                                    R.color.backGround
                                )
                            )
                        }
                    }
                }
            }

    override fun onStart() {
        super.onStart()
        val serviceIntent =
                Intent(this, MusicService::class.java)
        bindService(
            serviceIntent,
            connection,
            Context.BIND_AUTO_CREATE
        )
        val filter = IntentFilter("ACTION_PLAY_NEXT")
        registerReceiver(playNextReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        if (musicServiceBound) {
            unbindService(connection)
            musicServiceBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(playNextReceiver)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this
        Stetho.initializeWithDefaults(this)

        CoroutineScope(Dispatchers.IO).launch {
            val sn = RSAUtil().getSN(context)
            val deferred = MediaPlayerManager.getAudioList(context)

            nameList = deferred.map { it.name }
            if(nameList.isEmpty()) {
                emptyView = findViewById(R.id.empty)
                emptyView.visibility = View.VISIBLE
            }
            launch(Dispatchers.Main) {
                adapter = MediaPlayerAdapter(
                    nameList,
                )
                listView =
                        findViewById<View>(R.id.recyclerView) as RecyclerView
                listView!!.layoutManager =
                        LinearLayoutManager(context)
                listView!!.adapter = adapter

                listView!!.addOnItemTouchListener(
                    RecyclerView.SimpleOnItemTouchListener()
                )

                listView!!.addOnItemTouchListener(
                    RecyclerItemClickListener(
                        context,
                        listView!!,
                        object :
                            RecyclerItemClickListener.OnItemClickListener {
                            override fun onItemClick(
                                view: View,
                                position: Int
                            ) {
                                adapter.setSelectedIndex(position)
                                if (musicServiceBound) {
                                    musicService?.onclick(position)
                                }
                            }

                            override fun onLongItemClick(
                                view: View,
                                position: Int
                            ) {
                                // 处理 item 长按事件
                            }
                        })
                )

            }
            launch(Dispatchers.Main) {
                snView = findViewById(R.id.sn)
                snView.text = "设备SN: $sn"
                serviceSnView =
                        findViewById(R.id.service_sn)
                serviceSnView.text =
                        "服務SN: ${RabbitMqManager.Global.sn}"
                brandCode =
                        findViewById(R.id.brand_code)
                brandCode.text =
                        "門店代號: ${ServiceGlobal.brand}"
            }
        }

        play = findViewById(R.id.play)
        play.setOnClickListener(this)

        val serviceIntent = Intent(
            context,
            MusicService::class.java
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(
                serviceIntent
            )
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun pauseMusic(): Boolean? {
        return if (musicServiceBound) {
            musicService?.pauseMusic()
        } else {
            null
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.play -> {
                if(nameList.isEmpty()) {
                    return
                }
                if (musicService?.isMusicPlaying() == true) {
                    play.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                } else {
                    play.setImageResource(R.drawable.ic_baseline_pause_24)
                }
                pauseMusic()
            }
            else -> {

            }
        }
    }

}

/*
class MainActivity : AppCompatActivity(),
    View.OnClickListener {
    private var listView: RecyclerView? = null
    private var mediaPlayer: MediaPlayer? = null
    private var index = 0
    private lateinit var context: Context
    private lateinit var snView: TextView
    private lateinit var mode: ImageView
    private lateinit var play: ImageView
    private lateinit var next: ImageView
    private lateinit var previous: ImageView
    private lateinit var adapter: MediaPlayerAdapter
    private var type = 0 //0 顺序 1 随机 2 单曲
    private var lastIndex = 0 //0 顺序 1 随机 2 单曲
    private lateinit var audioList: List<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this
        CoroutineScope(Dispatchers.IO).launch {
            val sn = getSN(context)
            val httpClient = HttpClient()
            val url =
                    "http://10.100.203.20:9000/swiper/public/config.json"
            val response = httpClient.makeRequest(url)
            val jsonObject = Gson().fromJson(
                response,
                JsonObject::class.java
            )

            val branch =
                    jsonObject.getAsJsonObject("device")
                        .get(sn)
            if (branch != null) {
                val playlistArray =
                        jsonObject.getAsJsonObject("playlist")
                            .getAsJsonArray(branch.asString)
                val audioFileNameList =
                        mutableListOf<String>()
                val list = mutableListOf<Uri>()
                for (resourceId in playlistArray) {
                    audioFileNameList.add(
                        resourceId.asString.split(
                            "_"
                        )[1]
                    )
                    val uriString =
                            "http://10.100.203.20:9000/swiper/public/" + resourceId.asString
                    list.add(Uri.parse(uriString))
                }

                audioList = list.toList()
                launch(Dispatchers.Main) {
                    adapter = MediaPlayerAdapter(
                        audioFileNameList,
                        index
                    )
                    listView =
                            findViewById<View>(R.id.recyclerView) as RecyclerView
                    listView!!.layoutManager =
                            LinearLayoutManager(context)
                    listView!!.adapter = adapter

                    listView!!.addOnItemTouchListener(
                        RecyclerView.SimpleOnItemTouchListener()
                    )

                    listView!!.addOnItemTouchListener(
                        RecyclerItemClickListener(
                            context,
                            listView!!,
                            object :
                                RecyclerItemClickListener.OnItemClickListener {
                                override fun onItemClick(
                                    view: View,
                                    position: Int
                                ) {
                                    if (index != -1) {
                                        val previousSelectedItem =
                                                listView!!.getChildAt(
                                                    index
                                                )
                                        previousSelectedItem?.setBackgroundColor(
                                            resources.getColor(
                                                R.color.backGround
                                            )
                                        )
                                    }
                                    view.setBackgroundColor(
                                        resources.getColor(
                                            R.color.gray
                                        )
                                    )
                                    index = position

                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                    mediaPlayer =
                                            MediaPlayer.create(
                                                context,
                                                audioList[position]
                                            )
                                    // 监听音频播放完成事件
                                    mediaPlayer?.setOnCompletionListener {
                                        playNextAudio()
                                    }

                                    // 开始播放下一个音频
                                    mediaPlayer?.start()
                                }

                                override fun onLongItemClick(
                                    view: View,
                                    position: Int
                                ) {
                                    // 处理 item 长按事件
                                }
                            })
                    )

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
            launch(Dispatchers.Main) {
                snView = findViewById(R.id.sn)
                snView.text = "设备SN: $sn"
            }
        }


        mode = findViewById(R.id.play_mode)
        play = findViewById(R.id.play)
        next = findViewById(R.id.next)
        previous = findViewById(R.id.previous)

        play.setOnClickListener(this)
        mode.setOnClickListener(this)
        next.setOnClickListener(this)
        previous.setOnClickListener(this)
        */
/*        val minioClient: MinioClient =
        MinioClient.builder()
            .endpoint("10.100.203.20")
            .credentials(
                "root",
                "Npr}xbSru7?X<er"
            )
            .build()*//*

        val serviceIntent = Intent(
            context,
            MusicService::class.java
        )
        context.startService(serviceIntent)
    }

    private fun playNextAudio() {
        // 停止并释放当前的MediaPlayer
        mediaPlayer?.release()
        mediaPlayer = null
        lastIndex = index
        when (type) {
            0 -> index = (index + 1) % audioList.size
            1 -> index =
                    Random(System.currentTimeMillis()).nextInt(
                        audioList.size
                    )
        }
        Log.e("123", index.toString())
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
        play.setImageResource(R.drawable.ic_baseline_pause_24)
        for (i in 0 until listView!!.childCount) {
            val previousSelectedItem =
                    listView!!.getChildAt(
                        i
                    )
            if (i == index) {
                previousSelectedItem?.setBackgroundColor(
                    resources.getColor(
                        R.color.gray
                    )
                )
            } else {
                previousSelectedItem?.setBackgroundColor(
                    resources.getColor(
                        R.color.backGround
                    )
                )
            }
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.play_mode -> {
                when (type) {
                    0 -> {
                        type = 1
                        mode.setImageResource(R.mipmap.looping_play)
                    }
                    1 -> {
                        type = 2
                        mode.setImageResource(R.mipmap.single_loop)
                    }
                    2 -> {
                        type = 0
                        mode.setImageResource(R.mipmap.list_play)
                    }
                }
            }
            R.id.play -> {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.pause()
                    play.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                } else {
                    mediaPlayer!!.start()
                    play.setImageResource(R.drawable.ic_baseline_pause_24)
                }
            }
            R.id.previous -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val credentials =
                                BasicAWSCredentials(
                                    "root",
                                    "Npr}xbSru7?X<er"
                                )
                        val s3Client: AmazonS3 =
                                AmazonS3Client(
                                    credentials,
                                    Region.getRegion(
                                        Regions.US_WEST_1
                                    ),
                                    ClientConfiguration()
                                )

                        s3Client.setEndpoint("http://10.100.203.20:9000")
                        //                      s3Client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build())

// 设置错误提示中要求的 region
                        s3Client.setRegion(
                            Region.getRegion(
                                Regions.US_WEST_1
                            )
                        )

                        val bucketName = "swiper"
                        val directoryPrefix =
                                "public"
                        val listObjectsRequest =
                                ListObjectsV2Request()
                                    .withBucketName(
                                        bucketName
                                    )
                                    .withPrefix(
                                        directoryPrefix
                                    )

                        val result =
                                s3Client.listObjectsV2(
                                    listObjectsRequest
                                )
                        val objectSummaries =
                                result.objectSummaries

                        for (objectSummary in objectSummaries) {
                            // objectSummary.getKey() 包含文件的完整路径，可以提取文件名或其他信息
                            val objectName =
                                    objectSummary.key
                            // 执行你的操作，如显示文件名等
                            Log.e("123", objectName)
                        }
                    } catch (e: Exception) {
                        Log.e("123", e.toString())
                    }
                }
            }
            R.id.next -> {
                playNextAudio()
            }
        }
    }

    @SuppressLint("HardwareIds")
    fun getSN(context: Context): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        } else Build.SERIAL
    }

}
*/
