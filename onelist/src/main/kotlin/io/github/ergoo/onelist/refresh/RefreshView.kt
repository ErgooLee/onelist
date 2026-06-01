package io.github.ergoo.onelist.refresh

import android.view.View

/**
 * Contract for the visual representation of a pull-to-refresh header.
 *
 * Implementations provide the actual [View], define dimension/animation parameters,
 * and handle callbacks for each phase of the refresh lifecycle:
 * scrolling (drag), refreshing, success, and failure.
 *
 * A default implementation is available in [OneListRefreshView].
 *
 * @see RefreshAdapter
 * @see OneListRefreshView
 */
interface RefreshView {

    /**
     * The actual Android [View] displayed as the refresh header.
     *
     * Must have a valid view ID ([View.getId] != [View.NO_ID]) because it
     * is used as the adapter's view type.
     */
    val view: View

    /**
     * Minimum height (in pixels) of the refresh header when collapsed / idle.
     *
     * Typically set to `1` so the view occupies negligible space but remains
     * in the layout for attach/detach tracking.
     */
    val minHeight: Int

    /**
     * Normal display height (in pixels) of the refresh header when fully
     * expanded (e.g. during refreshing or showing result messages).
     *
     * Also serves as the drag threshold — the user must drag beyond this
     * height to trigger a refresh.
     */
    val height: Int

    /**
     * Maximum height (in pixels) the header can reach during a drag gesture.
     *
     * Prevents the user from dragging the header beyond a reasonable limit.
     * Typically set to `height * 1.5`.
     */
    val scrollMaxHeight: Int

    /**
     * Duration (in milliseconds) of the release animation when the user lifts
     * their finger after exceeding the refresh threshold.
     *
     * The header animates from its current drag height to [height].
     */
    val maxHeightToNormalDuration: Int

    /**
     * Duration (in milliseconds) of the collapse animation when the header
     * transitions from its normal [height] back to [minHeight].
     */
    val normalHeightToMinHeightDuration: Int

    /**
     * Duration (in milliseconds) to display the refresh result message
     * (success or failure) before automatically reverting to [RefreshState.None].
     */
    val refreshResultShowTime: Int


    /** Whether fast-scroll hint animation is enabled. */
    val showFastScrollHint: Boolean

    /** Visible height used for the fast-scroll hint reveal. */
    val fastScrollHintHeight: Int

    /** Minimum upward distance required before the top-arrival hint can be shown. */
    val fastScrollHintMinDistance: Int

    /** Expand duration for the fast-scroll hint animation, in milliseconds. */
    val fastScrollHintExpandDuration: Int

    /** Collapse duration for the fast-scroll hint animation, in milliseconds. */
    val fastScrollHintCollapseDuration: Int

    /**
     * Called when the state returns to [RefreshState.None] (idle).
     *
     * Typically resets the header to its default appearance.
     */
    fun onNone()

    /**
     * Called continuously while the user is dragging downward.
     *
     * @param arriveThreshold `true` when the drag height has reached or exceeded the
     *                        refresh threshold ([height]). Implementations can use this
     *                        to change the visual hint (e.g. "release to refresh").
     * @param progress drag progress in the range `[0, 1]`, where `1.0`
     *                 means the drag has reached [scrollMaxHeight].
     */
    fun onScrolling(arriveThreshold: Boolean, progress: Float)

    /**
     * Called when the user releases their finger after exceeding the refresh threshold.
     *
     * The header will animate to [height] and then transition to [RefreshState.Refreshing].
     * Implementations typically show a loading indicator at this point.
     */
    fun onReadyToRefresh()

    /**
     * Called when the user releases their finger without reaching the
     * refresh threshold. The header will animate back to [minHeight].
     */
    fun onScrollingCancel()

    /**
     * Called when a refresh operation begins ([RefreshState.Refreshing]).
     *
     * Typically shows a progress indicator.
     */
    fun onTriggerRefresh()

    /**
     * Called when the refresh operation completes successfully ([RefreshState.RefreshSucceed]).
     *
     * Typically displays a success message briefly before auto-dismissing.
     */
    fun onRefreshSucceed()

    /**
     * Called when the refresh operation fails ([RefreshState.RefreshFailed]).
     *
     * Typically displays an error message briefly before auto-dismissing.
     */
    fun onRefreshFailed()


}