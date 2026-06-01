package io.github.ergoo.onelist.refresh

/**
 * Represents the current state of a pull-to-refresh operation.
 *
 * Used by [RefreshAdapter] to drive the visual representation of the
 * refresh header and enforce valid state transitions via [RefreshAdapter.onStateChange].
 *
 * ## State machine
 *
 * ```
 * None ──(drag begins)──► Scrolling ──(finger up, threshold met)──► ReadyToRefresh ──► Refreshing
 *  ▲                         │  ▲          │                                              │
 *  │                         │  └──(move)──┘                                              │
 *  │                         │                                                            │
 *  │                         └──(finger up, threshold not met)──► ScrollingCancel ─────────┤
 *  │                                                                                      │
 *  ├──────────────────────────────────────────────────────────(collapse done)──────────────┘
 *  │
 *  ├──(load success, after delay)── RefreshSucceed
 *  └──(load failure, after delay)── RefreshFailed
 * ```
 */
sealed interface RefreshState {

    /**
     * Idle state — no refresh is in progress.
     *
     * The refresh header is collapsed to [RefreshView.minHeight].
     */
    object None : RefreshState

    /**
     * The user is actively dragging downward on the refresh header.
     *
     * A transient, internal state managed by [RefreshAdapter]'s touch handling.
     *
     * Valid transitions: from [None] or another [Scrolling] instance.
     *
     * @property arriveThreshold `true` when the drag height has reached or
     *                           exceeded the refresh threshold ([RefreshView.height]).
     * @property progress drag progress in `[0, 1]`, where `1.0` means the drag
     *                    has reached [RefreshView.scrollMaxHeight].
     */
    data class Scrolling(
        val arriveThreshold: Boolean,
        val progress: Float,
    ) : RefreshState

    /**
     * The user released their finger after exceeding the refresh threshold.
     *
     * The header animates to [RefreshView.height] and then automatically
     * transitions to [Refreshing].
     *
     * Valid transition: from [Scrolling] only.
     */
    object ReadyToRefresh : RefreshState

    /**
     * The user released their finger **without** reaching the refresh threshold,
     * or the drag was interrupted.
     *
     * The header animates back to [RefreshView.minHeight] and then transitions to [None].
     *
     * Valid transition: from [Scrolling] only.
     */
    object ScrollingCancel : RefreshState

    /**
     * A refresh operation is currently in progress.
     *
     * The header is expanded to [RefreshView.height] and displays a loading indicator.
     *
     * Valid transition: from [ReadyToRefresh] only.
     */
    object Refreshing : RefreshState

    /**
     * The refresh operation completed successfully.
     *
     * The header briefly shows a success message for [RefreshView.refreshResultShowTime]
     * milliseconds, then auto-transitions to [None].
     *
     * Valid transition: from [Refreshing] only.
     */
    object RefreshSucceed : RefreshState

    /**
     * The refresh operation failed.
     *
     * The header briefly shows a failure message for [RefreshView.refreshResultShowTime]
     * milliseconds, then auto-transitions to [None].
     *
     * Valid transition: from [Refreshing] only.
     */
    object RefreshFailed : RefreshState
}