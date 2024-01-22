package com.heyday.media_player.demo

/**
 *@Author：chenliee
 *@Date：2024/1/22 11:32
 *Describe:
 */
data class MediaModel(
    val list: List<Music>?,
    val page: Int?,
    val size: Int?,
    val total: Int?
) {
    data class Music(
        val hash: String?,
        val id: Int?,
        val mime: String?,
        val name: String?,
        val size: String?,
        val url: String?
    )

    override fun toString(): String {
        return "MediaModel(list=$list, page=$page, size=$size, total=$total)"
    }
}