package io.github.ergoo.onelist.more

import android.view.View
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.ergoo.onelist.FullSpan
import io.github.ergoo.onelist.MainContentAdapter
import io.github.ergoo.onelist.OneItemAdapter

/**
 * Base adapter for load-more / load-previous pagination UI.
 *
 * Works inside a [ConcatAdapter] and auto-discovers the primary content adapter
 * via the [MainContentAdapter] marker interface. It monitors child-view attach
 * events on the [RecyclerView] and calls [tryPreload] only when the scroll
 * position reaches the preload boundary.
 *
 * Subclasses implement [tryPreload] to trigger actual data loading and provide
 * the visual representation of the loading footer/header via [OneItemAdapter].
 *
 * @param VH the ViewHolder type for the load-more item.
 */
abstract class LoadMoreAdapter<VH : RecyclerView.ViewHolder> :
    OneItemAdapter<LoadState, VH>(LoadState.None),
    FullSpan {

    /**
     * Scroll direction inferred from the sequence of child-view attach positions.
     */
    enum class ScrollDirection {
        /** User is scrolling towards the start (top / left). */
        TOWARDS_START,
        /** User is scrolling towards the end (bottom / right). */
        TOWARDS_END,
        /** Direction cannot be determined (e.g. first attach). */
        UNKNOWN,
    }

    /**
     * Number of items from the edge at which [tryPreload] should fire.
     * `0` means fire only when the very last (or first) item is attached.
     */
    open var preloadSize = 0

    /** Master switch for preload; set to `false` to suppress all preload callbacks. */
    open var enablePreload = true

    /** Observes content adapter child attach events for preload triggering. */
    private var contentAttachObserver: ContentAttachObserver? = null

    /** Tracks current bound content adapter to avoid redundant re-binding. */
    private var observedContentAdapter: RecyclerView.Adapter<*>? = null

    /** Observes [ConcatAdapter] structural changes to re-discover [MainContentAdapter]. */
    private var concatObserver: ConcatAdapterObserver? = null

    open var loadState: LoadState
        get() = status
        set(value) {
            status = value
        }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        clearContentObserver()
        concatObserver?.unsubscribe()
        concatObserver = null

        val hostAdapter = recyclerView.adapter
        require(hostAdapter is ConcatAdapter) {
            "RecyclerView's adapter must be a ConcatAdapter, but was ${hostAdapter?.javaClass?.name}"
        }

        // Always observe ConcatAdapter to handle dynamic adapter additions/removals.
        concatObserver = ConcatAdapterObserver(this, hostAdapter).apply { subscribe() }

        tryBindMainContent(hostAdapter, recyclerView)
    }

    /**
     * Called when the scroll position reaches the preload boundary.
     *
     * The boundary check is already done by the base class, so subclasses
     * only need to initiate the actual load operation.
     *
     * @param itemCount   total item count in the content adapter.
     * @param currentPosition the content adapter position that triggered preload.
     * @param direction   inferred scroll direction when the boundary was hit.
     */
    abstract fun tryPreload(itemCount: Int, currentPosition: Int, direction: ScrollDirection)

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        clearContentObserver()
        concatObserver?.unsubscribe()
        concatObserver = null
    }

    /** Releases the content attach observer and clears the tracked adapter reference. */
    private fun clearContentObserver() {
        contentAttachObserver?.unsubscribe()
        contentAttachObserver = null
        observedContentAdapter = null
    }

    /**
     * Searches the [ConcatAdapter] for a [MainContentAdapter] and starts
     * observing its child attach events.
     *
     * @return `true` if a content adapter was found and bound.
     */
    private fun tryBindMainContent(
        hostAdapter: ConcatAdapter,
        recyclerView: RecyclerView
    ): Boolean {
        val markedAdapters = hostAdapter.adapters.filter { it is MainContentAdapter }

        if (markedAdapters.size > 1) {
            throw IllegalStateException("ConcatAdapter must contain only one MainContentAdapter")
        }
        val markedAdapter = markedAdapters.firstOrNull() ?: run {
            // MainContentAdapter removed dynamically; release stale observer.
            clearContentObserver()
            return false
        }

        // Skip re-binding if already observing the same adapter instance.
        if (observedContentAdapter === markedAdapter && contentAttachObserver != null) {
            return true
        }

        contentAttachObserver?.unsubscribe()
        contentAttachObserver = ContentAttachObserver(
            loadMoreAdapter = this,
            recyclerView = recyclerView,
            contentAdapter = markedAdapter
        ).apply {
            subscribe()
        }
        observedContentAdapter = markedAdapter
        return true
    }

    /**
     * Listens for child views being attached to the [RecyclerView].
     *
     * For each attach event belonging to the content adapter, it infers
     * scroll direction from position deltas and checks whether the
     * preload boundary has been reached.
     */
    private class ContentAttachObserver(
        private val loadMoreAdapter: LoadMoreAdapter<*>,
        private val recyclerView: RecyclerView,
        private val contentAdapter: RecyclerView.Adapter<*>,
    ) : RecyclerView.OnChildAttachStateChangeListener {

        /** Last attached position, used to infer scroll direction. */
        private var lastAttachedPosition = RecyclerView.NO_POSITION

        override fun onChildViewAttachedToWindow(view: View) {
            val holder = recyclerView.getChildViewHolder(view)
            // Only track items from the content adapter.
            if (holder.bindingAdapter !== contentAdapter) {
                return
            }
            val position = holder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) {
                return
            }
            val itemCount = contentAdapter.itemCount
            val direction = when {
                lastAttachedPosition == RecyclerView.NO_POSITION -> ScrollDirection.UNKNOWN
                position > lastAttachedPosition -> ScrollDirection.TOWARDS_END
                position < lastAttachedPosition -> ScrollDirection.TOWARDS_START
                else -> ScrollDirection.UNKNOWN
            }
            lastAttachedPosition = position

            if (shouldTriggerPreload(itemCount, position, direction)) {
                loadMoreAdapter.tryPreload(itemCount, position, direction)
            }
        }

        /**
         * Determines whether the current position has reached the preload boundary.
         *
         * - [ScrollDirection.TOWARDS_END]: fires when `preloadSize` items remain at the end.
         * - [ScrollDirection.TOWARDS_START]: fires when position equals `preloadSize` from the start.
         * - [ScrollDirection.UNKNOWN]: never fires to avoid speculative loads.
         */
        private fun shouldTriggerPreload(
            itemCount: Int,
            position: Int,
            direction: ScrollDirection,
        ): Boolean {
            if (!loadMoreAdapter.enablePreload) {
                return false
            }
            if (position !in 0..<itemCount) {
                return false
            }
            return when (direction) {
                ScrollDirection.TOWARDS_END -> itemCount - position - 1 == loadMoreAdapter.preloadSize
                ScrollDirection.TOWARDS_START -> position == loadMoreAdapter.preloadSize
                ScrollDirection.UNKNOWN -> false
            }
        }

        override fun onChildViewDetachedFromWindow(view: View) {
            // No action needed on detach.
        }

        fun subscribe() {
            recyclerView.addOnChildAttachStateChangeListener(this)
        }

        fun unsubscribe() {
            recyclerView.removeOnChildAttachStateChangeListener(this)
        }
    }

    /**
     * Observes the [ConcatAdapter] for structural changes (adapters added/removed/moved).
     *
     * On every change, re-attempts to discover and bind the [MainContentAdapter].
     */
    private class ConcatAdapterObserver(
        private val loadMoreAdapter: LoadMoreAdapter<*>,
        private val hostAdapter: ConcatAdapter,
    ) : RecyclerView.AdapterDataObserver() {

        override fun onChanged() = tryBind()
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = tryBind()
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = tryBind()
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = tryBind()

        private fun tryBind() {
            loadMoreAdapter.tryBindMainContent(hostAdapter, loadMoreAdapter.recyclerView)
        }

        fun subscribe() {
            hostAdapter.registerAdapterDataObserver(this)
        }

        fun unsubscribe() {
            hostAdapter.unregisterAdapterDataObserver(this)
        }
    }
}