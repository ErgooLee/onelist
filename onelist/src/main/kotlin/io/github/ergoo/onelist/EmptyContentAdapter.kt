package io.github.ergoo.onelist

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * An adapter that displays an empty-state view when the associated content adapter has no items.
 *
 * This adapter is designed to be used inside a [ConcatAdapter] alongside a content adapter
 * that implements [MainContentAdapter]. It automatically discovers and observes the content
 * adapter's data changes, showing/hiding the empty view accordingly.
 *
 * The [emptyView]'s [View.getId] is used as the adapter's view type, so it must have a
 * valid ID (not [View.NO_ID]).
 *
 * Usage:
 * ```kotlin
 * val emptyView = LayoutInflater.from(context).inflate(R.layout.view_empty, parent, false)
 * val emptyAdapter = EmptyContentAdapter(emptyView)
 * val concat = ConcatAdapter(contentAdapter, emptyAdapter)
 * recyclerView.adapter = concat
 * ```
 *
 * The content adapter must implement [MainContentAdapter] for auto-detection.
 * Alternatively, call [bindToContentAdapter] manually.
 *
 * @param emptyView the view to display when the content adapter is empty.
 *                  Must have a valid view ID ([View.getId] != [View.NO_ID]).
 */
open class EmptyContentAdapter(val emptyView: View) :
    OneItemAdapter<Boolean, RecyclerView.ViewHolder>(true),
    FullSpan {

    init {
        require(emptyView.id != View.NO_ID) { "emptyView must have a valid id." }
    }

    /** Observer registered on the content adapter to track data changes. */
    private val contentSizeChangeListener = ContentSizeChangeListener(this)

    /** Observer registered on the ConcatAdapter while waiting for a [MainContentAdapter] to appear. */
    private var concatObserver: ConcatAdapterObserver? = null

    /** Returns the [emptyView]'s ID as the view type for this adapter. */
    override fun getStateViewType(status: Boolean): Int {
        return emptyView.id
    }

    override fun onCreateViewHolderByStatus(
        parent: ViewGroup,
        status: Boolean
    ): RecyclerView.ViewHolder {
        // Ensure the view has no parent before attaching to the ViewHolder
        (emptyView.parent as? ViewGroup)?.removeView(emptyView)
        return object : RecyclerView.ViewHolder(emptyView) {}
    }

    /** Empty view is visible when [status] is true (i.e. content adapter has no items). */
    override fun isValid(status: Boolean): Boolean {
        return status
    }

    override fun onBindViewHolderByStatus(holder: RecyclerView.ViewHolder, status: Boolean) {
        // View is fully created in onCreateViewHolder; no binding needed.
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        autoBindContentAdapter(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        contentSizeChangeListener.unbind()
        concatObserver?.unregister()
        concatObserver = null
    }

    /**
     * Automatically finds and observes the [MainContentAdapter] within the host [ConcatAdapter].
     *
     * If the [MainContentAdapter] is not yet present (e.g. added later), a [ConcatAdapterObserver]
     * is registered to detect when it appears and bind automatically at that point.
     *
     * @throws IllegalArgumentException if the RecyclerView's adapter is not a [ConcatAdapter].
     */
    private fun autoBindContentAdapter(recyclerView: RecyclerView) {
        contentSizeChangeListener.unbind()
        concatObserver?.unregister()
        concatObserver = null

        val hostAdapter = recyclerView.adapter
        require(hostAdapter is ConcatAdapter) {
            "RecyclerView's adapter must be a ConcatAdapter, but was ${hostAdapter?.javaClass?.name}"
        }

        if (!tryBindMainContent(hostAdapter)) {
            // MainContentAdapter not yet added; observe ConcatAdapter for structural changes
            concatObserver = ConcatAdapterObserver(this, hostAdapter).also { it.register() }
        }
    }

    /**
     * Searches the [ConcatAdapter] for the first adapter implementing [MainContentAdapter]
     * and starts observing it.
     *
     * @return `true` if a [MainContentAdapter] was found and bound, `false` otherwise.
     */
    private fun tryBindMainContent(concatAdapter: ConcatAdapter): Boolean {
        val contentAdapter = concatAdapter.adapters.firstOrNull { it is MainContentAdapter }
        if (contentAdapter != null) {
            contentSizeChangeListener.bindTo(contentAdapter)
            return true
        }
        return false
    }

    /**
     * Manually binds to a specific content adapter.
     *
     * Use this when auto-detection via [MainContentAdapter] is not applicable
     * (e.g. the content adapter cannot implement [MainContentAdapter]).
     */
    fun bindToContentAdapter(contentAdapter: RecyclerView.Adapter<*>) {
        contentSizeChangeListener.unbind()
        contentSizeChangeListener.bindTo(contentAdapter)
    }

    /** Manually unbinds from the currently observed content adapter. */
    fun unBind() {
        contentSizeChangeListener.unbind()
    }

    /**
     * Observes a content adapter's data changes and updates the empty view visibility.
     *
     * When the content adapter's [itemCount] drops to 0, [EmptyContentAdapter.status] is set
     * to `true` (showing the empty view). When items are present, status is set to `false`.
     */
    private class ContentSizeChangeListener(
        val emptyAdapter: EmptyContentAdapter
    ) : RecyclerView.AdapterDataObserver() {

        private var contentAdapter: RecyclerView.Adapter<*>? = null

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = refreshStatus()
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = refreshStatus()
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = refreshStatus()
        override fun onChanged() = refreshStatus()
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = refreshStatus()
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) = refreshStatus()
        override fun onStateRestorationPolicyChanged() = refreshStatus()

        /** Updates empty view visibility: shown when content has no items. */
        private fun refreshStatus() {
            emptyAdapter.status = (contentAdapter?.itemCount ?: 0) == 0
        }

        /** Registers this observer on [adapter] and immediately syncs state. */
        fun bindTo(adapter: RecyclerView.Adapter<*>) {
            this.contentAdapter = adapter
            adapter.registerAdapterDataObserver(this)
            refreshStatus()
        }

        /** Unregisters this observer and releases the reference to the content adapter. */
        fun unbind() {
            this.contentAdapter?.unregisterAdapterDataObserver(this)
            this.contentAdapter = null
        }
    }

    /**
     * Observes the [ConcatAdapter] for structural changes (new adapters being added).
     *
     * When a [MainContentAdapter] is detected after being added to the [ConcatAdapter],
     * this observer automatically binds the [EmptyContentAdapter] to it and then
     * unregisters itself since its job is done.
     */
    private class ConcatAdapterObserver(
        private val emptyAdapter: EmptyContentAdapter,
        private val concatAdapter: ConcatAdapter
    ) : RecyclerView.AdapterDataObserver() {

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = tryBind()
        override fun onChanged() = tryBind()

        /** Attempts to find and bind MainContentAdapter; self-unregisters on success. */
        private fun tryBind() {
            if (emptyAdapter.tryBindMainContent(concatAdapter)) {
                unregister()
                emptyAdapter.concatObserver = null
            }
        }

        fun register() {
            concatAdapter.registerAdapterDataObserver(this)
        }

        fun unregister() {
            concatAdapter.unregisterAdapterDataObserver(this)
        }
    }

}