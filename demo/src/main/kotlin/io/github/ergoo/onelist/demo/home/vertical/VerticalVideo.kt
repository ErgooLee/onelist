package io.github.ergoo.onelist.demo.home.vertical

import io.github.ergoo.onelist.demo.home.HomeChannelItem

data class VerticalVideo(
    val id: Long,
    val title: String,
    val subTitle: String,
    val floorIndex: Int,
    val liked: Boolean,
) : HomeChannelItem