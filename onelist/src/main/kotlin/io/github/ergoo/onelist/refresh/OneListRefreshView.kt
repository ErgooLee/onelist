package io.github.ergoo.onelist.refresh

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import io.github.ergoo.onelist.R

open class OneListRefreshView(val context: Context) : RefreshView {

    override val view: View by lazy {
        LayoutInflater.from(context)
            .inflate(R.layout.one_list_default_refresh_view, null, false)
    }

    override var minHeight: Int = 1

    override var height: Int =
        context.resources.getDimension(R.dimen.one_list_refresh_height).toInt()

    /** Maximum drag height = 1.5× of [height]. */
    override var scrollMaxHeight: Int = (height * 1.5f).toInt()

    /** Release animation plays for up to 300ms. */
    override var maxHeightToNormalDuration: Int = 300

    /** Collapse animation plays for up to 300ms. */
    override var normalHeightToMinHeightDuration: Int = 300

    /** Refresh result tips are shown for 600ms before auto-dismissing. */
    override var refreshResultShowTime: Int = 600

    /** Whether fast-scroll hint animation is enabled. */
    override var showFastScrollHint = true

    /** Visible height used for the fast-scroll hint reveal. */
    override var fastScrollHintHeight: Int = (height * 0.8f).toInt().coerceAtLeast(minHeight)

    /** Minimum upward distance required before the top-arrival hint can be shown. */
    override var fastScrollHintMinDistance: Int = scrollMaxHeight

    /** Expand duration for the fast-scroll hint animation, in milliseconds. */
    override var fastScrollHintExpandDuration: Int = 240

    /** Collapse duration for the fast-scroll hint animation, in milliseconds. */
    override var fastScrollHintCollapseDuration: Int = 360

    /** The [TextView] used to display status messages (scrolling tips, result). */
    private val statusTextView: TextView by lazy {
        view.findViewById(R.id.status_tv)
    }

    /** The progress bar shown during the [RefreshState.Refreshing] state. */
    private val progressBar: View by lazy {
        view.findViewById(R.id.progress_bar)
    }

    override fun onScrolling(arriveThreshold: Boolean, progress: Float) {
        progressBar.visibility = View.INVISIBLE
        statusTextView.visibility = View.VISIBLE

        statusTextView.text = if (arriveThreshold) {
            context.getString(R.string.one_list_refresh_trigger_tips)
        } else {
            context.getString(R.string.one_list_refresh_scrolling_tips)
        }
    }

    override fun onReadyToRefresh() {
        onTriggerRefresh()
    }

    override fun onScrollingCancel() {
        onNone()
    }

    override fun onTriggerRefresh() {
        statusTextView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
    }

    override fun onRefreshSucceed() {
        progressBar.visibility = View.INVISIBLE
        statusTextView.visibility = View.VISIBLE
        statusTextView.text = context.getString(R.string.one_list_refresh_succeed)
    }

    override fun onRefreshFailed() {
        progressBar.visibility = View.INVISIBLE
        statusTextView.visibility = View.VISIBLE
        statusTextView.text = context.getString(R.string.one_list_refresh_failed)
    }

    override fun onNone() {
        statusTextView.visibility = View.VISIBLE
        statusTextView.text = context.getString(R.string.one_list_refresh_scrolling_tips)
    }
}