package com.heyday.media_player.demo

import android.util.Log
import com.chenliee.library.http.BaseResponse
import com.chenliee.library.http.CommonObserve
import com.chenliee.library.http.RetrofitManager
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import retrofit2.http.*

/**
 *@Author：chenliee
 *@Date：2024/1/22 11:27
 *Describe:
 */
class MediaObserve: CommonObserve() {
    private val musicService: MusicService?
    init {
        musicService =
                RetrofitManager.getInstance()
                    .create(
                        MusicService::class.java
                    )
    }
    
    
    fun getMusic(brand: String, onDataReceived: (MediaModel) -> Unit): Disposable {
        return observe(
            musicService!!.getMusic(
                project = "heyday",
                queryParams = mapOf(
                    "path" to "public/$brand",
                    "project" to "swiper",
                    "ext" to ".mp3",
                )
            ),
            onDataReceived // 将传入的方法作为 observe 函数的参数
        )
    }

    interface MusicService {
        @GET("storage/api/merchant/{project}/file/")
        fun getMusic(@Path("project") project:String,
                     @QueryMap queryParams: Map<String, String>
        ): Observable<BaseResponse<MediaModel>>
    }
}