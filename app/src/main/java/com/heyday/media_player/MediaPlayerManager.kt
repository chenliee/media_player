package com.heyday.media_player

import android.content.Context
import android.media.MediaPlayer
import com.heyday.pos.mylibrary.service_package.ServiceGlobal
import com.heyday.pos.mylibrary.storage.http.File
import com.heyday.pos.mylibrary.storage.model.FileModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 *@Author：chenliee
 *@Date：2023/10/27 13:31
 *Describe:
 */
object MediaPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var audioList: List<FileModel>? = null
    var selectIndex: Int = 0

    fun getMediaPlayer(): MediaPlayer {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        }
        return mediaPlayer!!
    }

    fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    suspend fun getAudioList(context: Context) : List<FileModel> {

            if(audioList == null) {
                val deferred = File().getFile(
                    context = context,
                    path = "meida-branch/${ServiceGlobal.brand}",
                    project = "swiper"
                )
                if(deferred != null) {
                    audioList =
                            deferred
                }
            }

        return audioList!!
    }

}