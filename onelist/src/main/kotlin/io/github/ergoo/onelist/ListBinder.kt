package io.github.ergoo.onelist

import android.content.Context
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView

/**
 * Delegate that handles view creation, binding, and click events for a single
 * item type within a [MergeAdapter] or [DifferMergeAdapter].
 *
 * Each `ListBinder` is responsible for one view-type (returned by [getViewType])
 * and is registered via [MergeAdapter.addListBinder].
 *
 * **Click handling:** Set [clickListener] / [longClickListener] for item-level
 * clicks, or use [addOnItemChildClickListener] for specific child views.
 * Listeners are bound in [onViewAttachedToWindow] and unbound in
 * [onViewDetachedFromWindow]; changes made while a view is already attached
 * will not take effect until the next attach cycle.
 *
 * @param T the type of data items this binder handles.
 * @param VH the type of [RecyclerView.ViewHolder] this binder creates.
 */
abstract class ListBinder<T : Any, VH : RecyclerView.ViewHolder> {

    /** Child-view click listeners, keyed by view id. */
    private val itemChildClickArray = SparseArray<ClickListener<T, VH>>(3)

    /** Child-view long-click listeners, keyed by view id. */
    private val itemChildLongClickArray = SparseArray<LongClickListener<T, VH>>(3)

    /** Set by [MergeAdapter] when this binder is registered. */
    internal var _adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null

    /** Set by [MergeAdapter] in [MergeAdapter.onAttachedToRecyclerView]. */
    internal var _recyclerView: RecyclerView? = null

    /** Set by [MergeAdapter] in [MergeAdapter.onAttachedToRecyclerView]. */
    internal var _context: Context? = null

    /** Item-level click listener; set before the view is attached to take effect. */
    var clickListener: ClickListener<T, VH>? = null

    /** Item-level long-click listener; set before the view is attached to take effect. */
    var longClickListener: LongClickListener<T, VH>? = null

    /**
     * The adapter this binder is attached to.
     *
     * @throws IllegalStateException if accessed before [MergeAdapter.addListBinder].
     */
    val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
        get() {
            checkNotNull(_adapter) {
                """This $this has not been attached to BaseAdapter yet.
                    You should not call the method before addListBinder()."""
            }
            return _adapter!!
        }

    /**
     * The [RecyclerView] this binder's adapter is attached to.
     *
     * @throws IllegalStateException if accessed before
     *         [RecyclerView.Adapter.onAttachedToRecyclerView].
     */
    val recyclerView: RecyclerView
        get() {
            checkNotNull(_recyclerView) {
                "Please get it after onAttachedToRecyclerView()"
            }
            return _recyclerView!!
        }

    /**
     * The [Context] obtained from the [RecyclerView].
     *
     * @throws IllegalStateException if accessed before the adapter is attached.
     */
    val context: Context
        get() {
            checkNotNull(_context) {
                """This $this has not been attached to BaseAdapter yet.
                    You should not call the method before onCreateViewHolder()."""
            }
            return _context!!
        }

    /**
     * Provides access to the adapter's data for click callbacks.
     * Set internally by [MergeAdapter.addListBinder]; should not be modified externally.
     */
    internal var dataHolder: DataHolder? = null

    /**
     * Creates a new [VH] for the given [viewType].
     *
     * @param parent the parent [ViewGroup] into which the new view will be added.
     * @param viewType the view type of the new view (same value as [getViewType]).
     */
    abstract fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH

    /**
     * Binds the [data] to the given [holder].
     *
     * @param holder the ViewHolder to bind data to.
     * @param data the data item for this position.
     */
    abstract fun convert(holder: VH, data: T)

    /**
     * Returns the unique view-type integer for this binder.
     *
     * Must be unique across all binders registered on the same [MergeAdapter].
     */
    abstract fun getViewType(): Int

    /**
     * Returns the number of grid columns this binder's items should span.
     * Defaults to `1`. Override to customise span size in a
     * [OneListGridLayoutManager].
     */
    open fun spanCount(): Int = 1

    /**
     * Binds [data] to [holder] with partial update [payloads].
     *
     * Default implementation delegates to [convert] (full bind).
     */
    open fun convert(holder: VH, data: T, payloads: List<Any>) {
        convert(holder, data)
    }

    /**
     * Called when the adapter fails to recycle [holder].
     *
     * @return `true` if the holder should be recycled despite the failure.
     * @see RecyclerView.Adapter.onFailedToRecycleView
     */
    open fun onFailedToRecycleView(holder: VH): Boolean {
        return false
    }

    /**
     * Called when [holder]'s view is attached to the window.
     * Binds click listeners by default; override to add custom logic
     * (call `super` to retain click binding).
     */
    open fun onViewAttachedToWindow(holder: VH) {
        bindClick(holder)
    }

    /**
     * Called when [holder]'s view is detached from the window.
     * Unbinds click listeners by default; override to add custom logic
     * (call `super` to retain unbinding).
     */
    open fun onViewDetachedFromWindow(holder: VH) {
        unbindClick(holder)
    }

    /** Called when [holder] is recycled. No-op by default. */
    open fun onViewRecycled(holder: VH) {}

    /**
     * Binds item-level and child-view click/long-click listeners to [viewHolder].
     *
     * Click callbacks retrieve the data item via [dataHolder] using the holder's
     * [bindingAdapterPosition][RecyclerView.ViewHolder.getBindingAdapterPosition].
     */
    @Suppress("UNCHECKED_CAST")
    protected open fun bindClick(viewHolder: VH) {
        clickListener?.let {
            viewHolder.itemView.setOnClickListener { v ->
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                val item = dataHolder?.getItem(position) as? T
                if (item != null) {
                    it.onClick(item, v, viewHolder)
                }
            }
        }
        longClickListener?.let {
            viewHolder.itemView.setOnLongClickListener { v ->
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    return@setOnLongClickListener false
                }
                val item = dataHolder?.getItem(position) as? T
                if (item != null) {
                    it.onLongClick(item, v, viewHolder)
                } else {
                    false
                }
            }
        }


        for (i in 0 until itemChildClickArray.size()) {
            val id = itemChildClickArray.keyAt(i)

            viewHolder.itemView.findViewById<View>(id)?.let { childView ->
                childView.setOnClickListener { v ->
                    val position = viewHolder.bindingAdapterPosition
                    if (position == RecyclerView.NO_POSITION) {
                        return@setOnClickListener
                    }
                    val item = dataHolder?.getItem(position) as? T

                    val listener = itemChildClickArray.valueAt(i)
                    if (item != null && listener != null) {
                        listener.onClick(item, v, viewHolder)
                    }
                }
            }
        }


        for (i in 0 until itemChildLongClickArray.size()) {
            val id = itemChildLongClickArray.keyAt(i)

            viewHolder.itemView.findViewById<View>(id)?.let { childView ->
                childView.setOnLongClickListener { v ->
                    val position = viewHolder.bindingAdapterPosition
                    if (position == RecyclerView.NO_POSITION) {
                        return@setOnLongClickListener false
                    }
                    val item = dataHolder?.getItem(position) as? T
                    val listener = itemChildLongClickArray.valueAt(i)
                    if (item != null && listener != null) {
                        listener.onLongClick(
                            item,
                            v,
                            viewHolder
                        )
                    } else {
                        false
                    }
                }
            }
        }
    }

    /**
     * Removes all click/long-click listeners previously set by [bindClick].
     */
    protected open fun unbindClick(viewHolder: RecyclerView.ViewHolder) {
        clickListener?.let {
            viewHolder.itemView.setOnClickListener(null)
        }

        longClickListener?.let {
            viewHolder.itemView.setOnLongClickListener(null)
        }

        for (i in 0 until itemChildClickArray.size()) {
            val id = itemChildClickArray.keyAt(i)

            viewHolder.itemView.findViewById<View>(id)?.setOnClickListener(null)
        }

        for (i in 0 until itemChildLongClickArray.size()) {
            val id = itemChildLongClickArray.keyAt(i)

            viewHolder.itemView.findViewById<View>(id)?.setOnLongClickListener(null)
        }

    }

    /**
     * Registers a click listener for the child view with [id].
     *
     * @param id the resource id of the child view.
     * @param clickListener the listener to invoke on click.
     */
    open fun addOnItemChildClickListener(@IdRes id: Int, clickListener: ClickListener<T, VH>) = apply {
        itemChildClickArray.put(id, clickListener)
    }

    /** Removes the child click listener for [id]. */
    open fun removeOnItemChildClickListener(@IdRes id: Int) = apply {
        itemChildClickArray.remove(id)
    }

    /**
     * Registers a long-click listener for the child view with [id].
     *
     * @param id the resource id of the child view.
     * @param clickListener the listener to invoke on long-click.
     */
    open fun addOnItemChildLongClickListener(@IdRes id: Int, clickListener: LongClickListener<T, VH>) =
        apply {
            itemChildLongClickArray.put(id, clickListener)
        }

    /** Removes the child long-click listener for [id]. */
    open fun removeOnItemChildLongClickListener(@IdRes id: Int) = apply {
        itemChildLongClickArray.remove(id)
    }

    /**
     * Abstraction that provides data items to click callbacks.
     * Implemented by [MergeAdapter] to bridge its data list to the binder.
     */
    interface DataHolder {
        /** Returns the data item at [position], or `null` if unavailable. */
        fun getItem(position: Int): Any?
    }

}