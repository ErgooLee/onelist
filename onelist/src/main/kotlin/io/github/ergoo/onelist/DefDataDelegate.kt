package io.github.ergoo.onelist

import android.annotation.SuppressLint
import androidx.annotation.IntRange
import androidx.recyclerview.widget.RecyclerView
import java.util.*

/**
 * Default [DataDelegate] implementation backed by a [List] that is lazily converted
 * to a [MutableList] on first mutation.
 *
 * Each mutation method dispatches the corresponding `notifyItem*` call on [adapter]
 * so the RecyclerView stays in sync.
 *
 * @param T the type of data items.
 * @param adapter the adapter to notify after data changes.
 */
class DefDataDelegate<T : Any>(
    private val adapter: RecyclerView.Adapter<*>,
) : DataDelegate<T> {

    private var items: List<T> = emptyList()

    override fun getData(): List<T> = items

    override fun dataSize(): Int = items.size

    override fun getItem(@IntRange(from = 0) position: Int): T = items[position]

    @SuppressLint("NotifyDataSetChanged")
    override fun setData(newList: List<T>?) {
        items = newList ?: emptyList()
        adapter.notifyDataSetChanged()
    }

    override operator fun set(@IntRange(from = 0) position: Int, item: T) {
        if (position >= items.size) {
            throw IndexOutOfBoundsException("position: $position. size:${items.size}")
        }
        ensureMutable()[position] = item
        adapter.notifyItemChanged(position)
    }

    override fun add(@IntRange(from = 0) position: Int, item: T) {
        if (position > items.size || position < 0) {
            throw IndexOutOfBoundsException("position: $position. size:${items.size}")
        }
        ensureMutable().add(position, item)
        adapter.notifyItemInserted(position)
    }

    override fun add(item: T) {
        ensureMutable().add(item)
        adapter.notifyItemInserted(items.size - 1)
    }

    override fun addAll(@IntRange(from = 0) position: Int, newCollection: Collection<T>) {
        if (position > items.size || position < 0) {
            throw IndexOutOfBoundsException("position: $position. size:${items.size}")
        }
        ensureMutable().addAll(position, newCollection)
        adapter.notifyItemRangeInserted(position, newCollection.size)
    }

    override fun addAll(newCollection: Collection<T>) {
        val oldSize = items.size
        ensureMutable().addAll(newCollection)
        adapter.notifyItemRangeInserted(oldSize, newCollection.size)
    }

    override fun removeAt(@IntRange(from = 0) position: Int) {
        if (position >= items.size) {
            throw IndexOutOfBoundsException("position: $position. size:${items.size}")
        }
        ensureMutable().removeAt(position)
        adapter.notifyItemRemoved(position)
    }

    override fun remove(item: T) {
        val index = items.indexOf(item)
        if (index == -1) return
        removeAt(index)
    }

    override fun swap(fromPosition: Int, toPosition: Int) {
        val size = items.size
        if (fromPosition !in 0 until size) {
            throw IndexOutOfBoundsException("fromPosition: $fromPosition. size:$size")
        }
        if (toPosition !in 0 until size) {
            throw IndexOutOfBoundsException("toPosition: $toPosition. size:$size")
        }
        Collections.swap(ensureMutable(), fromPosition, toPosition)
        adapter.notifyItemMoved(fromPosition, toPosition)
    }

    /**
     * Returns [items] as a [MutableList], converting it in-place if necessary.
     *
     * Only [ArrayList] is trusted as truly mutable. Other [List] implementations
     * (e.g. `Arrays.asList()`, `Collections.unmodifiableList()`) may implement
     * [MutableList] at the JVM level but throw on mutation, so they are copied
     * into a new [ArrayList] to be safe.
     */
    private fun ensureMutable(): MutableList<T> {
        if (items is ArrayList) {
            return items as ArrayList<T>
        }
        return ArrayList(items).also { items = it }
    }

}