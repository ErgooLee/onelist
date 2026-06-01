package io.github.ergoo.onelist.demo.home

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpaceItemDecoration() : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        outRect.left = 10
        outRect.left = 10
        outRect.top = 20
        outRect.bottom = 20
    }
}

