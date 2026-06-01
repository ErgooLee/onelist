package io.github.ergoo.onelist.more

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
import io.github.ergoo.onelist.Log

private const val TAG = "BottomLoadMoreAdapter"

/**
 * A gesture-driven bottom load-more adapter that supports pull-up-to-load-more interaction.
 *
 * This adapter places a [LoadMoreView] at the bottom of a [RecyclerView]. When the user
 * scrolls to the bottom and drags upward past a threshold, a load operation is triggered.
 * It also supports automatic preloading when the scroll position nears the end of the list.
 * While attached, it suppresses RecyclerView's native overscroll glow/stretch effect
 * to avoid visual conflict with the custom pull-up-to-load-more interaction.
 *
 * ## How it works
 *
 * 1. The adapter always shows exactly one item (the load-more footer).
 * 2. When the footer's ViewHolder is attached to the window, the adapter registers an
 *    [RecyclerView.OnItemTouchListener] to intercept drag gestures.
 * 3. As the user drags upward, the state transitions to [LoadState.Scrolling] and the
 *    footer view's height grows with non-linear damping, so the gesture feels light
 *    at first and gradually becomes heavier as it approaches
 *    [LoadMoreView.scrollMaxHeight]. Visual feedback is provided via
 *    [LoadMoreView.onScrolling].
 * 4. If the drag exceeds [LoadMoreView.height] (the threshold), releasing the finger
 *    transitions to [LoadState.ReadyToLoad], animates to the normal height, and then
 *    automatically transitions to [LoadState.Loading] which triggers [onTriggerLoadMore].
 * 5. If the drag does not reach the threshold, the state transitions to
 *    [LoadState.ScrollingCancel], the footer collapses back with an animation,
 *    and [LoadMoreView.onScrollCanceled] is called.
 * 6. When a load succeeds and the state returns to [LoadState.None] while the footer is
 *    still attached, this adapter intentionally keeps the footer container height unchanged
 *    and only hides [LoadMoreView.view]. The height is collapsed back to
 *    [LoadMoreView.minHeight] after the footer detaches (or is re-attached while idle).
 *    This trades a temporary blank footer slot for better scroll-position stability and
 *    avoids the visible jump caused by shrinking a visible footer immediately.
 *
 * ## State management
 *
 * All state transitions are centralized in [tryChangeLoadState], which enforces the valid
 * transition rules defined in [LoadState]. Setting [loadState] drives both the state
 * machine and the view updates.
 *
 * External callers typically only set:
 * - [LoadState.None] — after a successful load (more pages available).
 * - [LoadState.Error] — after a failed load.
 * - [LoadState.Final] — after the last page is loaded.
 *
 * The gesture-related states ([LoadState.Scrolling], [LoadState.ScrollingCancel],
 * [LoadState.ReadyToLoad]) are managed internally by the touch handling logic.
 *
 * ## Usage
 *
 * ```kotlin
 * val loadMoreAdapter = BottomLoadMoreAdapter(OneListBottomLoadMore(context)).apply {
 *     onTriggerLoadMore = { viewModel.loadNextPage() }
 *     allowTriggerLoadMore = { !viewModel.isRefreshing }
 * }
 * recyclerView.adapter = ConcatAdapter(contentAdapter, loadMoreAdapter)
 * ```
 *
 * @param loadMoreView the [LoadMoreView] that provides the footer's visual representation.
 *                     Its [LoadMoreView.view] must have a valid view ID.
 *
 * @see LoadMoreAdapter
 * @see LoadMoreView
 * @see OneListBottomLoadMore
 * @see LoadState
 */
open class BottomLoadMoreAdapter(
    private val loadMoreView: LoadMoreView
) : LoadMoreAdapter<BottomLoadMoreAdapter.BottomLoadMoreVH>(), RecyclerView.OnItemTouchListener {

    init {
        require(loadMoreView.view.id != View.NO_ID) { "LoadMoreView's view must have a valid id." }
    }

    /** Last recorded raw Y coordinate for delta calculation during drag. */
    private var lastY = -1f

    /**
     * Cumulative upward drag distance during the current gesture.
     *
     * This stores the raw finger movement before non-linear mapping is applied
     * and is used as the input to [mapDragOffsetToHeight].
     */
    private var sumOffset = 0f

    /**
     * Controls how quickly drag resistance ramps up.
     *
     * Larger values make the footer feel heavier earlier; smaller values keep it
     * more linear. `3f` provides a noticeable but still reachable pull distance.
     *
     * Values below `0` are coerced to `0`.
     */
    var dragDampingFactor = 3f
        set(value) {
            field = value.coerceAtLeast(0f)
        }


    /** Number of currently attached ViewHolders; used to determine footer visibility. */
    private var attachedNum = 0

    /** Root container view that wraps the [LoadMoreView.view] with controlled height. */
    private val root: View by lazy {
        createRootView()
    }

    /** Handler for scheduling delayed tasks (e.g. error auto-dismiss). */
    private val handler = Handler(Looper.getMainLooper())

    /** Currently running height animation, or `null` if idle. */
    private var heightAnimator: Animator? = null

    /**
     * Delayed task that auto-dismisses the error state.
     *
     * If the footer is visible when the task fires, it smoothly collapses
     * before resetting to [LoadState.None]; otherwise it resets immediately.
     */
    private val errorToNoneTask = Runnable {
        if (loadState is LoadState.Error) {
            if (isFooterVisible()) {
                smoothScrollTo(
                    loadMoreView.minHeight,
                    loadMoreView.normalHeightToGoneAnimDuration
                ) {
                    loadState = LoadState.None
                }
            } else {
                loadState = LoadState.None
            }
        }
    }

    /**
     * Callback invoked when a load-more operation should be triggered.
     *
     * Called during the [LoadState.Loading] transition, after [allowTriggerLoadMore]
     * has returned `true`. Can be triggered either by a drag gesture (via
     * [LoadState.ReadyToLoad] → [LoadState.Loading]) or by preload.
     */
    var onTriggerLoadMore: (() -> Unit)? = null

    /**
     * Guard function that determines whether a load operation is allowed.
     *
     * Return `false` to suppress load triggers (e.g. during a refresh).
     * Defaults to `{ true }`.
     */
    var allowTriggerLoadMore: (() -> Boolean) = { true }

    /**
     * Controls the visibility of the load-more footer based on the current [LoadState].
     *
     * When set, the footer's visibility is immediately re-evaluated. Return `false`
     * from the lambda to hide the footer for a given state.
     * Defaults to `{ true }` (always visible).
     */
    var canShow: (status: LoadState) -> Boolean = { true }
        set(value) {
            field = value
            updateView()
        }

    /**
     * The current load state. Setting this property drives the state machine
     * via [tryChangeLoadState] and updates the footer's visual representation.
     *
     * The state machine may reject or redirect transitions (e.g. setting
     * [LoadState.Loading] when the current state is not [LoadState.ReadyToLoad]
     * or [LoadState.None] will be ignored). The actual resulting state is
     * stored in [status].
     *
     * Must be set on the main thread.
     */
    override var loadState: LoadState
        get() = status
        @MainThread
        set(value) {
            status = value
        }

    /**
     * Backing field for [loadState]. Overrides the parent `status` property to intercept
     * value changes through the state machine ([tryChangeLoadState]) and synchronize
     * the footer view ([updateView]).
     *
     */
    override var status: LoadState = LoadState.None
        set(value) {
            if (value != field) {
                field = tryChangeLoadState(field, value)
                updateView()
            }
        }

    /**
     * Centralized state transition function that enforces valid [LoadState] transitions.
     *
     * Cancels any pending tasks/animations from the previous state via [clear] before
     * evaluating the transition. Each branch validates the `old → new` transition and invokes the corresponding
     * [LoadMoreView] callback. If the transition is invalid, the current state ([old])
     * is returned unchanged.
     *
     * **Transition rules:**
     * - [LoadState.None]: always accepted; calls [LoadMoreView.onLoadNone]. If the footer
     *   is still visible, the container height is preserved and only the inner
     *   [LoadMoreView.view] is hidden; the container is normalized later via
     *   [restoreFooterPresentationIfNeeded].
     * - [LoadState.Scrolling]: only from [LoadState.None] or another [LoadState.Scrolling]; calls [LoadMoreView.onScrolling].
     * - [LoadState.ScrollingCancel]: only from [LoadState.Scrolling]; calls [LoadMoreView.onScrollCanceled],
     *   then animates collapse → [LoadState.None].
     * - [LoadState.ReadyToLoad]: only from [LoadState.Scrolling]; calls [LoadMoreView.onReadyToLoad],
     *   then animates to normal height → [LoadState.Loading].
     * - [LoadState.Loading]: only from [LoadState.ReadyToLoad] (gesture) or [LoadState.None] (preload);
     *   guarded by [allowTriggerLoadMore]; calls [LoadMoreView.onLoadStart] + [onTriggerLoadMore].
     * - [LoadState.Error]: only if footer is visible; calls [LoadMoreView.onLoadError],
     *   schedules auto-dismiss to [LoadState.None]. Falls back to [LoadState.None] if footer is off-screen.
     * - [LoadState.Final]: always accepted; calls [LoadMoreView.onFinalPageSucceed].
     *
     * @param old the current state before the transition.
     * @param new the requested target state.
     * @return the actual state to adopt (may differ from [new] if the transition is rejected).
     */
    private fun tryChangeLoadState(old: LoadState, new: LoadState): LoadState {
        clear()

        when (new) {
            LoadState.None -> {
                loadMoreView.onLoadNone()
                return new
            }

            is LoadState.Scrolling -> {
                if (old == LoadState.None || old is LoadState.Scrolling) {
                    loadMoreView.onScrolling(new.arriveThreshold, new.progress)
                    return new
                } else {
                    return old
                }
            }

            is LoadState.ScrollingCancel -> {
                if (old is LoadState.Scrolling) {
                    loadMoreView.onScrollCanceled()
                    smoothScrollTo(
                        loadMoreView.minHeight,
                        loadMoreView.normalHeightToGoneAnimDuration
                    ) {
                        loadState = LoadState.None
                    }
                    return new
                } else {
                    return old
                }
            }

            is LoadState.ReadyToLoad -> {
                if (old is LoadState.Scrolling) {
                    loadMoreView.onReadyToLoad()
                    smoothScrollTo(
                        loadMoreView.height,
                        loadMoreView.scrollingMaxReleaseAnimDuration
                    ) {
                        if (loadState == LoadState.ReadyToLoad) {
                            loadState = LoadState.Loading
                        }
                    }
                    return new
                } else {
                    return old
                }
            }

            LoadState.Loading -> {
                if (old is LoadState.ReadyToLoad || old == LoadState.None) {
                    if (allowTriggerLoadMore()) {
                        loadMoreView.onLoadStart()
                        onTriggerLoadMore?.invoke()
                        return new
                    } else {
                        return LoadState.None
                    }
                }
                return old
            }

            is LoadState.Error -> {
                if (isFooterVisible()) {
                    loadMoreView.onLoadError(new.error)
                    handler.postDelayed(
                        errorToNoneTask,
                        loadMoreView.loadErrorTipsShowDuration.toLong()
                    )
                    return new
                } else {
                    return LoadState.None
                }
            }

            LoadState.Final -> {
                loadMoreView.onFinalPageSucceed()
                return new
            }
        }
    }

    /**
     * Synchronizes the footer view's height and visibility with the current [LoadState].
     *
     * Configures the footer:
     * - **Visibility**: controlled by [canShow].
     * - **Height by state**:
     *   - [LoadState.None] → when the footer is still attached, keep the previous container
     *     height and hide only [LoadMoreView.view]; once detached (or re-attached while idle),
     *     [restoreFooterPresentationIfNeeded] collapses the container back to [LoadMoreView.minHeight].
     *   - [LoadState.Scrolling] → [LoadMoreView.minHeight] + [LoadState.Scrolling.progress] × ([LoadMoreView.scrollMaxHeight] - [LoadMoreView.minHeight]).
     *   - [LoadState.ReadyToLoad] / [LoadState.ScrollingCancel] → no height change
     *     (height is being animated by [smoothScrollTo] from [tryChangeLoadState]).
     *   - [LoadState.Loading] / [LoadState.Error] → [LoadMoreView.height] (expanded).
     *   - [LoadState.Final] → keep [LoadMoreView.height]; if the footer is already attached,
     *     hide only [LoadMoreView.view] to avoid a brief "no more" flash before newly inserted
     *     items push the footer off-screen. Once detached/re-attached, the final view is shown.
     *
     * Note: [LoadMoreView] callbacks (e.g. [LoadMoreView.onLoadStart]) are invoked by
     * [tryChangeLoadState], not here. This method only handles layout dimensions.
     */
    private fun updateView() {

        val status = this.status

        val isFooterVisible = isFooterVisible()

        root.visibility = if (canShow(status)) View.VISIBLE else View.GONE

        when (status) {
            LoadState.None -> applyNonePresentation(isFooterVisible)
            is LoadState.Scrolling -> applyScrollingPresentation(status)
            LoadState.ReadyToLoad,
            LoadState.ScrollingCancel -> setFooterContentVisible(true)
            LoadState.Loading,
            is LoadState.Error -> applyExpandedPresentation()
            LoadState.Final -> applyFinalPresentation(isFooterVisible)
        }

        Log.d(TAG, "bind status=$status bottom=$isFooterVisible height=$height")
    }

    private fun applyNonePresentation(isFooterVisible: Boolean) {
        if (isFooterVisible) {
            setFooterContentVisible(false)
        } else {
            height = loadMoreView.minHeight
            setFooterContentVisible(true)
        }
    }

    private fun applyScrollingPresentation(status: LoadState.Scrolling) {
        height =
            loadMoreView.minHeight +
                    ((loadMoreView.scrollMaxHeight - loadMoreView.minHeight) * status.progress).toInt()
        setFooterContentVisible(true)
    }

    private fun applyExpandedPresentation() {
        height = loadMoreView.height
        setFooterContentVisible(true)
    }

    private fun applyFinalPresentation(isFooterVisible: Boolean) {
        height = loadMoreView.height
        setFooterContentVisible(!isFooterVisible)
    }

    private fun setFooterContentVisible(visible: Boolean) {
        loadMoreView.view.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    /**
     * Intercepts touch events on the [RecyclerView] to drive the drag-to-load gesture.
     *
     * **ACTION_MOVE:**
     * - If currently [LoadState.None] and conditions allow, enters [LoadState.Scrolling]
     *   and records the initial Y coordinate.
     * - If already [LoadState.Scrolling], computes the upward drag delta, accumulates it,
     *   and delegates to
     *   [onFingerMove] to update drag progress.
     * - If conditions no longer allow scrolling mid-drag, cancels via [LoadState.ScrollingCancel].
     *
     * **Other actions (ACTION_UP, ACTION_CANCEL, etc.):**
     * - If [LoadState.Scrolling] with threshold met → [LoadState.ReadyToLoad].
     * - If [LoadState.Scrolling] without threshold → [LoadState.ScrollingCancel].
     *
     * Always returns `false` — this adapter observes but never consumes touch events.
     */
    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        val canGoToScrolling = allowTriggerLoadMore() && isFooterVisible()

        val loadState = this.loadState

        when (e.action) {
            MotionEvent.ACTION_MOVE -> {
                if (loadState == LoadState.None) {
                    if (canGoToScrolling) {
                        lastY = e.rawY
                        sumOffset = 0f
                        this.loadState = LoadState.Scrolling(arriveThreshold = false, progress = 0f)
                    }
                } else if (loadState is LoadState.Scrolling) {
                    if (canGoToScrolling) {
                        val deltaY: Float = lastY - e.rawY
                        lastY = e.rawY
                        sumOffset = (sumOffset + deltaY).coerceAtLeast(0f)
                        onFingerMove(deltaY)
                    } else {
                        sumOffset = 0f
                        this.loadState = LoadState.ScrollingCancel
                    }
                }
            }

            else -> {
                if (loadState is LoadState.Scrolling) {
                    if (canGoToScrolling) {
                        if (loadState.arriveThreshold) {
                            sumOffset = 0f
                            this.loadState = LoadState.ReadyToLoad
                        } else {
                            sumOffset = 0f
                            this.loadState = LoadState.ScrollingCancel
                        }
                    } else {
                        sumOffset = 0f
                        this.loadState = LoadState.ScrollingCancel
                    }
                }
            }
        }
        return false
    }



    /**
     * Handles finger movement during an active drag gesture.
     *
     * Maps the cumulative upward drag distance ([sumOffset]) to a damped footer height,
     * then calculates drag progress as `(newHeight - minHeight) / (scrollMaxHeight - minHeight)`
     * (yielding a value in `[0, 1]`).
     *
     * The mapping is intentionally non-linear: the footer moves quickly for short drags,
     * then gradually becomes harder to pull as it approaches
     * [LoadMoreView.scrollMaxHeight]. This produces a more natural pull-up-to-load feel
     * than directly accumulating each move delta into the height.
     *
     * After computing the damped height, transitions to a new [LoadState.Scrolling]
     * with updated [LoadState.Scrolling.arriveThreshold] and [LoadState.Scrolling.progress].
     *
     * @param offset vertical drag delta in pixels (positive = dragging upward / expanding).
     */
    private fun onFingerMove(offset: Float) {
        Log.d(TAG, "on move rawDrag=$sumOffset, offset=$offset")

        val dragRange = loadMoreView.scrollMaxHeight - loadMoreView.minHeight
        if (dragRange <= 0) {
            return
        }

        val newHeight = mapDragOffsetToHeight(sumOffset)
        val progress = (newHeight - loadMoreView.minHeight).toFloat() / dragRange

        val arriveThreshold = newHeight >= loadMoreView.height

        this.loadState = LoadState.Scrolling(
            arriveThreshold,
            progress
        )
    }

    /**
     * Maps a raw cumulative drag distance to a damped footer height.
     *
     * Uses a non-linear curve so the footer is easy to pull initially but gains
     * resistance as it approaches [LoadMoreView.scrollMaxHeight]. The result is
     * always clamped to `[minHeight, scrollMaxHeight]`.
     */
    private fun mapDragOffsetToHeight(rawDrag: Float): Int {
        val dragRange = (loadMoreView.scrollMaxHeight - loadMoreView.minHeight).toFloat()
        if (dragRange <= 0f) {
            return loadMoreView.minHeight
        }

        val normalizedDrag = rawDrag.coerceAtLeast(0f) / dragRange
        val mappedDistance = dragRange *
                (1f - 1f / (1f + normalizedDrag * dragDampingFactor))

        return (loadMoreView.minHeight + mappedDistance)
            .toInt()
            .coerceIn(loadMoreView.minHeight, loadMoreView.scrollMaxHeight)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        Log.d(TAG, "onAttachedToRecyclerView at ${hashCode()}")
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        Log.d(TAG, "onDetachedFromRecyclerView at ${hashCode()}")
        // Fallback restore to avoid leaving RecyclerView in modified global state.
        recyclerView.removeOnItemTouchListener(this)
        attachedNum = 0
        clear()
    }

    override fun onViewAttachedToWindow(holder: BottomLoadMoreVH) {
        super.onViewAttachedToWindow(holder)
        Log.d(TAG, "onViewAttachedToWindow at ${hashCode()}")
        attachedNum++
        holder.attach(root)
        if (attachedNum == 1) {
            recyclerView.addOnItemTouchListener(this)
        }
        restoreFooterPresentationIfNeeded()
    }

    override fun onViewDetachedFromWindow(holder: BottomLoadMoreVH) {
        super.onViewDetachedFromWindow(holder)
        Log.d(TAG, "onViewDetachedFromWindow at ${hashCode()}")
        attachedNum = (attachedNum - 1).coerceAtLeast(0)
        holder.detach()
        if (attachedNum == 0) {
            recyclerViewOrNull?.removeOnItemTouchListener(this@BottomLoadMoreAdapter)
        }
        restoreFooterPresentationIfNeeded()
    }

    /**
     * Animates the footer height from its current value to [destHeight].
     *
     * Uses a [DecelerateInterpolator] so the footer settles more naturally than an
     * accelerating animation when snapping to the trigger height or collapsing back.
     *
     * @param destHeight target height in pixels.
     * @param duration animation duration in milliseconds.
     * @param commitCallback optional callback invoked when the animation ends.
     */
    private fun smoothScrollTo(
        destHeight: Int,
        duration: Int,
        commitCallback: (() -> Unit)? = null
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
                    commitCallback?.invoke()
                }
            })
            this.duration = duration.toLong()
            start()
        }
    }

    /**
     * Cancels any pending delayed tasks and running height animations.
     *
     * Called at the start of [tryChangeLoadState] to clean up residual
     * tasks from the previous state before scheduling new ones.
     */
    fun clear() {
        handler.removeCallbacks(errorToNoneTask)
        heightAnimator?.cancel()
    }

    /**
     * Proxy property for the root view's layout height.
     *
     * The getter returns the current `layoutParams.height`.
     * The setter clamps the value to at least [LoadMoreView.minHeight] and
     * only triggers a layout pass when the height actually changes.
     */
    private var height: Int
        get(): Int {
            return root.layoutParams.height
        }
        set(value) {
            val lp = root.layoutParams
            val newHeight = if (value <= loadMoreView.minHeight) loadMoreView.minHeight else value
            if (lp.height != newHeight) {
                lp.height = newHeight
                root.layoutParams = lp
            }
        }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        //do nothing
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        //do nothing
    }

    /**
     * Creates the root [LinearLayout] container that wraps the [LoadMoreView.view].
     *
     * The container is initialized with [LoadMoreView.minHeight] so it starts collapsed.
     */
    private fun createRootView(): View {

        val viewContext = recyclerView.context

        val rootViewGroup = LinearLayout(viewContext).apply {
            gravity = Gravity.TOP
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                loadMoreView.minHeight
            )
        }

        rootViewGroup.addView(
            loadMoreView.view,
            LinearLayout.LayoutParams.MATCH_PARENT,
            loadMoreView.height
        )

        return rootViewGroup
    }

    /**
     * Always returns `true` — the footer is always present in the adapter
     * (visibility is controlled separately via [canShow] and view height).
     */
    override fun isValid(status: LoadState): Boolean {
        return true
    }

    /**
     * Called by [LoadMoreAdapter] when the scroll position reaches the preload boundary.
     *
     * Ignores [ScrollDirection.TOWARDS_START] (scrolling away from the bottom).
     * For [ScrollDirection.TOWARDS_END], attempts to transition directly to
     * [LoadState.Loading]. The transition will only succeed if [tryChangeLoadState]
     * permits it (i.e. the current state is [LoadState.None] or [LoadState.ReadyToLoad]
     * and [allowTriggerLoadMore] returns `true`).
     */
    override fun tryPreload(
        itemCount: Int, currentPosition: Int, direction: ScrollDirection
    ) {
        if (direction == ScrollDirection.TOWARDS_START) {
            return
        }
        loadState = LoadState.Loading
    }


    /** Returns `true` if the footer ViewHolder is currently attached (visible). */
    private fun isFooterVisible() = attachedNum > 0


    override fun onCreateViewHolderByStatus(
        parent: ViewGroup, status: LoadState
    ): BottomLoadMoreVH {
        Log.d(TAG, "create view holder at ${hashCode()}")
        val view = FrameLayout(parent.context)
        return BottomLoadMoreVH(view)
    }

    override fun onBindViewHolderByStatus(holder: BottomLoadMoreVH, status: LoadState) {
        //do nothing
    }

    override fun getStateViewType(status: LoadState): Int {
        return loadMoreView.view.id
    }


    class BottomLoadMoreVH(private val container: FrameLayout) :
        RecyclerView.ViewHolder(container) {

        /** Attaches the given [view] into this holder's container, removing it from any previous parent. */
        fun attach(view: View) {
            val parent = view.parent
            if (parent is ViewGroup) {
                parent.removeView(view)
            }
            container.addView(view)
        }

        /** Removes all child views from this holder's container. */
        fun detach() {
            container.removeAllViews()
        }
    }

    /**
     * Normalizes footer presentation after attach-state changes.
     *
     * Under the "keep occupied space while visible" strategy, [LoadState.None] may leave
     * the outer container at its previous expanded height and simply hide
     * [LoadMoreView.view]. This method is called from attach/detach callbacks to normalize
     * the footer once doing so will no longer cause a visible jump:
     * - [LoadState.None] collapses back to [LoadMoreView.minHeight].
     * - [LoadState.Final] keeps the final-page height but restores [LoadMoreView.view]
     *   visibility after the footer leaves and re-enters the viewport.
     */
    private fun restoreFooterPresentationIfNeeded() {
        if (status == LoadState.None) {
            height = loadMoreView.minHeight
            loadMoreView.view.visibility = View.VISIBLE
        } else if (status == LoadState.Final) {
            loadMoreView.view.visibility = View.VISIBLE
        }
    }


}