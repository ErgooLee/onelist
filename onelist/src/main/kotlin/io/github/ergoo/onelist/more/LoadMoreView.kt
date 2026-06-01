package io.github.ergoo.onelist.more

import android.view.View

/**
 * Contract for the visual representation of a bottom load-more indicator.
 *
 * Implementations provide the actual [View], define dimension/animation parameters,
 * and handle callbacks for each phase of the load-more lifecycle:
 * scrolling (drag), loading, error, and final-page.
 *
 * A default implementation is available in [OneListBottomLoadMore].
 *
 * @see BottomLoadMoreAdapter
 * @see OneListBottomLoadMore
 */
interface LoadMoreView {

    /**
     * Minimum height (in pixels) of the load-more view when collapsed / idle.
     *
     * Typically set to `1` so the view occupies negligible space but remains
     * in the layout for attach/detach tracking.
     */
    val minHeight: Int

    /**
     * Duration (in milliseconds) to display the error message before
     * automatically reverting to [LoadState.None].
     */
    val loadErrorTipsShowDuration: Int

    /**
     * Maximum duration (in milliseconds) of the release animation when the
     * user lifts their finger after exceeding the load threshold.
     *
     * The view animates from its current drag height to [height].
     */
    val scrollingMaxReleaseAnimDuration: Int

    /**
     * Duration (in milliseconds) of the collapse animation when the view
     * transitions from its normal [height] back to [minHeight].
     */
    val normalHeightToGoneAnimDuration: Int

    /**
     * Normal display height (in pixels) of the load-more view when fully
     * expanded (e.g. during loading or showing an error/final-page message).
     *
     * Also serves as the drag threshold — the user must drag beyond this
     * height to trigger a load.
     */
    val height: Int

    /**
     * Maximum height (in pixels) the view can reach during a drag gesture.
     *
     * Prevents the user from dragging the view beyond a reasonable limit.
     * Typically set to `height * 1.5`.
     */
    val scrollMaxHeight: Int

    /**
     * The actual Android [View] displayed as the load-more indicator.
     *
     * Must have a valid view ID ([View.getId] != [View.NO_ID]) because it
     * is used as the adapter's view type.
     */
    val view: View

    /**
     * Called continuously while the user is dragging upward.
     *
     * @param arriveThreshold `true` when the drag height has reached or exceeded the
     *                        load threshold ([height]). Implementations can use this
     *                        to change the visual hint (e.g. "release to load").
     * @param progress drag progress in the range `[0, 1]`, where `1.0`
     *                 means the drag has reached [scrollMaxHeight] (the maximum drag limit).
     */
    fun onScrolling(arriveThreshold: Boolean, progress: Float)

    /**
     * Called when the user releases their finger without reaching the
     * load threshold. The view will animate back to [minHeight].
     */
    fun onScrollCanceled()

    /**
     * Called when the user releases their finger after exceeding the load threshold.
     *
     * The footer will animate to [height] and then transition to [LoadState.Loading].
     * Implementations typically show a loading indicator at this point
     * (the default [OneListBottomLoadMore] delegates to [onLoadStart]).
     */
    fun onReadyToLoad()

    /**
     * Called when a load operation begins ([LoadState.Loading]).
     *
     * Typically shows a progress indicator.
     */
    fun onLoadStart()

    /**
     * Called when the state returns to [LoadState.None] (idle).
     *
     * Typically resets the view to its default appearance.
     */
    fun onLoadNone()

    /**
     * Called when the load operation fails ([LoadState.Error]).
     *
     * @param error the optional [Throwable] that caused the failure;
     *              may be `null` if no specific error is available.
     */
    fun onLoadError(error: Throwable?)

    /**
     * Called when the last page has been loaded successfully ([LoadState.Final]).
     *
     * Typically displays an "end of list" message.
     */
    fun onFinalPageSucceed()

}