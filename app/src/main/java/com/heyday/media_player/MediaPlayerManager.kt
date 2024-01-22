package com.heyday.media_player

import android.media.MediaPlayer

/**
 *@Author：chenliee
 *@Date：2023/10/27 13:31
 *Describe:
 */
object MediaPlayerManager {
    private var mediaPlayer: MediaPlayer? = null

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
}