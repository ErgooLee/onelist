package io.github.ergoo.onelist.refresh

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.MainThread
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import io.github.ergoo.onelist.FullSpan
import io.github.ergoo.onelist.Log
import io.github.ergoo.onelist.OneItemAdapter

private const val TAG = "RefreshAdapter"

/**
 * A gesture-driven pull-to-refresh adapter that places a [RefreshView] header
 * at the top of a [RecyclerView].
 *
 * This adapter always shows exactly one item (the refresh header). When the user
 * is at the top of the list and drags downward past a threshold, a refresh
 * operation is triggered.
 * While attached, it suppresses RecyclerView's native overscroll glow/stretch effect
 * to avoid visual conflict with the custom pull-to-refresh interaction.
 *
 * ## How it works
 *
 * 1. The adapter registers an [OnItemTouchListener] on the [RecyclerView] when attached.
 * 2. On `ACTION_MOVE`, if the list is at the top ([atTop]) and the finger moves
 *    downward past the touch slop, the adapter intercepts the touch and enters
 *    [RefreshState.Scrolling].
 * 3. As the user drags downward, the header view's height grows with
 *    non-linear damping, so the gesture feels light at first and gradually
 *    becomes heavier as it approaches [RefreshView.scrollMaxHeight]. Visual
 *    feedback is provided via [RefreshView.onScrolling].
 * 4. If the drag exceeds [RefreshView.height] (the threshold), releasing the finger
 *    transitions to [RefreshState.ReadyToRefresh], animates to the normal height,
 *    and automatically transitions to [RefreshState.Refreshing] which invokes
 *    [triggerRefresh].
 * 5. If the drag does not reach the threshold, the state transitions to
 *    [RefreshState.ScrollingCancel], the header collapses back with an animation,
 *    and [RefreshView.onScrollingCancel] is called.
 * 6. When the user fast-scrolls back to the top, the adapter can briefly reveal
 *    part of the refresh header and then collapse it again, making the pull-to-
 *    refresh affordance discoverable without triggering an actual refresh.
 *
 * ## State management
 *
 * All state transitions are centralized in [onStateChange], which enforces the
 * valid transition rules defined in [RefreshState]. Setting [refreshState] drives
 * both the state machine and the view updates.
 *
 * External callers typically only set:
 * - [RefreshState.RefreshSucceed] — after a successful refresh.
 * - [RefreshState.RefreshFailed] — after a failed refresh.
 *
 * The gesture-related states ([RefreshState.Scrolling], [RefreshState.ScrollingCancel],
 * [RefreshState.ReadyToRefresh]) and the [RefreshState.Refreshing] trigger are
 * managed internally.
 *
 * ## Usage
 *
 * ```kotlin
 * val refreshAdapter = RefreshAdapter(OneListRefreshView(context)).apply {
 *     triggerRefresh = { viewModel.refresh() }
 *     allowRefresh = { !viewModel.isLoadingMore }
 * }
 * recyclerView.adapter = ConcatAdapter(refreshAdapter, contentAdapter)
 * ```
 *
 * @param refreshView the [RefreshView] that provides the header's visual representation.
 *                    Its [RefreshView.view] must have a valid view ID.
 *
 * @see RefreshView
 * @see OneListRefreshView
 * @see RefreshState
 */
class RefreshAdapter(private val refreshView: RefreshView) :
    OneItemAdapter<RefreshState, RefreshAdapter.RefreshViewHolder>(RefreshState.None),
    OnItemTouchListener,
    FullSpan {

    init {
        require(refreshView.view.id != View.NO_ID) { "refresh view must have a valid id" }
    }

    /** Temp array for [View.getLocationInWindow] calls. */
    private val tempArray = IntArray(2)

    /** Handler for scheduling delayed tasks (e.g. refresh result auto-dismiss). */
    private val handler = Handler(Looper.getMainLooper())

    /** Root container view that wraps the [RefreshView.view] with controlled height. */
    private val root: View by lazy {
        createRootView()
    }

    /** Last recorded Y coordinate for delta calculation during drag. */
    private var lastY: Int = -1

    /**
     * Cumulative raw vertical drag distance during the current gesture.
     *
     * This stores the user's finger movement before damping is applied and is used
     * as the input to the non-linear drag mapping in [mapDragOffsetToHeight].
     */
    private var sumOffset: Int = 0

    /**
     * Controls how quickly drag resistance ramps up.
     *
     * Larger values make the header feel heavier earlier; smaller values keep it
     * more linear. `3f` provides a noticeable but still reachable pull distance.
     *
     * Values below `0` are coerced to `0`.
     */
    var dragDampingFactor = 3f
        set(value) {
            field = value.coerceAtLeast(0f)
        }

    /** Currently running height animation, or `null` if idle. */
    private var heightAnimator: Animator? = null

    /** Whether the current scroll session moved from away-from-top back to the top. */
    private var wasAwayFromTopDuringScroll = false

    /** Accumulated upward scroll distance during the current scroll session. */
    private var upwardScrollDistance = 0

    /** `true` when the current scroll session included fling/settling. */
    private var sawSettlingDuringScroll = false

    /** Listener used to detect fast-scroll-to-top sessions and show a refresh hint. */
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                showFastScrollHintIfNeeded()
                resetScrollTracking()
            } else if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                sawSettlingDuringScroll = true
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (!allowRefresh()) {
                resetScrollTracking()
                return
            }

            if (!atTop()) {
                wasAwayFromTopDuringScroll = true
            }

            if (dy < 0) {
                upwardScrollDistance += -dy
            }
        }
    }

    /** Whether the refresh header ViewHolder is currently attached (visible). */
    private var _isShow = false

    /**
     * The current refresh state. Setting this property drives the state machine
     * via [onStateChange] and updates the header's visual representation.
     *
     * The state machine may reject transitions (e.g. setting [RefreshState.Refreshing]
     * when the current state is not [RefreshState.ReadyToRefresh]). The actual
     * resulting state is stored in [status].
     *
     * Must be set on the main thread.
     */
    var refreshState: RefreshState
        get() = status
        @MainThread
        set(value) {
            status = value
        }

    /**
     * Backing field for [refreshState]. Overrides the parent `status` property to intercept
     * value changes through the state machine ([onStateChange]) and synchronize
     * the header view ([updateView]).
     */
    override var status: RefreshState = RefreshState.None
        set(value) {
            val oldValue = field
            if (oldValue != value) {
                field = onStateChange(oldValue, value)
                updateView()
            }
        }

    /**
     * Callback invoked when a refresh operation should be triggered.
     *
     * Called during the [RefreshState.Refreshing] transition, after [allowRefresh]
     * has returned `true`.
     */
    var triggerRefresh: (() -> Unit)? = null

    /**
     * Guard function that determines whether a refresh operation is allowed.
     *
     * Return `false` to suppress refresh triggers (e.g. during a load-more operation).
     * Defaults to `{ true }`.
     */
    var allowRefresh: () -> Boolean = { true }

    /** Whether the refresh header ViewHolder is currently attached (visible). */
    val isShow: Boolean
        get() = _isShow

    /** Whether a refresh operation is currently in progress. */
    val refreshing: Boolean
        get() = refreshState == RefreshState.Refreshing

    /** Initial finger-down X coordinate, used for touch slop calculation. */
    private var initDownX: Int = 0

    /** Initial finger-down Y coordinate, used for touch slop calculation. */
    private var initDownY: Int = 0

    /**
     * Intercepts touch events on the [RecyclerView] to detect pull-down gestures.
     *
     * On `ACTION_MOVE`, if the list is at the top and the finger has moved downward
     * past the touch slop, the adapter takes over (returns `true`) and enters
     * [RefreshState.Scrolling]. Subsequent touch events are handled by [onTouchEvent].
     *
     * Returns `true` only when beginning to intercept the drag gesture.
     */
    override fun onInterceptTouchEvent(rv: RecyclerView, event: MotionEvent): Boolean {

        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val mainIndex = event.findPointerIndex(0)

                if (mainIndex == -1) {
                    return false
                }

                if (!allowRefresh()) {
                    return false
                }

                if (refreshState != RefreshState.None) {
                    return false
                }

                if (initDownX == 0 && initDownY == 0) {
                    initDownX = event.getX(mainIndex).toInt()
                    initDownY = event.getY(mainIndex).toInt()

                    root.getLocationInWindow(tempArray)

                    val rootRawY = tempArray[1]
                    recyclerView.getLocationInWindow(tempArray)

                    val recyclerY = tempArray[1]
                    val eventBelowRootView = recyclerY + initDownY >= rootRawY

                    Log.d(
                        TAG,
                        "ACTION_DOWN eventX=$initDownX eventY=$initDownY adapterY=${rootRawY} recyclerY=${recyclerY}"
                    )

                    if (!eventBelowRootView) {
                        initDownX = 0
                        initDownY = 0
                        return false
                    }
                }

                val x = event.getX(mainIndex).toInt()
                val y = event.getY(mainIndex).toInt()

                Log.d(TAG, "ACTION_MOVE x=$x y=$y initY=$initDownY")

                val scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
                if (y >= initDownY + scaledTouchSlop && atTop()) {
                    rv.requestDisallowInterceptTouchEvent(true)
                    lastY = y
                    sumOffset = 0
                    refreshState = RefreshState.Scrolling(
                        arriveThreshold = false, progress = 0f,
                    )
                    return true
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                initDownX = 0
                initDownY = 0
            }
        }
        return false
    }

    /**
     * Handles touch events after the adapter has intercepted the drag gesture.
     *
     * - **ACTION_MOVE**: computes finger delta and delegates to [onFingerMove]
     *   to update the [RefreshState.Scrolling] progress.
     * - **ACTION_UP**: transitions to [RefreshState.ReadyToRefresh] if the threshold
     *   was reached, or [RefreshState.ScrollingCancel] otherwise.
     * - **Other actions**: cancels the drag via [RefreshState.ScrollingCancel].
     */
    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        Log.d(TAG, "on touch event ${e.action}")
        when (e.action) {
            MotionEvent.ACTION_MOVE -> {
                val mainIndex = e.findPointerIndex(0)
                if (mainIndex != -1 && allowRefresh()) {
                    val newY = e.getY(mainIndex).toInt()
                    val deltaY = newY - lastY
                    lastY = newY
                    sumOffset = (sumOffset + deltaY).coerceAtLeast(0)
                    onFingerMove(deltaY)
                } else {
                    refreshState = RefreshState.ScrollingCancel
                    initDownX = 0
                    initDownY = 0
                    sumOffset = 0
                }
            }

            MotionEvent.ACTION_UP -> {
                val refreshState = this.refreshState
                if (refreshState is RefreshState.Scrolling) {
                    this.refreshState =
                        if (refreshState.arriveThreshold) {
                            RefreshState.ReadyToRefresh
                        } else {
                            RefreshState.ScrollingCancel
                        }
                }
                initDownX = 0
                initDownY = 0
                sumOffset = 0
            }

            else -> {
                refreshState = RefreshState.ScrollingCancel
                initDownX = 0
                initDownY = 0
                sumOffset = 0
            }
        }
        return
    }


    /**
     * Centralized state transition function that enforces valid [RefreshState] transitions.
     *
     * Cancels any pending tasks/animations from the previous state via [clearTasks] before
     * evaluating the transition. Each branch validates the `old → new` transition and
     * invokes the corresponding [RefreshView] callback. If the transition is invalid,
     * the current state ([oldValue]) is returned unchanged.
     *
     * **Transition rules:**
     * - [RefreshState.None]: always accepted; calls [RefreshView.onNone].
     * - [RefreshState.Scrolling]: only from [RefreshState.None] or another [RefreshState.Scrolling];
     *   calls [RefreshView.onScrolling].
     * - [RefreshState.ScrollingCancel]: only from [RefreshState.Scrolling];
     *   calls [RefreshView.onScrollingCancel], then animates collapse → [RefreshState.None].
     * - [RefreshState.ReadyToRefresh]: only from [RefreshState.Scrolling];
     *   calls [RefreshView.onReadyToRefresh], then animates to normal height → [RefreshState.Refreshing].
     * - [RefreshState.Refreshing]: only from [RefreshState.ReadyToRefresh];
     *   guarded by [allowRefresh]; calls [RefreshView.onTriggerRefresh] + [triggerRefresh].
     * - [RefreshState.RefreshSucceed]: only from [RefreshState.Refreshing];
     *   calls [RefreshView.onRefreshSucceed], schedules auto-dismiss to [RefreshState.None].
     * - [RefreshState.RefreshFailed]: only from [RefreshState.Refreshing];
     *   calls [RefreshView.onRefreshFailed], schedules auto-dismiss to [RefreshState.None].
     *
     * @param oldValue the current state before the transition.
     * @param newValue the requested target state.
     * @return the actual state to adopt (may differ from [newValue] if the transition is rejected).
     */
    private fun onStateChange(
        oldValue: RefreshState,
        newValue: RefreshState
    ): RefreshState {
        Log.d(TAG, "state change from $oldValue to $newValue")

        clearTasks()

        when (newValue) {
            RefreshState.None -> {
                refreshView.onNone()
                return newValue
            }

            is RefreshState.Scrolling -> {
                if (oldValue == RefreshState.None || oldValue is RefreshState.Scrolling) {
                    refreshView.onScrolling(
                        newValue.arriveThreshold,
                        newValue.progress
                    )
                    return newValue
                } else {
                    return oldValue
                }
            }

            RefreshState.ScrollingCancel -> {
                if (oldValue is RefreshState.Scrolling) {
                    refreshView.onScrollingCancel()
                    smoothScrollTo(
                        destHeight = refreshView.minHeight,
                        animDuration = refreshView.normalHeightToMinHeightDuration
                    ) {
                        this.status = RefreshState.None
                    }
                    return newValue
                } else {
                    return oldValue
                }
            }

            RefreshState.ReadyToRefresh -> {
                if (oldValue is RefreshState.Scrolling) {
                    refreshView.onReadyToRefresh()
                    smoothScrollTo(
                        destHeight = refreshView.height,
                        animDuration = refreshView.maxHeightToNormalDuration
                    ) {
                        this.status = RefreshState.Refreshing
                    }
                    return newValue
                } else {
                    return oldValue
                }
            }

            RefreshState.Refreshing -> {
                if (oldValue == RefreshState.ReadyToRefresh) {
                    if (allowRefresh()) {
                        refreshView.onTriggerRefresh()
                        triggerRefresh?.invoke()
                        return newValue
                    } else {
                        return RefreshState.None
                    }
                } else {
                    return oldValue
                }
            }

            RefreshState.RefreshSucceed -> {
                if (oldValue == RefreshState.Refreshing) {
                    refreshView.onRefreshSucceed()
                    subscribeTask({
                        this.status = RefreshState.None
                    }, refreshView.refreshResultShowTime)
                    return newValue
                } else {
                    return oldValue
                }
            }

            RefreshState.RefreshFailed -> {
                if (oldValue == RefreshState.Refreshing) {
                    refreshView.onRefreshFailed()
                    subscribeTask({
                        this.status = RefreshState.None
                    }, refreshView.refreshResultShowTime)
                    return newValue
                } else {
                    return oldValue
                }
            }
        }
    }

    /**
     * Synchronizes the header view's height with the current [RefreshState].
     *
     * Note: [RefreshView] callbacks (e.g. [RefreshView.onTriggerRefresh]) are invoked by
     * [onStateChange], not here. This method only handles layout dimensions.
     *
     * - [RefreshState.None] → [RefreshView.minHeight] (collapsed).
     * - [RefreshState.Scrolling] → proportional to [RefreshState.Scrolling.progress].
     * - [RefreshState.ScrollingCancel] / [RefreshState.ReadyToRefresh] → no height change
     *   (height is being animated by [smoothScrollTo] from [onStateChange]).
     * - [RefreshState.Refreshing] → [RefreshView.height] (expanded).
     * - [RefreshState.RefreshSucceed] / [RefreshState.RefreshFailed] → no height change
     *   (remains at [RefreshView.height], auto-dismiss scheduled in [onStateChange]).
     */
    private fun updateView() {

        when (val status = status) {
            RefreshState.None -> {
                height = refreshView.minHeight
            }

            is RefreshState.Scrolling -> {
                height =
                    refreshView.minHeight + ((refreshView.scrollMaxHeight - refreshView.minHeight) * status.progress).toInt()
            }

            RefreshState.ScrollingCancel -> {
                // Height animation is driven by onStateChange().
            }

            RefreshState.ReadyToRefresh -> {
                // Height animation is driven by onStateChange().
            }

            RefreshState.Refreshing -> {
                height = refreshView.height
            }

            RefreshState.RefreshFailed -> {
                // Height remains expanded until onStateChange() schedules dismissal.
            }

            RefreshState.RefreshSucceed -> {
                // Height remains expanded until onStateChange() schedules dismissal.
            }
        }
    }

    /** Shows a small, non-interactive refresh hint after a fast return to the top. */
    private fun showFastScrollHintIfNeeded() {
        if (!refreshView.showFastScrollHint || refreshState != RefreshState.None || !allowRefresh() || !atTop()) {
            return
        }

        val reachedTopQuickly =
            sawSettlingDuringScroll || upwardScrollDistance >= refreshView.fastScrollHintMinDistance
        if (!wasAwayFromTopDuringScroll || !reachedTopQuickly) {
            return
        }

        refreshView.onNone()
        smoothScrollTo(
            destHeight = refreshView.fastScrollHintHeight,
            animDuration = refreshView.fastScrollHintExpandDuration
        ) {
            if (refreshState == RefreshState.None) {
                smoothScrollTo(
                    destHeight = refreshView.minHeight,
                    animDuration = refreshView.fastScrollHintCollapseDuration
                )
            }
        }
    }

    /** Clears per-scroll-session bookkeeping used by the fast-scroll hint logic. */
    private fun resetScrollTracking() {
        wasAwayFromTopDuringScroll = false
        upwardScrollDistance = 0
        sawSettlingDuringScroll = false
    }

    /**
     * Handles finger movement during an active drag gesture.
     *
     * Maps the cumulative raw drag distance ([sumOffset]) to a damped header height,
     * then calculates drag progress as `(newHeight - minHeight) / (scrollMaxHeight - minHeight)`
     * (yielding a value in `[0, 1]`).
     *
     * The mapping is intentionally non-linear: the header moves quickly for short
     * drags, then gradually becomes harder to pull as it approaches
     * [RefreshView.scrollMaxHeight]. This produces a more natural pull-to-refresh feel
     * than directly accumulating each move delta into the height.
     *
     * After computing the damped height, transitions to a new [RefreshState.Scrolling]
     * with updated [RefreshState.Scrolling.arriveThreshold] and
     * [RefreshState.Scrolling.progress].
     *
     * @param offset vertical drag delta in pixels (positive = dragging downward / expanding).
     */
    private fun onFingerMove(offset: Int) {

        if (status !is RefreshState.Scrolling) {
            return
        }

        Log.d(TAG, "on move rawDrag=$sumOffset, offset=$offset")

        val dragRange = refreshView.scrollMaxHeight - refreshView.minHeight
        if (dragRange <= 0) {
            return
        }

        val newHeight = mapDragOffsetToHeight(sumOffset)
        val progress = (newHeight - refreshView.minHeight).toFloat() / dragRange

        this.refreshState = RefreshState.Scrolling(
            arriveThreshold = newHeight >= refreshView.height,
            progress = progress
        )
    }

    /**
     * Maps a raw cumulative drag distance to a damped header height.
     *
     * Uses a non-linear curve so the header is easy to pull initially but gains
     * resistance as it approaches [RefreshView.scrollMaxHeight]. The result is
     * always clamped to `[minHeight, scrollMaxHeight]`.
     */
    private fun mapDragOffsetToHeight(rawDrag: Int): Int {
        val dragRange = (refreshView.scrollMaxHeight - refreshView.minHeight).toFloat()
        if (dragRange <= 0f) {
            return refreshView.minHeight
        }

        val normalizedDrag = rawDrag.coerceAtLeast(0).toFloat() / dragRange
        val mappedDistance = dragRange *
                (1f - 1f / (1f + normalizedDrag * dragDampingFactor))

        return (refreshView.minHeight + mappedDistance)
            .toInt()
            .coerceIn(refreshView.minHeight, refreshView.scrollMaxHeight)
    }


    /**
     * Animates the header height from its current value to [destHeight].
     *
     * Uses a [DecelerateInterpolator] so the header settles more naturally than a
     * linear animation when snapping to the trigger height or collapsing back.
     *
     * @param destHeight target height in pixels.
     * @param animDuration animation duration in milliseconds.
     * @param finishTask optional callback invoked when the animation ends.
     */
    private fun smoothScrollTo(
        destHeight: Int,
        animDuration: Int,
        finishTask: (() -> Unit)? = null
    ) {
        val visibleHeight = height
        Log.d(TAG, "smoothScrollTo now=$visibleHeight dest = $destHeight")
        heightAnimator?.cancel()
        heightAnimator = ValueAnimator.ofInt(visibleHeight, destHeight).apply {
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                height = animation.animatedValue as Int
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    Log.d(TAG, "onAnimationEnd $height")
                    finishTask?.invoke()
                }
            })
            this.duration = animDuration.toLong()
            start()
        }
    }

    /**
     * Proxy property for the root view's layout height.
     *
     * The getter returns the current `layoutParams.height`.
     * The setter triggers a layout pass when the height changes.
     */
    private var height: Int
        get():Int {
            return root.layoutParams.height
        }
        set(value) {
            root.layoutParams = root.layoutParams.apply {
                this.height = value
            }
        }

    /**
     * ViewHolder that wraps the [RefreshView.view] in a [FrameLayout] container.
     *
     * The actual refresh view is attached/detached as the holder enters/leaves
     * the window, allowing a single view instance to be reused across rebinds.
     */
    class RefreshViewHolder(
        private val container: FrameLayout,
    ) :
        RecyclerView.ViewHolder(container) {

        /** Attaches the given [view] into this holder's container. */
        fun attach(view: View) {
            (view.parent as? ViewGroup)?.removeView(view)
            container.addView(view)
        }

        /** Removes all child views from this holder's container. */
        fun detach() {
            container.removeAllViews()
        }
    }

    /**
     * Cancels any pending delayed tasks and running height animations.
     *
     * Called at the start of [onStateChange] to clean up residual
     * tasks from the previous state before scheduling new ones.
     */
    private fun clearTasks() {
        handler.removeCallbacksAndMessages(null)
        heightAnimator?.cancel()
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
    }

    /**
     * Returns `true` if the first item in the list is fully visible,
     * meaning the user is at the top and a pull-down gesture should be recognized.
     */
    private fun atTop(): Boolean {
        return root.parent != null &&
                (recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() == 0
    }


    /**
     * Creates the root [LinearLayout] container that wraps the [RefreshView.view].
     *
     * The container uses [Gravity.BOTTOM] so the refresh view content aligns to
     * the bottom of the expanding container (appearing to slide down from above).
     * Initialized with [RefreshView.minHeight] so it starts collapsed.
     */
    private fun createRootView(): View {

        val context = recyclerView.context

        val rootView = LinearLayout(context).apply {
            gravity = Gravity.BOTTOM
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                refreshView.minHeight
            )
        }
        val view = refreshView.view

        rootView.addView(view, LinearLayout.LayoutParams.MATCH_PARENT, refreshView.height)

        return rootView
    }

    override fun onCreateViewHolderByStatus(
        parent: ViewGroup,
        status: RefreshState
    ): RefreshViewHolder {
        Log.d(TAG, "onCreateViewHolder")
        val view = FrameLayout(parent.context)
        return RefreshViewHolder(view)
    }

    /**
     * Always returns `true` — the header is always present in the adapter
     * (visibility is controlled separately via view height).
     */
    override fun isValid(status: RefreshState): Boolean {
        return true
    }

    override fun onBindViewHolderByStatus(holder: RefreshViewHolder, status: RefreshState) {
        // Binding is handled by onStateChange + updateView; nothing to do here.
    }

    override fun getItemViewType(position: Int, item: RefreshState): Int = refreshView.view.id

    override fun onViewAttachedToWindow(holder: RefreshViewHolder) {
        super.onViewAttachedToWindow(holder)
        Log.d(TAG, "onViewAttachedToWindow")
        holder.attach(root)
        if (refreshState == RefreshState.None) {
            height = refreshView.minHeight
        }
        _isShow = true
    }

    override fun onViewDetachedFromWindow(holder: RefreshViewHolder) {
        Log.d(TAG, "onViewDetachedFromWindow")
        super.onViewDetachedFromWindow(holder)
        holder.detach()
        _isShow = false
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        recyclerView.addOnItemTouchListener(this)
        recyclerView.addOnScrollListener(scrollListener)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        Log.d(TAG, "onDetachedFromRecyclerView at ${recyclerView.hashCode()}")
        super.onDetachedFromRecyclerView(recyclerView)
        recyclerView.removeOnItemTouchListener(this)
        recyclerView.removeOnScrollListener(scrollListener)
        refreshState = RefreshState.None
        resetScrollTracking()
        clearTasks()
    }

    /**
     * Posts a delayed task to the main thread handler.
     *
     * Used by [onStateChange] to schedule auto-dismiss of result states.
     */
    private fun subscribeTask(task: () -> Unit, delay: Int) {
        handler.postDelayed(task, delay.toLong())
    }

}