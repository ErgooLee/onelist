package io.github.ergoo.onelist

import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import io.github.ergoo.onelist.paging3.OneListPagingAdapter

/**
 * Sets an item-level [SimpleClickListener] on this [OneListAdapter].
 *
 * Internally wraps the simplified listener in a [ClickListenerWrapper] so callers
 * only need the bound data item and clicked view, not the ViewHolder.
 * Passing `null` clears the existing listener.
 */
fun <T : Any, VH : ViewHolder> OneListAdapter<T, VH>.setClickListener(
    simpleClickListener: SimpleClickListener<T>?
) {
    itemClickListener = if (simpleClickListener == null) {
        null
    } else {
        ClickListenerWrapper(simpleClickListener)
    }
}

/**
 * Sets an item-level [SimpleLongClickListener] on this [OneListAdapter].
 *
 * Internally wraps the simplified listener in a [LongClickListenerWrapper].
 * Passing `null` clears the existing listener.
 */
fun <T : Any, VH : ViewHolder> OneListAdapter<T, VH>.setLongClickListener(
    simpleClickListener: SimpleLongClickListener<T>?
) {
    itemLongClickListener = if (simpleClickListener == null) {
        null
    } else {
        LongClickListenerWrapper(simpleClickListener)
    }
}

/**
 * Registers a simplified click listener for a child view inside each item of this [OneListAdapter].
 *
 * @param id the child view id inside the item layout.
 * @return this adapter, for chaining.
 */
fun <T : Any, VH : ViewHolder> OneListAdapter<T, VH>.addOnItemChildSimpleClickListener(
    @IdRes id: Int,
    clickListener: SimpleClickListener<T>
) = apply {
    addOnItemChildClickListener(id, ClickListenerWrapper(clickListener))
}

/**
 * Registers a simplified long-click listener for a child view inside each item of this [OneListAdapter].
 *
 * @param id the child view id inside the item layout.
 * @return this adapter, for chaining.
 */
fun <T : Any, VH : ViewHolder> OneListAdapter<T, VH>.addOnItemChildSimpleLongClickListener(
    @IdRes id: Int,
    clickListener: SimpleLongClickListener<T>
) = apply {
    addOnItemChildLongClickListener(id, LongClickListenerWrapper(clickListener))
}

/**
 * Sets an item-level [SimpleClickListener] on this [OneListPagingAdapter].
 *
 * Internally wraps the simplified listener in a [ClickListenerWrapper].
 * Passing `null` clears the existing listener.
 */
fun <T : Any, VH : ViewHolder> OneListPagingAdapter<T, VH>.setClickListener(
    simpleClickListener: SimpleClickListener<T>?
) {
    itemClickListener = if (simpleClickListener == null) {
        null
    } else {
        ClickListenerWrapper(simpleClickListener)
    }
}

/**
 * Sets an item-level [SimpleLongClickListener] on this [OneListPagingAdapter].
 *
 * Internally wraps the simplified listener in a [LongClickListenerWrapper].
 * Passing `null` clears the existing listener.
 */
fun <T : Any, VH : ViewHolder> OneListPagingAdapter<T, VH>.setLongClickListener(
    simpleClickListener: SimpleLongClickListener<T>?
) {
    itemLongClickListener = if (simpleClickListener == null) {
        null
    } else {
        LongClickListenerWrapper(simpleClickListener)
    }
}

/**
 * Registers a simplified click listener for a child view inside each item of this [OneListPagingAdapter].
 *
 * @param id the child view id inside the item layout.
 * @return this adapter, for chaining.
 */
fun <T : Any, VH : ViewHolder> OneListPagingAdapter<T, VH>.addOnItemChildSimpleClickListener(
    @IdRes id: Int,
    clickListener: SimpleClickListener<T>
) = apply {
    addOnItemChildClickListener(id, ClickListenerWrapper(clickListener))
}

/**
 * Registers a simplified long-click listener for a child view inside each item of this [OneListPagingAdapter].
 *
 * @param id the child view id inside the item layout.
 * @return this adapter, for chaining.
 */
fun <T : Any, VH : ViewHolder> OneListPagingAdapter<T, VH>.addOnItemChildSimpleLongClickListener(
    @IdRes id: Int,
    clickListener: SimpleLongClickListener<T>
) = apply {
    addOnItemChildLongClickListener(id, LongClickListenerWrapper(clickListener))
}

/**
 * Sets an item-level [SimpleClickListener] on this [ListBinder].
 *
 * Useful in [MergeAdapter]-style multi-type lists when the full ViewHolder is not needed.
 * Passing `null` clears the existing listener.
 */
fun <T : Any, VH : ViewHolder> ListBinder<T, VH>.setClickListener(
    simpleClickListener: SimpleClickListener<T>?
) {
    clickListener = if (simpleClickListener == null) {
        null
    } else {
        ClickListenerWrapper(simpleClickListener)
    }
}

/**
 * Sets an item-level [SimpleLongClickListener] on this [ListBinder].
 *
 * Passing `null` clears the existing listener.
 */
fun <T : Any, VH : ViewHolder> ListBinder<T, VH>.setLongClickListener(
    simpleClickListener: SimpleLongClickListener<T>?
) {
    longClickListener = if (simpleClickListener == null) {
        null
    } else {
        LongClickListenerWrapper(simpleClickListener)
    }
}

/**
 * Registers a simplified click listener for a child view handled by this [ListBinder].
 *
 * @param id the child view id inside the binder's item layout.
 * @return this binder, for chaining.
 */
fun <T : Any, VH : ViewHolder> ListBinder<T, VH>.addOnItemChildSimpleClickListener(
    @IdRes id: Int,
    clickListener: SimpleClickListener<T>
) = apply {
    addOnItemChildClickListener(id, ClickListenerWrapper(clickListener))
}

/**
 * Registers a simplified long-click listener for a child view handled by this [ListBinder].
 *
 * @param id the child view id inside the binder's item layout.
 * @return this binder, for chaining.
 */
fun <T : Any, VH : ViewHolder> ListBinder<T, VH>.addOnItemChildSimpleLongClickListener(
    @IdRes id: Int,
    clickListener: SimpleLongClickListener<T>
) = apply {
    addOnItemChildLongClickListener(id, LongClickListenerWrapper(clickListener))
}

/**
 * Marks this ViewHolder's item view as full-span when used under a
 * [StaggeredGridLayoutManager].
 *
 * This is a no-op for other layout parameter types.
 */
fun ViewHolder.asStaggeredGridFullSpan() {
    val layoutParams = this.itemView.layoutParams
    if (layoutParams is StaggeredGridLayoutManager.LayoutParams) {
        layoutParams.isFullSpan = true
    }
}

/**
 * Returns the view type of this ViewHolder as reported by its
 * [binding adapter][RecyclerView.ViewHolder.getBindingAdapter],
 * or [RecyclerView.INVALID_TYPE] if the holder is no longer bound.
 */
val ViewHolder.localViewType: Int
    get() {
        val position = bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return RecyclerView.INVALID_TYPE
        return bindingAdapter?.getItemViewType(position) ?: RecyclerView.INVALID_TYPE
    }
