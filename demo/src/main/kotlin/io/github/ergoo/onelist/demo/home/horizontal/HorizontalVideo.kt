package io.github.ergoo.onelist.demo.home.horizontal

import io.github.ergoo.onelist.demo.home.HomeChannelItem

data class HorizontalVideo(
    val id: Long,
    val title: String,
    val subTitle: String,
    val floorIndex: Int,
    val liked: Boolean,
) : HomeChannelItem