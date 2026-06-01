package io.github.ergoo.onelist

import android.util.SparseArray
import android.view.View
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView

/**
 * Encapsulates click/long-click listener storage, binding, and unbinding
 * shared by [OneListAdapter] and
 * [io.github.ergoo.onelist.paging3.OneListPagingAdapter].
 *
 * @param T  the type of data items.
 * @param VH the type of [RecyclerView.ViewHolder].
 */
class ClickDelegate<T : Any, VH : RecyclerView.ViewHolder> {

    /** Item-level click listener. */
    var itemClickListener: ClickListener<T, VH>? = null

    /** Item-level long-click listener. */
    var itemLongClickListener: LongClickListener<T, VH>? = null

    /** Child-view click listeners, keyed by view id. */
    private val itemChildClickArray = SparseArray<ClickListener<T, VH>>(3)

    /** Child-view long-click listeners, keyed by view id. */
    private val itemChildLongClickArray = SparseArray<LongClickListener<T, VH>>(3)

    // -----------------------------------------------------------------------
    // Registration
    // -----------------------------------------------------------------------

    fun addOnItemChildClickListener(@IdRes id: Int, listener: ClickListener<T, VH>) {
        itemChildClickArray.put(id, listener)
    }

    fun removeOnItemChildClickListener(@IdRes id: Int) {
        itemChildClickArray.remove(id)
    }

    fun addOnItemChildLongClickListener(@IdRes id: Int, listener: LongClickListener<T, VH>) {
        itemChildLongClickArray.put(id, listener)
    }

    fun removeOnItemChildLongClickListener(@IdRes id: Int) {
        itemChildLongClickArray.remove(id)
    }

    // -----------------------------------------------------------------------
    // Bind / Unbind
    // -----------------------------------------------------------------------

    /**
     * Binds item-level and child-view click/long-click listeners to [viewHolder].
     *
     * @param viewHolder the holder to bind listeners to.
     * @param getItem function that retrieves the data item for a given position,
     *        or `null` if the item is unavailable (e.g. Paging placeholder).
     */
    fun bindViewClickListener(viewHolder: VH, getItem: (Int) -> T?) {
        itemClickListener?.let { listener ->
            viewHolder.itemView.setOnClickListener { v ->
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                val item = getItem(position) ?: return@setOnClickListener
                listener.onClick(item, v, viewHolder)
            }
        }

        itemLongClickListener?.let { listener ->
            viewHolder.itemView.setOnLongClickListener { v ->
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnLongClickListener false
                val item = getItem(position) ?: return@setOnLongClickListener false
                listener.onLongClick(item, v, viewHolder)
            }
        }

        for (i in 0 until itemChildClickArray.size()) {
            val id = itemChildClickArray.keyAt(i)
            val clickListener = itemChildClickArray.valueAt(i) ?: continue

            viewHolder.itemView.findViewById<View>(id)?.let { childView ->
                childView.setOnClickListener { v ->
                    val position = viewHolder.bindingAdapterPosition
                    if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                    val item = getItem(position) ?: return@setOnClickListener
                    clickListener.onClick(item, v, viewHolder)
                }
            }
        }

        for (i in 0 until itemChildLongClickArray.size()) {
            val id = itemChildLongClickArray.keyAt(i)
            val longClickListener = itemChildLongClickArray.valueAt(i) ?: continue

            viewHolder.itemView.findViewById<View>(id)?.let { childView ->
                childView.setOnLongClickListener { v ->
                    val position = viewHolder.bindingAdapterPosition
                    if (position == RecyclerView.NO_POSITION) return@setOnLongClickListener false
                    val item = getItem(position) ?: return@setOnLongClickListener false
                    longClickListener.onLongClick(item, v, viewHolder)
                }
            }
        }
    }

    /**
     * Removes all click/long-click listeners previously set by [bindViewClickListener].
     */
    fun unBindViewClickListener(viewHolder: RecyclerView.ViewHolder) {
        viewHolder.itemView.setOnClickListener(null)
        viewHolder.itemView.setOnLongClickListener(null)

        for (i in 0 until itemChildClickArray.size()) {
            val id = itemChildClickArray.keyAt(i)
            viewHolder.itemView.findViewById<View>(id)?.setOnClickListener(null)
        }

        for (i in 0 until itemChildLongClickArray.size()) {
            val id = itemChildLongClickArray.keyAt(i)
            viewHolder.itemView.findViewById<View>(id)?.setOnLongClickListener(null)
        }
    }
}

