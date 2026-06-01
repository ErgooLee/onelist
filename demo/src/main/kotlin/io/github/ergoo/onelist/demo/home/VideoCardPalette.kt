package io.github.ergoo.onelist.demo.home

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import io.github.ergoo.onelist.demo.R

object VideoCardPalette {

    private val colorResIds = intArrayOf(
        R.color.video_card_bg_1,
        R.color.video_card_bg_2,
        R.color.video_card_bg_3,
        R.color.video_card_bg_4,
        R.color.video_card_bg_5,
        R.color.video_card_bg_6,
        R.color.video_card_bg_7,
        R.color.video_card_bg_8,
    )

    @ColorRes
    fun colorResFor(id: Long): Int {
        val index = Math.floorMod(id.toInt(), colorResIds.size)
        return colorResIds[index]
    }

    @ColorInt
    fun colorFor(context: Context, id: Long): Int {
        return ContextCompat.getColor(context, colorResFor(id))
    }
}

