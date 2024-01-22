package com.heyday.media_player.demo

/**
 *@Author：chenliee
 *@Date：2023/11/2 10:35
 *Describe:
 */
data class Model2(
    val `data`: Data?,
    val uuid: String?
) {
    data class Data(
        val action: String?,
        val params: Params?
    ) {
        data class Params(
            val code: String?
        )
    }
}