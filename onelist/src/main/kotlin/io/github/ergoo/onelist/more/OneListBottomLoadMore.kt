package io.github.ergoo.onelist.more

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import io.github.ergoo.onelist.R

/**
 * Default implementation of [LoadMoreView] that provides a simple text + progress-bar
 * load-more indicator.
 *
 * Uses the layout `R.layout.one_list_bottom_load_more` which contains a [TextView]
 * for status messages and a progress bar for the loading state.
 *
 * Subclass this to customize strings, dimensions, or view behavior while
 * retaining the default layout structure.
 *
 * @param context used to inflate the layout and resolve dimension/string resources.
 *
 * @see LoadMoreView
 * @see BottomLoadMoreAdapter
 */
open class OneListBottomLoadMore(context: Context) : LoadMoreView {

    /** @see LoadMoreView.minHeight */
    override var minHeight: Int = 1

    /** Error tips are shown for 600ms before auto-dismissing. */
    override var loadErrorTipsShowDuration: Int = 600

    /** Release animation plays for up to 300ms. */
    override var scrollingMaxReleaseAnimDuration: Int = 300

    /** Collapse animation plays for up to 300ms. */
    override var normalHeightToGoneAnimDuration: Int = 300

    /** Normal expanded height, read from `R.dimen.one_list_load_more_height`. */
    override var height: Int = context.resources.getDimension(R.dimen.one_list_load_more_height).toInt()

    /** Maximum drag height = 1.5× of [height]. */
    override var scrollMaxHeight: Int = (height * 1.5f).toInt()

    override val view: View by lazy {
        LayoutInflater.from(context).inflate(R.layout.one_list_default_bottom_load_more, null)
    }

    /** The [TextView] used to display status messages (scrolling tips, error, final page). */
    private val loadTipsView: TextView by lazy {
        view.findViewById(R.id.tips_tv)
    }

    /** The progress bar shown during the [LoadState.Loading] state. */
    private val progressBar: View by lazy {
        view.findViewById(R.id.progress_bar)
    }

    /** Text displayed when all pages have been loaded. */
    private val finalTips: String = context.getString(R.string.one_list_more_final_page_tips)

    /** Text displayed while the user is dragging or in idle state. */
    private val scrollingTips: String = context.getString(R.string.one_list_more_scrolling_tips)

    /** Text displayed when a load operation fails. */
    private val loadErrorTips: String = context.getString(R.string.one_list_more_load_failed_tips)

    override fun onScrolling(arriveThreshold: Boolean, progress: Float) {
        loadTipsView.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        loadTipsView.text = scrollingTips
    }

    override fun onScrollCanceled() {
        loadTipsView.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        loadTipsView.text = scrollingTips
    }

    /** Delegates to [onLoadStart] to immediately show the loading indicator. */
    override fun onReadyToLoad() {
        onLoadStart()
    }

    override fun onLoadStart() {
        loadTipsView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
    }

    override fun onLoadNone() {
        loadTipsView.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        loadTipsView.text = scrollingTips
    }

    override fun onLoadError(error: Throwable?) {
        loadTipsView.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        loadTipsView.text = loadErrorTips
    }

    override fun onFinalPageSucceed() {
        loadTipsView.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        loadTipsView.text = finalTips
    }


}