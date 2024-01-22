package com.heyday.media_player.demo

import android.util.Log
import com.chenliee.library.http.CommonObserve
import com.chenliee.library.http.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import retrofit2.http.*

/**
 *@Author：chenliee
 *@Date：2023/10/30 17:18
 *Describe:
 */
class DemoObserve: CommonObserve() {
    private val musicService: MusicService?
    init {
        musicService =
                RetrofitManager.getInstance()
                    .create(
                        MusicService::class.java
                    )
    }


    fun getMovie(): Disposable {
        return observe(
            musicService!!.getMusic(project = "heyday", queryParams = mapOf(
                "height" to "40",
                "width" to "120"
            ))
        ) { data ->
            Log.e("123", data.src!!)
        }
    }

    fun register(): Disposable {
        return observe(
            musicService!!.register(project = "heyday", queryParams = mapOf(
                "package" to "com.heyday.pos",
                "token" to "363237500338343333462F72"
            ))
        ) { data ->
            Log.e("123", data.uuid!!)
        }
    }

    interface MusicService {
        @GET("iam/api/merchant/{project}/captcha")
        fun getMusic(@Path("project") project:String,
            @QueryMap queryParams: Map<String, String>
        ): Observable<BaseResponse<DemoModel>>


        @POST("notify/api/merchant/{project}/channel/pos/device-registration")
        fun register(@Path("project") project:String,
                     @Body queryParams: Map<String, String>
        ): Observable<BaseResponse<Model2>>
    }
}