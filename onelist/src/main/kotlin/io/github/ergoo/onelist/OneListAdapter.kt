package io.github.ergoo.onelist

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView

/**
 * Base adapter for all OneList adapters.
 *
 * Extends [RecyclerView.Adapter] with:
 * - A convenience [onCreateViewHolder] overload that passes [Context].
 * - Item-level and child-view click / long-click listener management.
 * - [Spannable] support for [OneListGridLayoutManager] and
 *   [StaggeredGridLayoutManager][androidx.recyclerview.widget.StaggeredGridLayoutManager].
 *
 * Click listeners are bound in [onViewAttachedToWindow] and unbound in
 * [onViewDetachedFromWindow]; changes made while a view is already attached
 * will not take effect until the next attach cycle.
 *
 * Binding on attach/detach (instead of in [onBindViewHolder]) also reduces
 * the risk of leaking stale Fragment references when a ViewHolder instance is
 * reused across Fragment boundaries.
 *
 * @param T  the type of data items.
 * @param VH the type of [RecyclerView.ViewHolder].
 */
abstract class OneListAdapter<T : Any, VH : RecyclerView.ViewHolder> :
    RecyclerView.Adapter<VH>(),
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
     * if not attached. Safe to call at any time.
     */
    val recyclerViewOrNull: RecyclerView?
        get() = _recyclerView

    /**
     * The [RecyclerView] this adapter is attached to.
     *
     * @throws IllegalStateException if called before
     *         [onAttachedToRecyclerView].
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

    /**
     * Delegates to [onCreateViewHolder] with an additional [Context] parameter
     * extracted from [parent].
     */
    final override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): VH {
        return onCreateViewHolder(parent.context, parent, viewType)
    }

    /**
     * Creates a new [VH] for the given [viewType].
     *
     * @param context the [Context] obtained from [parent].
     * @param parent  the parent [ViewGroup].
     * @param viewType the view type of the new view.
     */
    protected abstract fun onCreateViewHolder(
        context: Context, parent: ViewGroup, viewType: Int
    ): VH

    // -----------------------------------------------------------------------
    // View binding
    // -----------------------------------------------------------------------

    /** Resolves the item at [position] and delegates to the three-arg overload. */
    override fun onBindViewHolder(holder: VH, position: Int) {
        onBindViewHolder(holder, position, getItem(position))
    }

    /**
     * Binds [item] to [holder] at [position].
     *
     * Subclasses must implement this to populate their views.
     */
    protected abstract fun onBindViewHolder(holder: VH, position: Int, item: T)

    /**
     * Called when RecyclerView needs to re-bind with [payloads].
     * Delegates to the payload-aware overload if payloads are non-empty,
     * or falls back to the full-bind overload.
     */
    override fun onBindViewHolder(
        holder: VH, position: Int, payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            onBindViewHolder(holder, position, getItem(position), payloads)
        }
    }

    /**
     * Partial bind with [payloads]. Default implementation falls back to
     * the full bind via [onBindViewHolder].
     */
    protected open fun onBindViewHolder(holder: VH, position: Int, item: T, payloads: List<Any>) {
        onBindViewHolder(holder, position, item)
    }

    // -----------------------------------------------------------------------
    // View type
    // -----------------------------------------------------------------------

    /**
     * Resolves the item at [position] and delegates to the two-arg
     * [getItemViewType] for subclass customisation.
     */
    final override fun getItemViewType(position: Int): Int {
        return getItemViewType(position, getItem(position))
    }

    /** Returns the data item at [position]. */
    abstract fun getItem(position: Int): T

    /**
     * Returns the view type for [item] at [position]. Defaults to `0`.
     * Override to support multiple view types.
     */
    protected open fun getItemViewType(position: Int, item: T): Int = 0

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Called when [holder]'s view is attached to the window.
     *
     * Handles full-span setup for
     * [StaggeredGridLayoutManager][androidx.recyclerview.widget.StaggeredGridLayoutManager],
     * and binds click listeners.
     */
    @CallSuper
    override fun onViewAttachedToWindow(holder: VH) {
        super.onViewAttachedToWindow(holder)

        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION && isFullSpanByViewType(getItemViewType(position))) {
            holder.asStaggeredGridFullSpan()
        }

        bindViewClickListener(holder)
    }

    /**
     * Called when [holder]'s view is detached from the window.
     *
     * Unbinds click listeners.
     */
    @CallSuper
    override fun onViewDetachedFromWindow(holder: VH) {
        super.onViewDetachedFromWindow(holder)
        unBindViewClickListener(holder)
    }

    @CallSuper
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        _recyclerView = recyclerView
        super.onAttachedToRecyclerView(recyclerView)
    }

    @CallSuper
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        _recyclerView = null
        super.onDetachedFromRecyclerView(recyclerView)
    }

    // -----------------------------------------------------------------------
    // Spannable
    // -----------------------------------------------------------------------

    override fun isFullSpanByViewType(itemType: Int): Boolean {
        return false
    }

    override fun isFullSpanItemByPosition(position: Int): Boolean {
        return isFullSpanByViewType(getItemViewType(position))
    }

    override fun spanCount(position: Int): Int = 1

    // -----------------------------------------------------------------------
    // Click binding
    // -----------------------------------------------------------------------

    /**
     * Binds item-level and child-view click/long-click listeners to [viewHolder].
     *
     * Called from [onViewAttachedToWindow] rather than [onBindViewHolder]. This keeps
     * listener lifecycle aligned with the view attach lifecycle, so listeners are
     * removed in [onViewDetachedFromWindow], reducing the chance of retaining stale
     * Fragment references when ViewHolders are reused across Fragment boundaries.
     *
     * Override in multi-type adapters (e.g. [MergeAdapter]) to delegate to [ListBinder]s instead.
     */
    protected open fun bindViewClickListener(viewHolder: VH) {
        clickDelegate.bindViewClickListener(viewHolder) { position -> getItem(position) }
    }

    /**
     * Removes all click/long-click listeners previously set by [bindViewClickListener].
     *
     * Called from [onViewDetachedFromWindow].
     */
    protected open fun unBindViewClickListener(viewHolder: VH) {
        clickDelegate.unBindViewClickListener(viewHolder)
    }

    // -----------------------------------------------------------------------
    // Child click listener registration
    // -----------------------------------------------------------------------

    /**
     * Registers a click listener for the child view with [id].
     *
     * @param id the resource id of the child view.
     * @param listener the listener to invoke on click.
     */
    open fun addOnItemChildClickListener(@IdRes id: Int, listener: ClickListener<T, VH>) = apply {
        clickDelegate.addOnItemChildClickListener(id, listener)
    }

    /** Removes the child click listener for [id]. */
    open fun removeOnItemChildClickListener(@IdRes id: Int) = apply {
        clickDelegate.removeOnItemChildClickListener(id)
    }

    /**
     * Registers a long-click listener for the child view with [id].
     *
     * @param id the resource id of the child view.
     * @param listener the listener to invoke on long-click.
     */
    open fun addOnItemChildLongClickListener(
        @IdRes id: Int,
        listener: LongClickListener<T, VH>
    ) = apply {
        clickDelegate.addOnItemChildLongClickListener(id, listener)
    }

    /** Removes the child long-click listener for [id]. */
    open fun removeOnItemChildLongClickListener(@IdRes id: Int) = apply {
        clickDelegate.removeOnItemChildLongClickListener(id)
    }


}
