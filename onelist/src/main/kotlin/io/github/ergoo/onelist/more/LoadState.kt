package io.github.ergoo.onelist.more

/**
 * Represents the current state of a load-more pagination operation.
 *
 * Used by [LoadMoreAdapter] and its subclasses to drive the visual
 * representation of footer/header loading indicators.
 *
 * ## State machine (BottomLoadMoreAdapter)
 *
 * ```
 *  None ──(drag begins)──► Scrolling ──(finger up, threshold met)──► ReadyToLoad ──► Loading
 *    ▲                        │  ▲          │                                           │
 *    │                        │  └──(move)──┘                                           │
 *    │                        │                                                         │
 *    │                        └──(finger up, threshold not met)──► ScrollingCancel ──────┘
 *    │                                                                                  │
 *    ├──────────────────────────────────────────────────────────────────(collapse done)───┘
 *    │
 *    ├──(preload trigger)──► Loading
 *    │
 *    └──(error auto-dismiss)── Error
 *
 *  Loading ──(success, more pages)──► None
 *  Loading ──(success, last page)──► Final
 *  Loading ──(failure)────────────► Error ──(auto-dismiss)──► None
 * ```
 */
sealed interface LoadState {

    /**
     * Idle state — no loading is in progress and more data may be available.
     *
     * The load-more view is typically collapsed (at [LoadMoreView.minHeight]) in this state,
     * waiting for a user drag gesture or preload trigger to start loading.
     */
    object None : LoadState

    /**
     * The user is actively dragging upward on the footer.
     *
     * This is a transient, internal state used by [BottomLoadMoreAdapter] to track
     * the drag gesture. It is **not** intended to be set by external callers.
     *
     * Valid transitions: from [None] or another [Scrolling] instance.
     *
     * @property arriveThreshold `true` when the current drag height has reached or
     *                           exceeded the load threshold ([LoadMoreView.height]).
     * @property progress drag progress in the range `[0, 1]`, where `1.0` means the
     *                    drag has reached the maximum drag height ([LoadMoreView.scrollMaxHeight]).
     */
    data class Scrolling(val arriveThreshold: Boolean, val progress: Float) : LoadState

    /**
     * The user released their finger after exceeding the load threshold.
     *
     * A transient, internal state used by [BottomLoadMoreAdapter]. The footer
     * animates to [LoadMoreView.height] and then automatically transitions to [Loading].
     *
     * Valid transition: from [Scrolling] only.
     */
    object ReadyToLoad : LoadState

    /**
     * The user released their finger **without** reaching the load threshold,
     * or the drag was interrupted (e.g. [BottomLoadMoreAdapter.allowTriggerLoadMore]
     * returned `false` mid-drag).
     *
     * A transient, internal state used by [BottomLoadMoreAdapter]. The footer
     * animates back to [LoadMoreView.minHeight] and then transitions to [None].
     *
     * Valid transition: from [Scrolling] only.
     */
    object ScrollingCancel : LoadState

    /**
     * A load operation is currently in progress.
     *
     * The load-more view should display a loading indicator (e.g. a progress bar).
     * The footer is expanded to [LoadMoreView.height].
     *
     * Valid transitions: from [ReadyToLoad] (gesture-driven) or [None] (preload-driven).
     */
    object Loading : LoadState

    /**
     * The most recent load operation failed.
     *
     * The load-more view displays an error message for
     * [LoadMoreView.loadErrorTipsShowDuration] milliseconds, then automatically
     * collapses and reverts to [None].
     *
     * Only takes effect when the footer is currently visible (attached);
     * otherwise the state immediately falls back to [None].
     *
     * @property error the optional [Throwable] that caused the failure.
     */
    class Error(
        val error: Throwable? = null
    ) : LoadState

    /**
     * All pages have been loaded — no more data is available.
     *
     * The load-more view displays a "no more data" indicator at
     * [LoadMoreView.height] and no further load attempts will be triggered.
     */
    object Final : LoadState
}
