package com.heyday.media_player.demo

import android.util.Log

/**
 *@Author：chenliee
 *@Date：2024/8/26 09:24
 *Describe:
 */
class MediaFun {
    private val mediaObserve: MediaObserve = MediaObserve()
    
    private fun getMusicList() {
        mediaObserve.getMusic(brand =  "sad"){ data ->
            Log.e("123", data.toString())
            // 处理数据
        }
    }
}