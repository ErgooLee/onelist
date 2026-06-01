package io.github.ergoo.onelist.demo.home

import io.github.ergoo.onelist.DifferMergeAdapter
import io.github.ergoo.onelist.MainContentAdapter
import io.github.ergoo.onelist.demo.home.floor.FloorTitleListBinder
import io.github.ergoo.onelist.demo.home.horizontal.HorizontalVideoListBinder
import io.github.ergoo.onelist.demo.home.vertical.VerticalVideoListBinder

class HomeContentAdapter : DifferMergeAdapter(), MainContentAdapter {

    val titleBinder = FloorTitleListBinder()

    val verticalVideoBinder = VerticalVideoListBinder()

    val horizontalVideoListBinder = HorizontalVideoListBinder()

    init {

        addListBinder(
            titleBinder,
            FloorTitleListBinder.DIFF_CALLBACK
        )

        addListBinder(
            verticalVideoBinder,
            VerticalVideoListBinder.DIFF_CALLBACK,
        )

        addListBinder(
            horizontalVideoListBinder,
            HorizontalVideoListBinder.DIFF_CALLBACK
        )

    }
}

