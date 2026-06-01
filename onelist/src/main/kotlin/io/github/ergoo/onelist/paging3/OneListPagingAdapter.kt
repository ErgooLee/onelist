package io.github.ergoo.onelist.paging3

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.github.ergoo.onelist.ClickDelegate
import io.github.ergoo.onelist.ClickListener
import io.github.ergoo.onelist.LongClickListener
import io.github.ergoo.onelist.Spannable
import io.github.ergoo.onelist.asStaggeredGridFullSpan
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Base adapter for Paging 3 integration, mirroring [io.github.ergoo.onelist.OneListAdapter]'s
 * API on top of [PagingDataAdapter].
 *
 * Provides:
 * - A convenience [onCreateViewHolder] overload that passes [Context].
 * - Item-level and child-view click / long-click listener management.
 * - [Spannable] support for [io.github.ergoo.onelist.OneListGridLayoutManager] and
 *   [androidx.recyclerview.widget.StaggeredGridLayoutManager].
 *
 * Click listeners are bound in [onViewAttachedToWindow] and unbound in
 * [onViewDetachedFromWindow]; changes made while a view is already attached
 * will not take effect until the next attach cycle.
 *
 * Binding on attach/detach (instead of in [onBindViewHolder]) also reduces
 * the risk of leaking stale Fragment references when a ViewHolder instance is
 * reused across Fragment boundaries.
 *
 * **Note:** Because [PagingDataAdapter.getItem] triggers prefetch,
 * [getItemWithoutPreload] is used in view-type resolution and click callbacks
 * to avoid unintended page loads.
 *
 * @param T  the type of data items.
 * @param VH the type of [RecyclerView.ViewHolder].
 * @param diffCallback callback for computing list diffs.
 * @param mainDispatcher dispatcher for UI updates (defaults to [Dispatchers.Main]).
 * @param workerDispatcher dispatcher for background diff computation (defaults to [Dispatchers.Default]).
 */
@Suppress("UNCHECKED_CAST")
abstract class OneListPagingAdapter<T : Any, VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
) :
    PagingDataAdapter<T, RecyclerView.ViewHolder>(diffCallback, mainDispatcher, workerDispatcher),
    Spannable {

    /** Delegate that manages click/long-click listener storage, binding, and unbinding. */
    private val clickDelegate = ClickDelegate<T, VH>()

    /** Item-level click listener, invoked when the entire item view is tapped. */
    open var itemClickListener: ClickListener<T, VH>?
        get() = clickDelegate.itemClickListener
        set(value) {
            clickDelegate.itemClickListener = value
        }

    /** Item-level long-click listener, invoked when the entire item view is long-pressed. */
    open var itemLongClickListener: LongClickListener<T, VH>?
        get() = clickDelegate.itemLongClickListener
        set(value) {
            clickDelegate.itemLongClickListener = value
        }

    private var _recyclerView: RecyclerView? = null

    /**
     * The [RecyclerView] this adapter is currently attached to, or `null`
     * if not attached.
     */
    val recyclerViewOrNull: RecyclerView?
        get() = _recyclerView

    /**
     * The [RecyclerView] this adapter is attached to.
     *
     * @throws IllegalStateException if called before [onAttachedToRecyclerView].
     */
    val recyclerView: RecyclerView
        get() {
            checkNotNull(_recyclerView) {
                "Please get it after onAttachedToRecyclerView()"
            }
            return _recyclerView!!
        }

    /** Convenience accessor for [RecyclerView.getContext]. */
    val context: Context
        get() = recyclerView.context

    // -----------------------------------------------------------------------
    // View creation
    // -----------------------------------------------------------------------

    final override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int,
    ): RecyclerView.ViewHolder {
        return onCreateViewHolder(parent.context, parent, viewType)
    }

    protected abstract fun onCreateViewHolder(
        context: Context, parent: ViewGroup, viewType: Int,
    ): VH

    // -----------------------------------------------------------------------
    // View binding
    // -----------------------------------------------------------------------

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        onBindViewHolder(holder as VH, position, getItem(position))
    }

    protected abstract fun onBindViewHolder(holder: VH, position: Int, item: T?)

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>,
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            onBindViewHolder(holder as VH, position, getItem(position), payloads)
        }
    }

    protected open fun onBindViewHolder(holder: VH, position: Int, item: T?, payloads: List<Any>) {
        onBindViewHolder(holder, position, item)
    }

    // -----------------------------------------------------------------------
    // View type
    // -----------------------------------------------------------------------

    final override fun getItemViewType(position: Int): Int {
        return getItemViewType(position, getItemWithoutPreload(position))
    }

    protected open fun getItemViewType(position: Int, item: T?): Int = 0

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @CallSuper
    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)

        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION && isFullSpanByViewType(getItemViewType(position))) {
            holder.asStaggeredGridFullSpan()
        }

        bindViewClickListener(holder as VH)
    }

    @CallSuper
    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        unBindViewClickListener(holder)
    }

    @CallSuper
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        _recyclerView = recyclerView
    }

    @CallSuper
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        _recyclerView = null
    }

    // -----------------------------------------------------------------------
    // Spannable
    // -----------------------------------------------------------------------

    override fun isFullSpanByViewType(itemType: Int): Boolean = false

    override fun isFullSpanItemByPosition(position: Int): Boolean =
        isFullSpanByViewType(getItemViewType(position))

    override fun spanCount(position: Int): Int = 1

    // -----------------------------------------------------------------------
    // Click binding
    // -----------------------------------------------------------------------

    /**
     * Binds item-level and child-view click/long-click listeners via [ClickDelegate].
     *
     * Called from [onViewAttachedToWindow] rather than [onBindViewHolder]. This keeps
     * listener lifecycle aligned with the view attach lifecycle, so listeners are
     * removed in [onViewDetachedFromWindow], reducing the chance of retaining stale
     * Fragment references when ViewHolders are reused across Fragment boundaries.
     *
     * Uses [getItemWithoutPreload] to avoid triggering Paging prefetch.
     */
    protected open fun bindViewClickListener(viewHolder: VH) {
        clickDelegate.bindViewClickListener(viewHolder) { position ->
            getItemWithoutPreload(position)
        }
    }

    protected open fun unBindViewClickListener(viewHolder: RecyclerView.ViewHolder) {
        clickDelegate.unBindViewClickListener(viewHolder)
    }

    // -----------------------------------------------------------------------
    // Child click listener registration
    // -----------------------------------------------------------------------

    open fun addOnItemChildClickListener(@IdRes id: Int, listener: ClickListener<T, VH>) = apply {
        clickDelegate.addOnItemChildClickListener(id, listener)
    }

    open fun removeOnItemChildClickListener(@IdRes id: Int) = apply {
        clickDelegate.removeOnItemChildClickListener(id)
    }

    open fun addOnItemChildLongClickListener(
        @IdRes id: Int,
        listener: LongClickListener<T, VH>,
    ) = apply {
        clickDelegate.addOnItemChildLongClickListener(id, listener)
    }

    open fun removeOnItemChildLongClickListener(@IdRes id: Int) = apply {
        clickDelegate.removeOnItemChildLongClickListener(id)
    }


    /**
     * Returns the item at [position] from the current snapshot **without**
     * triggering [PagingDataAdapter]'s prefetch mechanism.
     */
    fun getItemWithoutPreload(position: Int): T? {
        return snapshot().items.getOrNull(position)
    }

}

